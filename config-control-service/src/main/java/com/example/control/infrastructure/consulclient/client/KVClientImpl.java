package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.HttpTransport;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.core.WriteOptions;
import com.example.control.infrastructure.consulclient.exception.ConsulException;
import com.example.control.infrastructure.consulclient.model.KVPair;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import org.springframework.web.client.HttpClientErrorException;

/**
 * Implementation of KVClient using HttpTransport.
 */
@Slf4j
@RequiredArgsConstructor
public class KVClientImpl implements KVClient {

    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;

    @Override
    public ConsulResponse<Optional<KVPair>> get(String key, QueryOptions options) {
        log.debug("Getting KV key: {} with raw: {}", key, options != null && Boolean.TRUE.equals(options.getRaw()));

        try {
            String path = "/v1/kv/" + key;
            
            // When raw=true, Consul returns raw bytes (text/plain), not JSON
            if (options != null && Boolean.TRUE.equals(options.getRaw())) {
                ConsulResponse<byte[]> rawResponse = httpTransport.get(path, options, byte[].class);
                byte[] rawBytes = rawResponse.getBody();
                if (rawBytes == null || rawBytes.length == 0) {
                    return ConsulResponse.of(Optional.empty(), rawResponse.getConsulIndex());
                }
                // Create KVPair with raw bytes (will be base64 encoded in the value field)
                KVPair kvPair = KVPair.withValueBytes(key, rawBytes, 0, 0, rawResponse.getConsulIndex() != null ? rawResponse.getConsulIndex() : 0, 0, null);
                return ConsulResponse.of(Optional.of(kvPair), rawResponse.getConsulIndex());
            }
            
            // Normal JSON response
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || node.isEmpty()) {
                return ConsulResponse.of(Optional.empty(), response.getConsulIndex());
            }

            // Consul returns array even for single key
            if (node.isArray()) {
                if (node.size() == 0) {
                    return ConsulResponse.of(Optional.empty(), response.getConsulIndex());
                }
                KVPair kvPair = objectMapper.treeToValue(node.get(0), KVPair.class);
                return ConsulResponse.of(Optional.of(kvPair), response.getConsulIndex());
            } else {
                KVPair kvPair = objectMapper.treeToValue(node, KVPair.class);
                return ConsulResponse.of(Optional.of(kvPair), response.getConsulIndex());
            }

        } catch (HttpClientErrorException.NotFound e) {
            // Consul returns 404 when key doesn't exist - map to Optional.empty()
            log.debug("KV key not found: {}", key);
            return ConsulResponse.of(Optional.empty(), null);
        } catch (Exception e) {
            log.error("Failed to get KV key: {}", key, e);
            throw new ConsulException("Failed to get KV key: " + key, e);
        }
    }

    @Override
    public ConsulResponse<List<KVPair>> list(String prefix, QueryOptions options) {
        log.debug("Listing KV keys with prefix: {}", prefix);

        try {
            String path = "/v1/kv/" + prefix;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<KVPair> kvPairs = objectMapper.convertValue(node, new TypeReference<List<KVPair>>() {
            });
            return ConsulResponse.of(kvPairs, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to list KV keys with prefix: {}", prefix, e);
            throw new ConsulException("Failed to list KV keys with prefix: " + prefix, e);
        }
    }

    @Override
    public ConsulResponse<List<String>> listKeys(String prefix, QueryOptions options) {
        log.debug("Listing KV keys (keys-only) with prefix: {}", prefix);

        try {
            String path = "/v1/kv/" + prefix;
            
            // Ensure keys=true and recurse in options
            QueryOptions keysOptions = QueryOptions.builder()
                    .consistent(options != null && Boolean.TRUE.equals(options.getConsistent()))
                    .stale(options != null && Boolean.TRUE.equals(options.getStale()))
                    .recurse(options == null || Boolean.TRUE.equals(options.getRecurse()))
                    .keys(true)
                    .separator(options != null ? options.getSeparator() : null)
                    .build();
            
            ConsulResponse<JsonNode> response = httpTransport.get(path, keysOptions, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            // When keys=true, Consul returns array of strings
            List<String> keys = new java.util.ArrayList<>();
            for (JsonNode keyNode : node) {
                if (keyNode.isTextual()) {
                    keys.add(keyNode.asText());
                }
            }
            
            return ConsulResponse.of(keys, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to list KV keys with prefix: {}", prefix, e);
            throw new ConsulException("Failed to list KV keys with prefix: " + prefix, e);
        }
    }

    @Override
    public ConsulResponse<Boolean> put(String key, byte[] value, WriteOptions options, Long cas) {
        log.debug("Putting KV key: {} with CAS: {}, flags: {}", key, cas, options != null ? options.getFlags() : null);

        try {
            String path = "/v1/kv/" + key;

            // Merge cas parameter into WriteOptions if provided
            WriteOptions finalOptions = options;
            if (cas != null) {
                // If cas is provided as parameter, merge it into options
                WriteOptions.WriteOptionsBuilder builder = options != null 
                    ? WriteOptions.builder()
                        .datacenter(options.getDatacenter())
                        .token(options.getToken())
                        .timeout(options.getTimeout())
                        .recurse(options.getRecurse())
                        .flags(options.getFlags())
                        .cas(cas)
                    : WriteOptions.builder().cas(cas);
                finalOptions = builder.build();
            } else if (options != null && options.getCas() != null) {
                // Use cas from options if not provided as parameter
                finalOptions = options;
            }

            // PUT the raw bytes (Consul expects raw bytes, not base64)
            // HttpTransport will build the query string from WriteOptions
            ConsulResponse<Boolean> response = httpTransport.put(path, value, finalOptions, Boolean.class);
            return response;

        } catch (Exception e) {
            log.error("Failed to put KV key: {}", key, e);
            throw new ConsulException("Failed to put KV key: " + key, e);
        }
    }

    @Override
    public ConsulResponse<Boolean> delete(String key, WriteOptions options, Long cas) {
        log.debug("Deleting KV key: {} with CAS: {}", key, cas);

        try {
            String path = "/v1/kv/" + key;

            // Build query parameters
            StringBuilder query = new StringBuilder();
            if (cas != null) {
                query.append("cas=").append(cas);
            }

            // Add write options
            if (options != null) {
                String optionsQuery = options.buildQueryString();
                if (!optionsQuery.isEmpty()) {
                    if (query.length() > 0) {
                        query.append("&");
                    }
                    query.append(optionsQuery);
                }
            }

            String fullPath = path;
            if (query.length() > 0) {
                fullPath += "?" + query;
            }

            ConsulResponse<Boolean> response = httpTransport.delete(fullPath, options, Boolean.class);
            return response;

        } catch (Exception e) {
            log.error("Failed to delete KV key: {}", key, e);
            throw new ConsulException("Failed to delete KV key: " + key, e);
        }
    }

    @Override
    public ConsulResponse<Boolean> acquire(String key, byte[] value, String sessionId, WriteOptions options) {
        log.debug("Acquiring lock on KV key: {} with session: {}", key, sessionId);

        try {
            String path = "/v1/kv/" + key;

            // Build query parameters
            StringBuilder query = new StringBuilder();
            query.append("acquire=").append(sessionId);

            // Add write options
            if (options != null) {
                String optionsQuery = options.buildQueryString();
                if (!optionsQuery.isEmpty()) {
                    query.append("&").append(optionsQuery);
                }
            }

            String fullPath = path + "?" + query;

            // PUT the raw bytes
            ConsulResponse<Boolean> response = httpTransport.put(fullPath, value, options, Boolean.class);
            return response;

        } catch (Exception e) {
            log.error("Failed to acquire lock on KV key: {}", key, e);
            throw new ConsulException("Failed to acquire lock on KV key: " + key, e);
        }
    }

    @Override
    public ConsulResponse<Boolean> release(String key, byte[] value, String sessionId, WriteOptions options) {
        log.debug("Releasing lock on KV key: {} with session: {}", key, sessionId);

        try {
            String path = "/v1/kv/" + key;

            // Build query parameters
            StringBuilder query = new StringBuilder();
            query.append("release=").append(sessionId);

            // Add write options
            if (options != null) {
                String optionsQuery = options.buildQueryString();
                if (!optionsQuery.isEmpty()) {
                    query.append("&").append(optionsQuery);
                }
            }

            String fullPath = path + "?" + query;

            // PUT the raw bytes
            ConsulResponse<Boolean> response = httpTransport.put(fullPath, value, options, Boolean.class);
            return response;

        } catch (Exception e) {
            log.error("Failed to release lock on KV key: {}", key, e);
            throw new ConsulException("Failed to release lock on KV key: " + key, e);
        }
    }
}
