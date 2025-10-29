package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.HttpTransport;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.core.WriteOptions;
import com.example.control.infrastructure.consulclient.exception.ConsulException;
import com.example.control.infrastructure.consulclient.model.Event;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;

/**
 * Implementation of EventClient using HttpTransport.
 */
@Slf4j
@RequiredArgsConstructor
public class EventClientImpl implements EventClient {

    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;

    @Override
    public ConsulResponse<List<Event>> list(QueryOptions options) {
        log.debug("Listing all events");

        try {
            String path = "/v1/event/list";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<Event> events = objectMapper.convertValue(node,
                    new TypeReference<List<Event>>() {
                    });

            return ConsulResponse.of(events, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to list events", e);
            throw new ConsulException("Failed to list events", e);
        }
    }

    @Override
    public ConsulResponse<List<Event>> listName(String name, QueryOptions options) {
        log.debug("Listing events for name: {}", name);

        try {
            String path = "/v1/event/list/" + name;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<Event> events = objectMapper.convertValue(node,
                    new TypeReference<List<Event>>() {
                    });

            return ConsulResponse.of(events, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to list events for name: {}", name, e);
            throw new ConsulException("Failed to list events for name: " + name, e);
        }
    }

    @Override
    public ConsulResponse<List<Event>> listNameDatacenter(String name, String datacenter, QueryOptions options) {
        log.debug("Listing events for name {} in datacenter: {}", name, datacenter);

        try {
            String path = "/v1/event/list/" + name + "/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<Event> events = objectMapper.convertValue(node,
                    new TypeReference<List<Event>>() {
                    });

            return ConsulResponse.of(events, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to list events for name {} in datacenter: {}", name, datacenter, e);
            throw new ConsulException("Failed to list events for name " + name + " in datacenter: " + datacenter, e);
        }
    }

    @Override
    public ConsulResponse<String> fire(String name, byte[] payload, WriteOptions options) {
        log.debug("Firing event: {}", name);

        try {
            String path = "/v1/event/fire/" + name;
            String base64Payload = Base64.getEncoder().encodeToString(payload);

            ConsulResponse<JsonNode> response = httpTransport.put(path, base64Payload, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.has("ID")) {
                return ConsulResponse.of("", response.getConsulIndex());
            }

            String eventId = node.get("ID").asText();
            return ConsulResponse.of(eventId, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to fire event: {}", name, e);
            throw new ConsulException("Failed to fire event: " + name, e);
        }
    }

    @Override
    public ConsulResponse<String> fireDatacenter(String name, byte[] payload, String datacenter, WriteOptions options) {
        log.debug("Firing event {} in datacenter: {}", name, datacenter);

        try {
            String path = "/v1/event/fire/" + name + "/" + datacenter;
            String base64Payload = Base64.getEncoder().encodeToString(payload);

            ConsulResponse<JsonNode> response = httpTransport.put(path, base64Payload, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.has("ID")) {
                return ConsulResponse.of("", response.getConsulIndex());
            }

            String eventId = node.get("ID").asText();
            return ConsulResponse.of(eventId, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to fire event {} in datacenter: {}", name, datacenter, e);
            throw new ConsulException("Failed to fire event " + name + " in datacenter: " + datacenter, e);
        }
    }

    @Override
    public ConsulResponse<String> fireNode(String name, byte[] payload, String nodeFilter, WriteOptions options) {
        log.debug("Firing event {} with node filter: {}", name, nodeFilter);

        try {
            String path = "/v1/event/fire/" + name + "?node=" + nodeFilter;
            String base64Payload = Base64.getEncoder().encodeToString(payload);

            ConsulResponse<JsonNode> response = httpTransport.put(path, base64Payload, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.has("ID")) {
                return ConsulResponse.of("", response.getConsulIndex());
            }

            String eventId = node.get("ID").asText();
            return ConsulResponse.of(eventId, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to fire event {} with node filter: {}", name, nodeFilter, e);
            throw new ConsulException("Failed to fire event " + name + " with node filter: " + nodeFilter, e);
        }
    }

    @Override
    public ConsulResponse<String> fireService(String name, byte[] payload, String serviceFilter, WriteOptions options) {
        log.debug("Firing event {} with service filter: {}", name, serviceFilter);

        try {
            String path = "/v1/event/fire/" + name + "?service=" + serviceFilter;
            String base64Payload = Base64.getEncoder().encodeToString(payload);

            ConsulResponse<JsonNode> response = httpTransport.put(path, base64Payload, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.has("ID")) {
                return ConsulResponse.of("", response.getConsulIndex());
            }

            String eventId = node.get("ID").asText();
            return ConsulResponse.of(eventId, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to fire event {} with service filter: {}", name, serviceFilter, e);
            throw new ConsulException("Failed to fire event " + name + " with service filter: " + serviceFilter, e);
        }
    }

    @Override
    public ConsulResponse<String> fireTag(String name, byte[] payload, String tagFilter, WriteOptions options) {
        log.debug("Firing event {} with tag filter: {}", name, tagFilter);

        try {
            String path = "/v1/event/fire/" + name + "?tag=" + tagFilter;
            String base64Payload = Base64.getEncoder().encodeToString(payload);

            ConsulResponse<JsonNode> response = httpTransport.put(path, base64Payload, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.has("ID")) {
                return ConsulResponse.of("", response.getConsulIndex());
            }

            String eventId = node.get("ID").asText();
            return ConsulResponse.of(eventId, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to fire event {} with tag filter: {}", name, tagFilter, e);
            throw new ConsulException("Failed to fire event " + name + " with tag filter: " + tagFilter, e);
        }
    }

    @Override
    public ConsulResponse<String> fireFilters(String name, byte[] payload, String nodeFilter, String serviceFilter, String tagFilter, WriteOptions options) {
        log.debug("Firing event {} with filters - node: {}, service: {}, tag: {}", name, nodeFilter, serviceFilter, tagFilter);

        try {
            StringBuilder pathBuilder = new StringBuilder("/v1/event/fire/" + name + "?");
            boolean first = true;

            if (nodeFilter != null && !nodeFilter.trim().isEmpty()) {
                pathBuilder.append("node=").append(nodeFilter);
                first = false;
            }

            if (serviceFilter != null && !serviceFilter.trim().isEmpty()) {
                if (!first) pathBuilder.append("&");
                pathBuilder.append("service=").append(serviceFilter);
                first = false;
            }

            if (tagFilter != null && !tagFilter.trim().isEmpty()) {
                if (!first) pathBuilder.append("&");
                pathBuilder.append("tag=").append(tagFilter);
            }

            String path = pathBuilder.toString();
            String base64Payload = Base64.getEncoder().encodeToString(payload);

            ConsulResponse<JsonNode> response = httpTransport.put(path, base64Payload, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.has("ID")) {
                return ConsulResponse.of("", response.getConsulIndex());
            }

            String eventId = node.get("ID").asText();
            return ConsulResponse.of(eventId, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to fire event {} with filters - node: {}, service: {}, tag: {}", name, nodeFilter, serviceFilter, tagFilter, e);
            throw new ConsulException("Failed to fire event " + name + " with filters", e);
        }
    }
}
