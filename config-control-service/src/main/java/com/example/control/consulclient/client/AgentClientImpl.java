package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.HttpTransport;
import com.example.control.consulclient.core.QueryOptions;
import com.example.control.consulclient.core.WriteOptions;
import com.example.control.consulclient.exception.ConsulException;
import com.example.control.consulclient.model.HealthCheck;
import com.example.control.consulclient.model.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Implementation of AgentClient using HttpTransport.
 */
@Slf4j
@RequiredArgsConstructor
public class AgentClientImpl implements AgentClient {
    
    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;
    
    @Override
    public ConsulResponse<Map<String, Object>> self(QueryOptions options) {
        log.debug("Getting agent self information");
        
        try {
            String path = "/v1/agent/self";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> selfInfo = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(selfInfo, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get agent self information", e);
            throw new ConsulException("Failed to get agent self information", e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> selfNode(String nodeName, QueryOptions options) {
        log.debug("Getting agent self information for node: {}", nodeName);
        
        try {
            String path = "/v1/agent/self/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> selfInfo = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(selfInfo, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get agent self information for node: {}", nodeName, e);
            throw new ConsulException("Failed to get agent self information for node: " + nodeName, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Service>> services(QueryOptions options) {
        log.debug("Listing agent services");
        
        try {
            String path = "/v1/agent/services";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Service> services = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Service>>() {});
            
            return ConsulResponse.of(services, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to list agent services", e);
            throw new ConsulException("Failed to list agent services", e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, HealthCheck>> checks(QueryOptions options) {
        log.debug("Listing agent health checks");
        
        try {
            String path = "/v1/agent/checks";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, HealthCheck> checks = objectMapper.convertValue(node, 
                new TypeReference<Map<String, HealthCheck>>() {});
            
            return ConsulResponse.of(checks, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to list agent health checks", e);
            throw new ConsulException("Failed to list agent health checks", e);
        }
    }
    
    @Override
    public ConsulResponse<List<Map<String, Object>>> members(QueryOptions options) {
        log.debug("Getting cluster members");
        
        try {
            String path = "/v1/agent/members";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<Map<String, Object>> members = objectMapper.convertValue(node, 
                new TypeReference<List<Map<String, Object>>>() {});
            
            return ConsulResponse.of(members, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster members", e);
            throw new ConsulException("Failed to get cluster members", e);
        }
    }
    
    @Override
    public ConsulResponse<List<Map<String, Object>>> membersDatacenter(String datacenter, QueryOptions options) {
        log.debug("Getting cluster members for datacenter: {}", datacenter);
        
        try {
            String path = "/v1/agent/members/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<Map<String, Object>> members = objectMapper.convertValue(node, 
                new TypeReference<List<Map<String, Object>>>() {});
            
            return ConsulResponse.of(members, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get cluster members for datacenter: {}", datacenter, e);
            throw new ConsulException("Failed to get cluster members for datacenter: " + datacenter, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> config(QueryOptions options) {
        log.debug("Getting agent configuration");
        
        try {
            String path = "/v1/agent/config";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> config = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(config, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get agent configuration", e);
            throw new ConsulException("Failed to get agent configuration", e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> configNode(String nodeName, QueryOptions options) {
        log.debug("Getting agent configuration for node: {}", nodeName);
        
        try {
            String path = "/v1/agent/config/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> config = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(config, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get agent configuration for node: {}", nodeName, e);
            throw new ConsulException("Failed to get agent configuration for node: " + nodeName, e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> metrics(QueryOptions options) {
        log.debug("Getting agent metrics");
        
        try {
            String path = "/v1/agent/metrics";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> metrics = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(metrics, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get agent metrics", e);
            throw new ConsulException("Failed to get agent metrics", e);
        }
    }
    
    @Override
    public ConsulResponse<Map<String, Object>> metricsNode(String nodeName, QueryOptions options) {
        log.debug("Getting agent metrics for node: {}", nodeName);
        
        try {
            String path = "/v1/agent/metrics/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, Object> metrics = objectMapper.convertValue(node, 
                new TypeReference<Map<String, Object>>() {});
            
            return ConsulResponse.of(metrics, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get agent metrics for node: {}", nodeName, e);
            throw new ConsulException("Failed to get agent metrics for node: " + nodeName, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> reload(WriteOptions options) {
        log.debug("Reloading agent configuration");
        
        try {
            String path = "/v1/agent/reload";
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to reload agent configuration", e);
            throw new ConsulException("Failed to reload agent configuration", e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> reloadNode(String nodeName, WriteOptions options) {
        log.debug("Reloading agent configuration for node: {}", nodeName);
        
        try {
            String path = "/v1/agent/reload/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to reload agent configuration for node: {}", nodeName, e);
            throw new ConsulException("Failed to reload agent configuration for node: " + nodeName, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> forceLeave(String nodeName, WriteOptions options) {
        log.debug("Force leaving node: {}", nodeName);
        
        try {
            String path = "/v1/agent/force-leave/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to force leave node: {}", nodeName, e);
            throw new ConsulException("Failed to force leave node: " + nodeName, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> forceLeaveDatacenter(String nodeName, String datacenter, WriteOptions options) {
        log.debug("Force leaving node {} in datacenter: {}", nodeName, datacenter);
        
        try {
            String path = "/v1/agent/force-leave/" + nodeName + "/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to force leave node {} in datacenter: {}", nodeName, datacenter, e);
            throw new ConsulException("Failed to force leave node " + nodeName + " in datacenter: " + datacenter, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> join(String address, WriteOptions options) {
        log.debug("Joining node: {}", address);
        
        try {
            String path = "/v1/agent/join/" + address;
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to join node: {}", address, e);
            throw new ConsulException("Failed to join node: " + address, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> joinDatacenter(String address, String datacenter, WriteOptions options) {
        log.debug("Joining node {} in datacenter: {}", address, datacenter);
        
        try {
            String path = "/v1/agent/join/" + address + "/" + datacenter;
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to join node {} in datacenter: {}", address, datacenter, e);
            throw new ConsulException("Failed to join node " + address + " in datacenter: " + datacenter, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> leave(WriteOptions options) {
        log.debug("Leaving cluster");
        
        try {
            String path = "/v1/agent/leave";
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to leave cluster", e);
            throw new ConsulException("Failed to leave cluster", e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> leaveNode(String nodeName, WriteOptions options) {
        log.debug("Leaving cluster for node: {}", nodeName);
        
        try {
            String path = "/v1/agent/leave/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to leave cluster for node: {}", nodeName, e);
            throw new ConsulException("Failed to leave cluster for node: " + nodeName, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> maintenance(boolean enable, String reason, WriteOptions options) {
        log.debug("Setting maintenance mode: {} with reason: {}", enable, reason);
        
        try {
            String path = "/v1/agent/maintenance?enable=" + enable;
            if (reason != null && !reason.trim().isEmpty()) {
                path += "&reason=" + reason;
            }
            
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to set maintenance mode: {} with reason: {}", enable, reason, e);
            throw new ConsulException("Failed to set maintenance mode: " + enable + " with reason: " + reason, e);
        }
    }
    
    @Override
    public ConsulResponse<Boolean> maintenanceNode(String nodeName, boolean enable, String reason, WriteOptions options) {
        log.debug("Setting maintenance mode for node {}: {} with reason: {}", nodeName, enable, reason);
        
        try {
            String path = "/v1/agent/maintenance/" + nodeName + "?enable=" + enable;
            if (reason != null && !reason.trim().isEmpty()) {
                path += "&reason=" + reason;
            }
            
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            return ConsulResponse.of(true, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to set maintenance mode for node {}: {} with reason: {}", nodeName, enable, reason, e);
            throw new ConsulException("Failed to set maintenance mode for node " + nodeName + ": " + enable + " with reason: " + reason, e);
        }
    }
}
