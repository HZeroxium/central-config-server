package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.HttpTransport;
import com.example.control.consulclient.core.QueryOptions;
import com.example.control.consulclient.core.WriteOptions;
import com.example.control.consulclient.exception.ConsulException;
import com.example.control.consulclient.model.Session;
import com.example.control.consulclient.model.SessionCreateRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of SessionClient using HttpTransport.
 */
@Slf4j
@RequiredArgsConstructor
public class SessionClientImpl implements SessionClient {
    
    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;
    
    @Override
    public ConsulResponse<Session> create(SessionCreateRequest request, WriteOptions options) {
        log.debug("Creating session: {}", request.name());
        
        try {
            String path = "/v1/session/create";
            ConsulResponse<JsonNode> response = httpTransport.put(path, request, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.has("ID")) {
                throw new ConsulException("Invalid session creation response");
            }
            
            String sessionId = node.get("ID").asText();
            
            // Create session object with the ID from response
            Session session = Session.builder()
                .id(sessionId)
                .name(request.name())
                .node(request.node())
                .checks(request.checks())
                .lockDelay(request.lockDelay())
                .behavior(request.behavior())
                .ttl(request.ttl())
                .createIndex(response.getConsulIndex())
                .modifyIndex(response.getConsulIndex())
                .build();
            
            return ConsulResponse.of(session, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to create session", e);
            throw new ConsulException("Failed to create session", e);
        }
    }
    
    @Override
    public ConsulResponse<Void> destroy(String sessionId, WriteOptions options) {
        log.debug("Destroying session: {}", sessionId);
        
        try {
            String path = "/v1/session/destroy/" + sessionId;
            ConsulResponse<Void> response = httpTransport.put(path, null, options, Void.class);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to destroy session: {}", sessionId, e);
            throw new ConsulException("Failed to destroy session: " + sessionId, e);
        }
    }
    
    @Override
    public ConsulResponse<List<Session>> list(QueryOptions options) {
        log.debug("Listing sessions");
        
        try {
            String path = "/v1/session/list";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }
            
            List<Session> sessions = objectMapper.convertValue(node, new TypeReference<List<Session>>() {});
            return ConsulResponse.of(sessions, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to list sessions", e);
            throw new ConsulException("Failed to list sessions", e);
        }
    }
    
    @Override
    public ConsulResponse<Optional<Session>> read(String sessionId, QueryOptions options) {
        log.debug("Reading session: {}", sessionId);
        
        try {
            String path = "/v1/session/info/" + sessionId;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || node.isEmpty()) {
                return ConsulResponse.of(Optional.empty(), response.getConsulIndex());
            }
            
            // Consul returns array even for single session
            if (node.isArray()) {
                if (node.size() == 0) {
                    return ConsulResponse.of(Optional.empty(), response.getConsulIndex());
                }
                Session session = objectMapper.treeToValue(node.get(0), Session.class);
                return ConsulResponse.of(Optional.of(session), response.getConsulIndex());
            } else {
                Session session = objectMapper.treeToValue(node, Session.class);
                return ConsulResponse.of(Optional.of(session), response.getConsulIndex());
            }
            
        } catch (Exception e) {
            log.error("Failed to read session: {}", sessionId, e);
            throw new ConsulException("Failed to read session: " + sessionId, e);
        }
    }
    
    @Override
    public ConsulResponse<Session> renew(String sessionId, WriteOptions options) {
        log.debug("Renewing session: {}", sessionId);
        
        try {
            String path = "/v1/session/renew/" + sessionId;
            ConsulResponse<JsonNode> response = httpTransport.put(path, null, options, JsonNode.class);
            
            JsonNode node = response.getBody();
            if (node == null || node.isEmpty()) {
                throw new ConsulException("Session not found or expired: " + sessionId);
            }
            
            // Consul returns array even for single session
            Session session;
            if (node.isArray()) {
                if (node.size() == 0) {
                    throw new ConsulException("Session not found or expired: " + sessionId);
                }
                session = objectMapper.treeToValue(node.get(0), Session.class);
            } else {
                session = objectMapper.treeToValue(node, Session.class);
            }
            
            return ConsulResponse.of(session, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to renew session: {}", sessionId, e);
            throw new ConsulException("Failed to renew session: " + sessionId, e);
        }
    }
}
