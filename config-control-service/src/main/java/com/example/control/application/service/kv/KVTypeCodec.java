package com.example.control.application.service.kv;

import com.example.control.domain.model.kv.KVListManifest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Helper component responsible for translating between raw KV byte payloads and
 * structured representations (JSON/YAML/Properties).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KVTypeCodec {

    private final ObjectMapper objectMapper;
    private final YAMLMapper yamlMapper = new YAMLMapper();

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {};

    public enum StructuredFormat {
        JSON,
        YAML,
        PROPERTIES
    }

    /**
     * Parses the manifest JSON payload into the domain representation.
     *
     * @param data raw manifest bytes
     * @return manifest instance (empty if payload missing/invalid)
     */
    public KVListManifest parseManifest(byte[] data) {
        if (data == null || data.length == 0) {
            return KVListManifest.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            List<String> order = Optional.ofNullable(root.get("order"))
                    .filter(JsonNode::isArray)
                    .map(node -> objectMapper.convertValue(node, STRING_LIST))
                    .orElse(List.of());
            long version = Optional.ofNullable(root.get("version"))
                    .filter(JsonNode::isNumber)
                    .map(JsonNode::asLong)
                    .orElse(0L);
            String etag = Optional.ofNullable(root.get("etag"))
                    .map(JsonNode::asText)
                    .orElse("");
            Map<String, Object> metadata = Optional.ofNullable(root.get("metadata"))
                    .filter(JsonNode::isObject)
                    .map(node -> objectMapper.convertValue(node, OBJECT_MAP))
                    .orElse(Map.of());
            return new KVListManifest(order, version, etag, metadata);
        } catch (Exception ex) {
            log.warn("Failed to parse KV manifest: {}", ex.getMessage());
            return KVListManifest.empty();
        }
    }

    /**
     * Serialises manifest into JSON bytes.
     *
     * @param manifest domain manifest
     * @return JSON encoded manifest
     */
    public byte[] writeManifest(KVListManifest manifest) {
        try {
            return objectMapper.writeValueAsBytes(Map.of(
                    "order", manifest.order(),
                    "version", manifest.version(),
                    "etag", manifest.etag(),
                    "metadata", manifest.metadata()
            ));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encode manifest", ex);
        }
    }

    /**
     * Deserialises structured data stored as JSON/YAML/Properties into a Map
     * representation.
     *
     * @param bytes   raw content
     * @param format  structured format
     * @return immutable map representation
     */
    public Map<String, Object> deserializeStructuredContent(byte[] bytes, StructuredFormat format) {
        if (bytes == null || bytes.length == 0) {
            return Map.of();
        }
        try {
            return switch (format) {
                case JSON -> objectMapper.readValue(bytes, OBJECT_MAP);
                case YAML -> yamlMapper.readValue(bytes, OBJECT_MAP);
                case PROPERTIES -> decodeProperties(bytes);
            };
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to decode structured content: " + ex.getMessage(), ex);
        }
    }

    /**
     * Serialises a map representation into a structured format.
     *
     * @param data    map data
     * @param format  desired format
     * @return encoded bytes
     */
    public byte[] serializeStructuredContent(Map<String, Object> data, StructuredFormat format) {
        Map<String, Object> safeData = data == null ? Collections.emptyMap() : new LinkedHashMap<>(data);
        try {
            return switch (format) {
                case JSON -> objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(safeData);
                case YAML -> yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(safeData);
                case PROPERTIES -> encodeProperties(safeData);
            };
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encode structured content: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> decodeProperties(byte[] bytes) throws Exception {
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(bytes));
        Map<String, Object> map = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }
        return Collections.unmodifiableMap(map);
    }

    private byte[] encodeProperties(Map<String, Object> data) throws Exception {
        Properties properties = new Properties();
        data.forEach((key, value) -> properties.put(key, value == null ? "" : value.toString()));
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            properties.store(baos, "generated by config-control-service");
            return baos.toByteArray();
        }
    }

    /**
     * Utility to convert bytes to UTF-8 string safely.
     */
    public String asString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Serialises an arbitrary object into JSON bytes.
     *
     * @param value value to serialise
     * @return JSON representation
     */
    public byte[] toJsonBytes(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialise value to JSON: " + ex.getMessage(), ex);
        }
    }
}


