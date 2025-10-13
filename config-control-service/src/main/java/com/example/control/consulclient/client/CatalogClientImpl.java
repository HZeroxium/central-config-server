package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.HttpTransport;
import com.example.control.consulclient.core.QueryOptions;
import com.example.control.consulclient.exception.ConsulException;
import com.example.control.consulclient.model.Node;
import com.example.control.consulclient.model.ServiceEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Implementation of CatalogClient using HttpTransport.
 */
@Slf4j
@RequiredArgsConstructor
public class CatalogClientImpl implements CatalogClient {
    
    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;
    
    @Override
    public ConsulResponse<Map<String, List<String>>> services(QueryOptions options) {
        log.debug("Listing catalog services");
        
        try {
            String path = "/v1/catalog/services";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null) {
                return ConsulResponse.of(Map.of(), response.getConsulIndex());
            }
            
            Map<String, List<String>> services = objectMapper.convertValue(node, 
                new TypeReference<Map<String, List<String>>>() {});
            
            return ConsulResponse.of(services, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to list catalog services", e);
            throw new ConsulException("Failed to list catalog services", e);
        }
    }
    
    @Override
    public ConsulResponse<List<ServiceEntry>> service(String serviceName, QueryOptions options) {
        log.debug("Getting catalog service: {}", serviceName);
        
        try {
            String path = "/v1/catalog/service/" + serviceName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<ServiceEntry> serviceEntries = objectMapper.convertValue(node, 
                new TypeReference<List<ServiceEntry>>() {});
            
            return ConsulResponse.of(serviceEntries, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get catalog service: {}", serviceName, e);
            throw new ConsulException("Failed to get catalog service: " + serviceName, e);
        }
    }
    
    @Override
    public ConsulResponse<List<Node>> nodes(QueryOptions options) {
        log.debug("Listing catalog nodes");
        
        try {
            String path = "/v1/catalog/nodes";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<Node> nodes = objectMapper.convertValue(node, 
                new TypeReference<List<Node>>() {});
            
            return ConsulResponse.of(nodes, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to list catalog nodes", e);
            throw new ConsulException("Failed to list catalog nodes", e);
        }
    }
    
    @Override
    public ConsulResponse<List<ServiceEntry>> nodeServices(String nodeName, QueryOptions options) {
        log.debug("Getting services for node: {}", nodeName);
        
        try {
            String path = "/v1/catalog/node/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.has("Services")) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            JsonNode servicesNode = node.get("Services");
            if (!servicesNode.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<ServiceEntry> serviceEntries = objectMapper.convertValue(servicesNode, 
                new TypeReference<List<ServiceEntry>>() {});
            
            return ConsulResponse.of(serviceEntries, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to get services for node: {}", nodeName, e);
            throw new ConsulException("Failed to get services for node: " + nodeName, e);
        }
    }
    
    @Override
    public ConsulResponse<List<String>> datacenters() {
        log.debug("Listing datacenters");
        
        try {
            String path = "/v1/catalog/datacenters";
            ConsulResponse<JsonNode> response = httpTransport.get(path, null, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<String> datacenters = objectMapper.convertValue(node, 
                new TypeReference<List<String>>() {});
            
            return ConsulResponse.of(datacenters, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to list datacenters", e);
            throw new ConsulException("Failed to list datacenters", e);
        }
    }
}
