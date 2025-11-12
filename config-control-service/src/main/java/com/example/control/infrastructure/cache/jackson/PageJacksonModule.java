// package com.example.control.infrastructure.cache.jackson;

package com.example.control.infrastructure.cache.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson module to (de)serialize Spring Data PageImpl safely for Redis cache.
 * Writes stable JSON and preserves type id at root (via TypeSerializer).
 */
public final class PageJacksonModule extends SimpleModule {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PageJacksonModule() {
        // Register serializers/deserializers for PageImpl and Page
        addSerializer((Class) PageImpl.class, (JsonSerializer) new PageImplJsonSerializer());
        addDeserializer((Class) PageImpl.class, (JsonDeserializer) new PageImplJsonDeserializer());
        addSerializer((Class) Page.class, (JsonSerializer) new PageInterfaceSerializer());
        addDeserializer((Class) Page.class, (JsonDeserializer) new PageInterfaceDeserializer());
    }

    /** Common body writer (no start/end object here) */
    private static void writeBody(Page<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // content
        gen.writeFieldName("content");
        // Let Jackson handle element typing inside the array (default typing on)
        serializers.defaultSerializeValue(value.getContent(), gen);

        // page metadata
        gen.writeNumberField("number", value.getNumber());
        gen.writeNumberField("size", value.getSize());
        gen.writeNumberField("totalElements", value.getTotalElements());
        gen.writeBooleanField("paged", value.getPageable().isPaged());

        // sort as list of orders
        gen.writeArrayFieldStart("sort");
        for (Sort.Order o : value.getSort()) {
            gen.writeStartObject();
            gen.writeStringField("property", o.getProperty());
            gen.writeStringField("direction", o.getDirection().name());
            gen.writeBooleanField("ignoreCase", o.isIgnoreCase());
            gen.writeStringField("nullHandling", o.getNullHandling().name());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    public static class PageImplJsonSerializer extends JsonSerializer<PageImpl<?>> {
        @Override
        public void serialize(PageImpl<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            writeBody(value, gen, serializers);
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(PageImpl<?> value, JsonGenerator gen, SerializerProvider serializers,
                                      com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
            // IMPORTANT: write type id at root so GenericJackson2JsonRedisSerializer can read it
            WritableTypeId typeId = typeSer.typeId(value, JsonToken.START_OBJECT);
            typeSer.writeTypePrefix(gen, typeId);
            writeBody(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    public static class PageImplJsonDeserializer extends JsonDeserializer<PageImpl<?>> {
        @Override
        public PageImpl<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            ObjectNode node = mapper.readTree(p);

            List<?> content = mapper.convertValue(node.get("content"), new TypeReference<List<?>>() {});
            int number = node.path("number").asInt(0);
            int size = node.path("size").asInt(content != null ? content.size() : 0);
            long total = node.path("totalElements").asLong(content != null ? content.size() : 0L);
            boolean paged = node.path("paged").asBoolean(size > 0);

            // sort
            List<Sort.Order> orders = new ArrayList<>();
            JsonNode sortNode = node.get("sort");
            if (sortNode != null && sortNode.isArray()) {
                for (JsonNode n : sortNode) {
                    String property = n.path("property").asText();
                    Sort.Direction dir = Sort.Direction.valueOf(n.path("direction").asText("ASC"));
                    boolean ignoreCase = n.path("ignoreCase").asBoolean(false);
                    Sort.NullHandling nh = Sort.NullHandling.valueOf(n.path("nullHandling").asText("NATIVE"));
                    Sort.Order order = new Sort.Order(dir, property);
                    if (ignoreCase) order = order.ignoreCase();
                    order = switch (nh) {
                        case NULLS_FIRST -> order.with(Sort.NullHandling.NULLS_FIRST);
                        case NULLS_LAST  -> order.with(Sort.NullHandling.NULLS_LAST);
                        default          -> order.with(Sort.NullHandling.NATIVE);
                    };
                    orders.add(order);
                }
            }
            Sort sort = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
            Pageable pageable = paged ? PageRequest.of(number, Math.max(size, 1), sort) : Pageable.unpaged();

            return new PageImpl<>(content == null ? List.of() : content, pageable, total);
        }
    }

    /** Serializer for Page interface (delegates to PageImpl shape) */
    static class PageInterfaceSerializer extends JsonSerializer<Page<?>> {
        @Override
        public void serialize(Page<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            new PageImplJsonSerializer().serialize(new PageImpl<>(value.getContent(), value.getPageable(), value.getTotalElements()), gen, serializers);
        }
        @Override
        public void serializeWithType(Page<?> value, JsonGenerator gen, SerializerProvider serializers,
                                      com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
            new PageImplJsonSerializer().serializeWithType(new PageImpl<>(value.getContent(), value.getPageable(), value.getTotalElements()), gen, serializers, typeSer);
        }
    }

    /** Deserializer for Page interface (delegates to PageImpl) */
    static class PageInterfaceDeserializer extends JsonDeserializer<Page<?>> {
        @Override
        public Page<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new PageImplJsonDeserializer().deserialize(p, ctxt);
        }
    }
}
