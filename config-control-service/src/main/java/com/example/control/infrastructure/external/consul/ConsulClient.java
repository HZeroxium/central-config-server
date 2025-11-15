package com.example.control.infrastructure.external.consul;

import com.example.control.infrastructure.resilience.ResilienceDecoratorsFactory;
import com.example.control.infrastructure.external.fallback.ConsulFallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import com.example.control.infrastructure.config.misc.ConsulProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Resilience-enabled client for Consul HTTP API.
 * <p>
 * Read operations use Circuit Breaker, Retry, Bulkhead with cached fallbacks.
 * Write operations use Circuit Breaker and Bulkhead only (no retry for
 * idempotency).
 * </p>
 */
@Component("customConsulClient")
@Slf4j
public class ConsulClient {

    private static final String SERVICE_NAME = "consul";

    private final ResilienceDecoratorsFactory resilienceFactory;
    private final RestClient restClient;
    private final ConsulProperties consulProperties;
    private final ConsulFallback consulFallback;

    public ConsulClient(
            ResilienceDecoratorsFactory resilienceFactory,
            @Qualifier("consulRestClient") RestClient restClient,
            ConsulProperties consulProperties,
            ConsulFallback consulFallback) {
        this.resilienceFactory = resilienceFactory;
        this.restClient = restClient;
        this.consulProperties = consulProperties;
        this.consulFallback = consulFallback;
    }

    /**
     * Get all services from Consul catalog with resilience.
     */
    @Cacheable(value = "consul-services", key = "'catalog'")
    public String getServices() {
        return executeReadWithResilience(
                consulProperties.getUrl() + "/v1/catalog/services",
                "{}",
                "getServices");
    }

    /**
     * Get service details by name with resilience.
     */
    @Cacheable(value = "consul-services", key = "#serviceName")
    public String getService(String serviceName) {
        return executeReadWithResilience(
                consulProperties.getUrl() + "/v1/catalog/service/" + serviceName,
                "[]",
                "getService:" + serviceName);
    }

    /**
     * Get all instances of a service (including unhealthy) with resilience.
     */
    public String getServiceInstances(String serviceName) {
        return executeReadWithResilience(
                consulProperties.getUrl() + "/v1/health/service/" + serviceName,
                "[]",
                "getServiceInstances:" + serviceName);
    }

    /**
     * Get only healthy instances of a service with resilience.
     */
    public String getHealthyServiceInstances(String serviceName) {
        return executeReadWithResilience(
                consulProperties.getUrl() + "/v1/health/service/" + serviceName + "?passing",
                "[]",
                "getHealthyServiceInstances:" + serviceName);
    }

    /**
     * Get health checks for a service with resilience.
     */
    @Cacheable(value = "consul-health", key = "#serviceName")
    public String getServiceHealth(String serviceName) {
        return executeReadWithResilience(
                consulProperties.getUrl() + "/v1/health/checks/" + serviceName,
                "[]",
                "getServiceHealth:" + serviceName);
    }

    /**
     * Get all nodes in the cluster
     */
    public String getNodes() {
        String url = consulProperties.getUrl() + "/v1/catalog/nodes";
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
        String url = consulProperties.getUrl() + "/v1/kv/" + key;
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
        String url = consulProperties.getUrl() + "/v1/kv/" + key;
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
        String url = consulProperties.getUrl() + "/v1/kv/" + key;
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
        String url = consulProperties.getUrl() + "/v1/agent/services";
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
        String url = consulProperties.getUrl() + "/v1/agent/checks";
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
        String url = consulProperties.getUrl() + "/v1/agent/members";
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
        String url = consulProperties.getUrl() + "/v1/agent/service/register";
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
        String url = consulProperties.getUrl() + "/v1/agent/service/deregister/" + serviceId;
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
        String url = consulProperties.getUrl() + "/v1/agent/check/pass/" + checkId;
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
        String url = consulProperties.getUrl() + "/v1/agent/check/fail/" + checkId;
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
        String url = consulProperties.getUrl() + "/v1/health/state/" + state;
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
        String url = consulProperties.getUrl() + "/v1/kv/" + key;
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
        String url = consulProperties.getUrl() + "/v1/kv/" + prefix + "?recurse";
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
        String url = consulProperties.getUrl() + "/v1/kv/" + key;
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
        String url = consulProperties.getUrl() + "/v1/kv/" + key;
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
        String url = consulProperties.getUrl() + "/v1/session/create";
        log.debug("Creating session at: {}", url);
        try {
            String behavior = deleteOnInvalidate ? "delete" : "release";
            String requestBody = String.format(
                    "{\"TTL\":\"%ds\",\"Behavior\":\"%s\"}",
                    ttl.getSeconds(),
                    behavior);

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
        String url = consulProperties.getUrl() + "/v1/session/destroy/" + sessionId;
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
        String url = consulProperties.getUrl() + "/v1/kv/" + key + "?acquire=" + sessionId;
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
        String url = consulProperties.getUrl() + "/v1/kv/" + key + "?acquire=" + sessionId;
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
        String url = consulProperties.getUrl() + "/v1/kv/" + prefix + "?recurse&wait=30s&index=" + lastIndex;
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
        String url = consulProperties.getUrl() + "/v1/txn";
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
                            result.has("Index") ? result.get("Index").asLong() : null);
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
            // "set", "delete", "get", "get-tree", "check-index", "check-session", "lock",
            // "unlock"
            String key,
            byte[] value, // null for delete/get operations
            Long cas, // for CAS operations
            String session, // for session-based operations
            String acquire, // for lock acquisition
            String release // for lock release
    ) {
    }

