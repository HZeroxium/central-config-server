package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.HttpTransport;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.exception.ConsulException;
import com.example.control.infrastructure.consulclient.model.HealthCheck;
import com.example.control.infrastructure.consulclient.model.HealthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Implementation of HealthClient using HttpTransport.
 */
@Slf4j
@RequiredArgsConstructor
public class HealthClientImpl implements HealthClient {

    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;

    @Override
    public ConsulResponse<List<HealthCheck>> node(String nodeName, QueryOptions options) {
        log.debug("Getting health checks for node: {}", nodeName);

        try {
            String path = "/v1/health/node/" + nodeName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<HealthCheck> healthChecks = objectMapper.convertValue(node,
                    new TypeReference<List<HealthCheck>>() {
                    });

            return ConsulResponse.of(healthChecks, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to get health checks for node: {}", nodeName, e);
            throw new ConsulException("Failed to get health checks for node: " + nodeName, e);
        }
    }

    @Override
    public ConsulResponse<List<HealthCheck>> service(String serviceName, QueryOptions options) {
        log.debug("Getting health checks for service: {}", serviceName);

        try {
            String path = "/v1/health/service/" + serviceName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<HealthCheck> healthChecks = objectMapper.convertValue(node,
                    new TypeReference<List<HealthCheck>>() {
                    });

            return ConsulResponse.of(healthChecks, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to get health checks for service: {}", serviceName, e);
            throw new ConsulException("Failed to get health checks for service: " + serviceName, e);
        }
    }

    @Override
    public ConsulResponse<List<HealthService>> serviceHealthy(String serviceName, QueryOptions options) {
        log.debug("Getting healthy instances for service: {}", serviceName);

        try {
            String path = "/v1/health/service/" + serviceName + "?passing";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<HealthService> healthServices = objectMapper.convertValue(node,
                    new TypeReference<List<HealthService>>() {
                    });

            return ConsulResponse.of(healthServices, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to get healthy instances for service: {}", serviceName, e);
            throw new ConsulException("Failed to get healthy instances for service: " + serviceName, e);
        }
    }

    @Override
    public ConsulResponse<List<HealthService>> serviceAny(String serviceName, QueryOptions options) {
        log.debug("Getting any instances for service: {}", serviceName);

        try {
            String path = "/v1/health/service/" + serviceName + "?any";
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<HealthService> healthServices = objectMapper.convertValue(node,
                    new TypeReference<List<HealthService>>() {
                    });

            return ConsulResponse.of(healthServices, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to get any instances for service: {}", serviceName, e);
            throw new ConsulException("Failed to get any instances for service: " + serviceName, e);
        }
    }

    @Override
    public ConsulResponse<List<HealthService>> serviceState(String serviceName, String state, QueryOptions options) {
        log.debug("Getting instances with state {} for service: {}", state, serviceName);

        try {
            String path = "/v1/health/service/" + serviceName + "?" + state;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<HealthService> healthServices = objectMapper.convertValue(node,
                    new TypeReference<List<HealthService>>() {
                    });

            return ConsulResponse.of(healthServices, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to get instances with state {} for service: {}", state, serviceName, e);
            throw new ConsulException("Failed to get instances with state " + state + " for service: " + serviceName, e);
        }
    }

    @Override
    public ConsulResponse<List<HealthCheck>> state(String state, QueryOptions options) {
        log.debug("Getting health checks with state: {}", state);

        try {
            String path = "/v1/health/state/" + state;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<HealthCheck> healthChecks = objectMapper.convertValue(node,
                    new TypeReference<List<HealthCheck>>() {
                    });

            return ConsulResponse.of(healthChecks, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to get health checks with state: {}", state, e);
            throw new ConsulException("Failed to get health checks with state: " + state, e);
        }
    }

    @Override
    public ConsulResponse<List<HealthService>> connect(String serviceName, QueryOptions options) {
        log.debug("Getting connect-capable instances for service: {}", serviceName);

        try {
            String path = "/v1/health/connect/" + serviceName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<HealthService> healthServices = objectMapper.convertValue(node,
                    new TypeReference<List<HealthService>>() {
                    });

            return ConsulResponse.of(healthServices, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to get connect-capable instances for service: {}", serviceName, e);
            throw new ConsulException("Failed to get connect-capable instances for service: " + serviceName, e);
        }
    }

    @Override
    public ConsulResponse<List<HealthService>> ingress(String serviceName, QueryOptions options) {
        log.debug("Getting ingress gateway instances for service: {}", serviceName);

        try {
            String path = "/v1/health/ingress/" + serviceName;
            ConsulResponse<JsonNode> response = httpTransport.get(path, options, JsonNode.class);

            JsonNode node = response.getBody();
            if (node == null || !node.isArray()) {
                return ConsulResponse.of(List.of(), response.getConsulIndex());
            }

            List<HealthService> healthServices = objectMapper.convertValue(node,
                    new TypeReference<List<HealthService>>() {
                    });

            return ConsulResponse.of(healthServices, response.getConsulIndex());

        } catch (Exception e) {
            log.error("Failed to get ingress gateway instances for service: {}", serviceName, e);
            throw new ConsulException("Failed to get ingress gateway instances for service: " + serviceName, e);
        }
    }
}