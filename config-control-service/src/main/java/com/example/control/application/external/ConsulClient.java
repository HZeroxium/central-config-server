package com.example.control.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Client for interacting with Consul HTTP API
 */
@Component("customConsulClient")
@RequiredArgsConstructor
public class ConsulClient {

    private static final Logger log = LoggerFactory.getLogger(ConsulClient.class);
    private final RestClient restClient = RestClient.create();

    @Value("${consul.url:http://localhost:8500}")
    private String consulUrl;

    /**
     * Get all services from Consul catalog
     */
    @Cacheable(value = "consul-services", key = "'catalog'")
    public String getServices() {
        String url = consulUrl + "/v1/catalog/services";
        log.debug("Getting services from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get services from Consul", e);
            throw new RuntimeException("Consul API call failed: getServices", e);
        }
    }

    /**
     * Get service details by name
     */
    @Cacheable(value = "consul-services", key = "#serviceName")
    public String getService(String serviceName) {
        String url = consulUrl + "/v1/catalog/service/" + serviceName;
        log.debug("Getting service details from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get service details for: {}", serviceName, e);
            throw new RuntimeException("Consul API call failed: getService", e);
        }
    }

    /**
     * Get all instances of a service (including unhealthy)
     */
    public String getServiceInstances(String serviceName) {
        String url = consulUrl + "/v1/health/service/" + serviceName;
        log.debug("Getting service instances from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get service instances for: {}", serviceName, e);
            throw new RuntimeException("Consul API call failed: getServiceInstances", e);
        }
    }

    /**
     * Get only healthy instances of a service
     */
    public String getHealthyServiceInstances(String serviceName) {
        String url = consulUrl + "/v1/health/service/" + serviceName + "?passing";
        log.debug("Getting healthy service instances from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get healthy service instances for: {}", serviceName, e);
            throw new RuntimeException("Consul API call failed: getHealthyServiceInstances", e);
        }
    }

    /**
     * Get health checks for a service
     */
    @Cacheable(value = "consul-health", key = "#serviceName")
    public String getServiceHealth(String serviceName) {
        String url = consulUrl + "/v1/health/checks/" + serviceName;
        log.debug("Getting service health from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get service health for: {}", serviceName, e);
            throw new RuntimeException("Consul API call failed: getServiceHealth", e);
        }
    }

    /**
     * Get all nodes in the cluster
     */
    public String getNodes() {
        String url = consulUrl + "/v1/catalog/nodes";
        log.debug("Getting nodes from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get nodes from Consul", e);
            throw new RuntimeException("Consul API call failed: getNodes", e);
        }
    }

    /**
     * Get value from KV store
     */
    public String getKVValue(String key, boolean recurse) {
        String url = consulUrl + "/v1/kv/" + key;
        if (recurse) {
            url += "?recurse";
        }
        log.debug("Getting KV value from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get KV value for key: {}", key, e);
            throw new RuntimeException("Consul API call failed: getKVValue", e);
        }
    }

    /**
     * Set value in KV store
     */
    public boolean setKVValue(String key, String value) {
        String url = consulUrl + "/v1/kv/" + key;
        log.debug("Setting KV value at: {}", url);
        try {
            restClient.put()
                    .uri(url)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(value)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to set KV value for key: {}", key, e);
            return false;
        }
    }

    /**
     * Delete value from KV store
     */
    public boolean deleteKVValue(String key) {
        String url = consulUrl + "/v1/kv/" + key;
        log.debug("Deleting KV value at: {}", url);
        try {
            restClient.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to delete KV value for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get services registered on local agent
     */
    public String getAgentServices() {
        String url = consulUrl + "/v1/agent/services";
        log.debug("Getting agent services from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get agent services from Consul", e);
            throw new RuntimeException("Consul API call failed: getAgentServices", e);
        }
    }

    /**
     * Get health checks on local agent
     */
    public String getAgentChecks() {
        String url = consulUrl + "/v1/agent/checks";
        log.debug("Getting agent checks from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get agent checks from Consul", e);
            throw new RuntimeException("Consul API call failed: getAgentChecks", e);
        }
    }

    /**
     * Get cluster members from local agent
     */
    public String getAgentMembers() {
        String url = consulUrl + "/v1/agent/members";
        log.debug("Getting agent members from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get agent members from Consul", e);
            throw new RuntimeException("Consul API call failed: getAgentMembers", e);
        }
    }

    /**
     * Register a service with the local agent
     */
    public boolean registerService(String serviceJson) {
        String url = consulUrl + "/v1/agent/service/register";
        log.debug("Registering service at: {}", url);
        try {
            restClient.put()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(serviceJson)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to register service", e);
            return false;
        }
    }

    /**
     * Deregister a service from the local agent
     */
    public boolean deregisterService(String serviceId) {
        String url = consulUrl + "/v1/agent/service/deregister/" + serviceId;
        log.debug("Deregistering service at: {}", url);
        try {
            restClient.put()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to deregister service: {}", serviceId, e);
            return false;
        }
    }

    /**
     * Mark a TTL check as passing
     */
    public boolean passCheck(String checkId) {
        String url = consulUrl + "/v1/agent/check/pass/" + checkId;
        log.debug("Passing check at: {}", url);
        try {
            restClient.put()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to pass check: {}", checkId, e);
            return false;
        }
    }

    /**
     * Mark a TTL check as failing
     */
    public boolean failCheck(String checkId) {
        String url = consulUrl + "/v1/agent/check/fail/" + checkId;
        log.debug("Failing check at: {}", url);
        try {
            restClient.put()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to fail check: {}", checkId, e);
            return false;
        }
    }

    /**
     * Get health state (passing, warning, critical)
     */
    public String getHealthState(String state) {
        String url = consulUrl + "/v1/health/state/" + state;
        log.debug("Getting health state from: {}", url);
        try {
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get health state: {}", state, e);
            throw new RuntimeException("Consul API call failed: getHealthState", e);
        }
    }

    // ========== KV Store Operations ==========

    /**
     * Get KV value as JsonNode for easier parsing.
     */
    public Optional<JsonNode> kvGetJson(String key) {
        String url = consulUrl + "/v1/kv/" + key;
        log.debug("Getting KV value from: {}", url);
        try {
            String response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.trim().isEmpty() || response.equals("null")) {
                return Optional.empty();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return Optional.of(node);
        } catch (Exception e) {
            log.error("Failed to get KV value for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * List KV values with prefix as JsonNode array.
     */
    public List<JsonNode> kvListJson(String prefix) {
        String url = consulUrl + "/v1/kv/" + prefix + "?recurse";
        log.debug("Listing KV values from: {}", url);
        try {
            String response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.trim().isEmpty() || response.equals("null")) {
                return List.of();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            if (node.isArray()) {
                List<JsonNode> nodes = new java.util.ArrayList<>();
                node.elements().forEachRemaining(nodes::add);
                return nodes;
            }
            return List.of(node);
        } catch (Exception e) {
            log.error("Failed to list KV values for prefix: {}", prefix, e);
            return List.of();
        }
    }

    /**
     * Put KV value with CAS support.
     */
    public boolean kvPut(String key, byte[] value, Long cas) {
        String url = consulUrl + "/v1/kv/" + key;
        if (cas != null) {
            url += "?cas=" + cas;
        }
        log.debug("Putting KV value at: {}", url);
        try {
            String base64Value = Base64.getEncoder().encodeToString(value);
            restClient.put()
                    .uri(url)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(base64Value)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to put KV value for key: {}", key, e);
            return false;
        }
    }

    /**
     * Delete KV value with CAS support.
     */
    public boolean kvDelete(String key, Long cas) {
        String url = consulUrl + "/v1/kv/" + key;
        if (cas != null) {
            url += "?cas=" + cas;
        }
        log.debug("Deleting KV value at: {}", url);
        try {
            restClient.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to delete KV value for key: {}", key, e);
            return false;
        }
    }

    /**
     * Create a Consul session.
     */
    public String createSession(Duration ttl, boolean deleteOnInvalidate) {
        String url = consulUrl + "/v1/session/create";
        log.debug("Creating session at: {}", url);
        try {
            String behavior = deleteOnInvalidate ? "delete" : "release";
            String requestBody = String.format(
                    "{\"TTL\":\"%ds\",\"Behavior\":\"%s\"}",
                    ttl.getSeconds(),
                    behavior
            );

            String response = restClient.put()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return node.get("ID").asText();
        } catch (Exception e) {
            log.error("Failed to create session", e);
            throw new RuntimeException("Consul API call failed: createSession", e);
        }
    }

    /**
     * Destroy a Consul session.
     */
    public boolean destroySession(String sessionId) {
        String url = consulUrl + "/v1/session/destroy/" + sessionId;
        log.debug("Destroying session at: {}", url);
        try {
            restClient.put()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Failed to destroy session: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Put KV value with session (for locks).
     */
    public boolean putWithAcquire(String key, byte[] value, String sessionId) {
        String url = consulUrl + "/v1/kv/" + key + "?acquire=" + sessionId;
        log.debug("Putting KV value with acquire at: {}", url);
        try {
            String base64Value = Base64.getEncoder().encodeToString(value);
            String response = restClient.put()
                    .uri(url)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(base64Value)
                    .retrieve()
                    .body(String.class);

            return "true".equals(response);
        } catch (Exception e) {
            log.error("Failed to put KV value with acquire for key: {}", key, e);
            return false;
        }
    }

    /**
     * Put KV value with session (for ephemeral keys).
     */
    public boolean putWithSession(String key, byte[] value, String sessionId) {
        String url = consulUrl + "/v1/kv/" + key + "?acquire=" + sessionId;
        log.debug("Putting KV value with session at: {}", url);
        try {
            String base64Value = Base64.getEncoder().encodeToString(value);
            String response = restClient.put()
                    .uri(url)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(base64Value)
                    .retrieve()
                    .body(String.class);

            return "true".equals(response);
        } catch (Exception e) {
            log.error("Failed to put KV value with session for key: {}", key, e);
            return false;
        }
    }

    /**
     * Blocking query for watching changes.
     */
    public WatchResult kvListBlockingWithIndex(String prefix, long lastIndex) {
        String url = consulUrl + "/v1/kv/" + prefix + "?recurse&wait=30s&index=" + lastIndex;
        log.debug("Blocking KV query from: {}", url);
        try {
            String response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);

            long newIndex = lastIndex;
            if (node.isArray() && node.size() > 0) {
                JsonNode first = node.get(0);
                newIndex = first.get("ModifyIndex").asLong();
            }

            return new WatchResult(response, newIndex);
        } catch (Exception e) {
            log.error("Failed to perform blocking KV query for prefix: {}", prefix, e);
            return new WatchResult("[]", lastIndex);
        }
    }

    /**
     * Execute a transaction using Consul's /v1/txn endpoint.
     */
    public TxnResult kvTxn(List<TxnOperation> operations) {
        String url = consulUrl + "/v1/txn";
        log.debug("Executing Consul transaction with {} operations", operations.size());

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode txnRequest = mapper.createObjectNode();
            ArrayNode opsArray = mapper.createArrayNode();

            for (TxnOperation op : operations) {
                ObjectNode opNode = mapper.createObjectNode();
                opNode.put("Verb", op.verb());
                opNode.put("Key", op.key());

                if (op.value() != null) {
                    opNode.put("Value", Base64.getEncoder().encodeToString(op.value()));
                }

                if (op.cas() != null) {
                    opNode.put("Index", op.cas());
                }

                if (op.session() != null) {
                    opNode.put("Session", op.session());
                }

                if (op.acquire() != null) {
                    opNode.put("Acquire", op.acquire());
                }

                if (op.release() != null) {
                    opNode.put("Release", op.release());
                }

                opsArray.add(opNode);
            }

            txnRequest.set("Operations", opsArray);

            String requestBody = mapper.writeValueAsString(txnRequest);

            String response = restClient.put()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode responseNode = mapper.readTree(response);
            ArrayNode results = (ArrayNode) responseNode.get("Results");
            ArrayNode errors = (ArrayNode) responseNode.get("Errors");

            List<TxnOperationResult> operationResults = new java.util.ArrayList<>();
            if (results != null) {
                for (JsonNode result : results) {
                    TxnOperationResult opResult = new TxnOperationResult(
                            result.get("Verb").asText(),
                            result.get("Key").asText(),
                            result.has("Value") ? Base64.getDecoder().decode(result.get("Value").asText()) : null,
                            result.has("Index") ? result.get("Index").asLong() : null
                    );
                    operationResults.add(opResult);
                }
            }

            List<String> errorMessages = new java.util.ArrayList<>();
            if (errors != null) {
                for (JsonNode error : errors) {
                    errorMessages.add(error.get("What").asText());
                }
            }

            boolean success = errorMessages.isEmpty();
            return new TxnResult(success, operationResults, errorMessages);

        } catch (Exception e) {
            log.error("Failed to execute Consul transaction", e);
            return new TxnResult(false, List.of(), List.of("Transaction failed: " + e.getMessage()));
        }
    }

    /**
     * Transaction operation for Consul /v1/txn endpoint.
     */
    public record TxnOperation(
            String verb,
            // "set", "delete", "get", "get-tree", "check-index", "check-session", "lock", "unlock"
            String key,
            byte[] value,          // null for delete/get operations
            Long cas,              // for CAS operations
            String session,        // for session-based operations
            String acquire,        // for lock acquisition
            String release         // for lock release
    ) {
    }

    /**
     * Result of a transaction operation.
     */
    public record TxnOperationResult(
            String verb,
            String key,
            byte[] value,
            Long index
    ) {
    }

    /**
     * Result of a transaction.
     */
    public record TxnResult(
            boolean success,
            List<TxnOperationResult> results,
            List<String> errors
    ) {
    }

    /**
     * Result of a blocking query.
     */
    public record WatchResult(String data, long index) {
    }
}
