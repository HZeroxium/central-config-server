package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.HttpTransport;
import com.example.control.consulclient.core.QueryOptions;
import com.example.control.consulclient.core.WriteOptions;
import com.example.control.consulclient.exception.ConsulException;
import com.example.control.consulclient.model.KVPair;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

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
        log.debug("Getting KV key: {}", key);
        
        try {
            String path = "/v1/kv/" + key;
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
            
            List<KVPair> kvPairs = objectMapper.convertValue(node, new TypeReference<List<KVPair>>() {});
            return ConsulResponse.of(kvPairs, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to list KV keys with prefix: {}", prefix, e);
            throw new ConsulException("Failed to list KV keys with prefix: " + prefix, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> put(String key, byte[] value, WriteOptions options, Long cas) {
        log.debug("Putting KV key: {} with CAS: {}", key, cas);
        
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
            
            // PUT the raw bytes (Consul expects raw bytes, not base64)
            ConsulResponse<Boolean> response = httpTransport.put(fullPath, value, options, Boolean.class);
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
