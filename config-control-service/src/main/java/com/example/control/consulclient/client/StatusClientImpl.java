package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.HttpTransport;
import com.example.control.consulclient.core.QueryOptions;
import com.example.control.consulclient.exception.ConsulException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Implementation of StatusClient using HttpTransport.
 */
@Slf4j
@RequiredArgsConstructor
public class StatusClientImpl implements StatusClient {
    
    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;
    
    @Override
    public ConsulResponse<String> leader(QueryOptions options) {
        log.debug("Getting cluster leader");
        
        try {
            String path = "/v1/status/leader";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of("", response.getConsulIndex());
            }
            
            String leader = node.asText();
            return ConsulResponse.of(leader, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster leader", e);
            throw new ConsulException("Failed to get cluster leader", e);
        }
    }
    
    @Override
    public ConsulResponse<String> leaderDatacenter(String datacenter, QueryOptions options) {
        log.debug("Getting cluster leader for datacenter: {}", datacenter);
        
        try {
            String path = "/v1/status/leader/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of("", response.getConsulIndex());
            }
            
            String leader = node.asText();
            return ConsulResponse.of(leader, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster leader for datacenter: {}", datacenter, e);
            throw new ConsulException("Failed to get cluster leader for datacenter: " + datacenter, e);
        }
    }
    
    @Override
    public ConsulResponse<List<String>> peers(QueryOptions options) {
        log.debug("Getting cluster peers");
        
        try {
            String path = "/v1/status/peers";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<String> peers = objectMapper.convertValue(node, 
                new TypeReference<List<String>>() {});
            
            return ConsulResponse.of(peers, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster peers", e);
            throw new ConsulException("Failed to get cluster peers", e);
        }
    }
    
    @Override
    public ConsulResponse<List<String>> peersDatacenter(String datacenter, QueryOptions options) {
        log.debug("Getting cluster peers for datacenter: {}", datacenter);
        
        try {
            String path = "/v1/status/peers/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<String> peers = objectMapper.convertValue(node, 
                new TypeReference<List<String>>() {});
            
            return ConsulResponse.of(peers, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster peers for datacenter: {}", datacenter, e);
            throw new ConsulException("Failed to get cluster peers for datacenter: " + datacenter, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> status(QueryOptions options) {
        log.debug("Getting cluster status");
        
        try {
            String path = "/v1/status";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> status = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(status, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster status", e);
            throw new ConsulException("Failed to get cluster status", e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> statusDatacenter(String datacenter, QueryOptions options) {
        log.debug("Getting cluster status for datacenter: {}", datacenter);
        
        try {
            String path = "/v1/status/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> status = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(status, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster status for datacenter: {}", datacenter, e);
            throw new ConsulException("Failed to get cluster status for datacenter: " + datacenter, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> statusNode(String nodeName, QueryOptions options) {
        log.debug("Getting cluster status for node: {}", nodeName);
        
        try {
            String path = "/v1/status/node/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> status = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(status, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster status for node: {}", nodeName, e);
            throw new ConsulException("Failed to get cluster status for node: " + nodeName, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> statusNodeDatacenter(String nodeName, String datacenter, QueryOptions options) {
        log.debug("Getting cluster status for node {} in datacenter: {}", nodeName, datacenter);
        
        try {
            String path = "/v1/status/node/" + nodeName + "/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> status = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(status, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster status for node {} in datacenter: {}", nodeName, datacenter, e);
            throw new ConsulException("Failed to get cluster status for node " + nodeName + " in datacenter: " + datacenter, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> statusService(String serviceName, QueryOptions options) {
        log.debug("Getting cluster status for service: {}", serviceName);
        
        try {
            String path = "/v1/status/service/" + serviceName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> status = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(status, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster status for service: {}", serviceName, e);
            throw new ConsulException("Failed to get cluster status for service: " + serviceName, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> statusServiceDatacenter(String serviceName, String datacenter, QueryOptions options) {
        log.debug("Getting cluster status for service {} in datacenter: {}", serviceName, datacenter);
        
        try {
            String path = "/v1/status/service/" + serviceName + "/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> status = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(status, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster status for service {} in datacenter: {}", serviceName, datacenter, e);
            throw new ConsulException("Failed to get cluster status for service " + serviceName + " in datacenter: " + datacenter, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> statusCheck(String checkId, QueryOptions options) {
        log.debug("Getting cluster status for check: {}", checkId);
        
        try {
            String path = "/v1/status/check/" + checkId;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> status = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(status, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster status for check: {}", checkId, e);
            throw new ConsulException("Failed to get cluster status for check: " + checkId, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> statusCheckDatacenter(String checkId, String datacenter, QueryOptions options) {
        log.debug("Getting cluster status for check {} in datacenter: {}", checkId, datacenter);
        
        try {
            String path = "/v1/status/check/" + checkId + "/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> status = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(status, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster status for check {} in datacenter: {}", checkId, datacenter, e);
            throw new ConsulException("Failed to get cluster status for check " + checkId + " in datacenter: " + datacenter, e);
        }
    }
}