    /**
     * Result of a transaction operation.
     */
    public record TxnOperationResult(
            String verb,
            String key,
            byte[] value,
            Long index) {
    }

    /**
     * Result of a transaction.
     */
    public record TxnResult(
            boolean success,
            List<TxnOperationResult> results,
            List<String> errors) {
    }

    /**
     * Result of a blocking query.
     */
    public record WatchResult(String data, long index) {
    }

    /**
     * Execute read operation (GET) with full resilience patterns (CB, Retry,
     * Bulkhead).
     */
    private String executeReadWithResilience(String url, String fallback, String operation) {
        Supplier<String> apiCall = () -> {
            try {
                log.debug("Consul GET {}: {}", operation, url);
                String result = restClient.get()
                        .uri(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

                // Cache successful response for future fallback
                cacheSuccessfulResponse(url, operation, result);

                return result;
            } catch (Exception e) {
                log.error("Consul API call failed: {}", operation, e);
                throw new RuntimeException("Consul API call failed: " + operation, e);
            }
        };

        Function<Throwable, String> fallbackFunction = (Throwable t) -> {
            log.warn("Consul fallback for {} due to: {}", operation, t.getMessage());

            // Try to get cached fallback based on operation type
            String cachedFallback = getCachedFallback(url, operation);
            if (cachedFallback != null && !cachedFallback.equals("{}") && !cachedFallback.equals("[]")) {
                return cachedFallback;
            }

            // Use provided fallback or empty default
            return fallback != null ? fallback : (operation.contains("health") ? "[]" : "{}");
        };
        Supplier<String> decoratedCall = resilienceFactory.decorateSupplier(
                SERVICE_NAME,
                apiCall,
                fallbackFunction);

        return decoratedCall.get();
    }

    /**
     * Cache successful response based on URL pattern.
     */
    private void cacheSuccessfulResponse(String url, String operation, String result) {
        try {
            if (url.contains("/v1/catalog/services")) {
                consulFallback.saveServiceToCache("catalog", result);
            } else if (url.contains("/v1/catalog/service/")) {
                String serviceName = extractServiceNameFromUrl(url, "/v1/catalog/service/");
                if (serviceName != null) {
                    consulFallback.saveServiceToCache(serviceName, result);
                }
            } else if (url.contains("/v1/health/service/")) {
                String serviceName = extractServiceNameFromUrl(url, "/v1/health/service/");
                if (serviceName != null) {
                    consulFallback.saveHealthToCache(serviceName, result);
                }
            } else if (url.contains("/v1/health/checks/")) {
                String serviceName = extractServiceNameFromUrl(url, "/v1/health/checks/");
                if (serviceName != null) {
                    consulFallback.saveHealthToCache(serviceName, result);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to cache Consul response for {}: {}", operation, e.getMessage());
        }
    }

    /**
     * Get cached fallback based on URL pattern.
     */
    private String getCachedFallback(String url, String operation) {
        try {
            if (url.contains("/v1/catalog/services")) {
                return consulFallback.getFallbackServices();
            } else if (url.contains("/v1/catalog/service/")) {
                String serviceName = extractServiceNameFromUrl(url, "/v1/catalog/service/");
                if (serviceName != null) {
                    return consulFallback.getFallbackService(serviceName);
                }
            } else if (url.contains("/v1/health/service/")) {
                String serviceName = extractServiceNameFromUrl(url, "/v1/health/service/");
                if (serviceName != null) {
                    return consulFallback.getFallbackHealth(serviceName);
                }
            } else if (url.contains("/v1/health/checks/")) {
                String serviceName = extractServiceNameFromUrl(url, "/v1/health/checks/");
                if (serviceName != null) {
                    return consulFallback.getFallbackHealth(serviceName);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get cached fallback for {}: {}", operation, e.getMessage());
        }
        return null;
    }

    /**
     * Extract service name from URL.
     */
    private String extractServiceNameFromUrl(String url, String prefix) {
        try {
            int index = url.indexOf(prefix);
            if (index >= 0) {
                String remaining = url.substring(index + prefix.length());
                // Remove query parameters and trailing slashes
                int queryIndex = remaining.indexOf('?');
                if (queryIndex >= 0) {
                    remaining = remaining.substring(0, queryIndex);
                }
                remaining = remaining.replaceAll("/$", "");
                return remaining.isEmpty() ? null : remaining;
            }
        } catch (Exception e) {
            log.debug("Failed to extract service name from URL: {}", url, e);
        }
        return null;
    }

    /**
     * Execute write operation (PUT/POST/DELETE) without retry (idempotency
     * concern).
     */
    private boolean executeWriteWithResilience(Supplier<Boolean> writeOperation, String operation) {
        Supplier<Boolean> decoratedCall = resilienceFactory.decorateSupplierWithoutRetry(
                SERVICE_NAME,
                writeOperation,
                false // Fallback to false on failure
        );

        try {
            return decoratedCall.get();
        } catch (Exception e) {
            log.error("Consul write operation failed: {}", operation, e);
            return false;
        }
    }
}
