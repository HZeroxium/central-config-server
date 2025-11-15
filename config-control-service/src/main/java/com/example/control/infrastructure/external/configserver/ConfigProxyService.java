package com.example.control.infrastructure.external.configserver;

import com.example.control.api.http.exception.exceptions.ExternalServiceException;
import com.example.control.api.http.exception.exceptions.ServiceNotFoundException;
import com.example.control.infrastructure.config.misc.ConfigProxyProperties;
import com.example.control.infrastructure.config.misc.ConfigServerProperties;
import com.example.control.domain.valueobject.configsnapshot.ConfigSnapshot;
import com.example.control.domain.valueobject.configsnapshot.ConfigSnapshotBuilder;
import com.example.control.domain.valueobject.configsnapshot.ConfigHashCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Service that proxies requests to Config Server and provides config-related
 * operations.
 * <p>
 * Supports mock mode to avoid GitHub rate limits during testing and load testing.
 * When mock mode is enabled, services not in the whitelist will receive mock
 * config hashes instead of fetching from Config Server.
 * <p>
 * Uses service discovery via Consul with fallback to direct URL.
 */
@Slf4j
@Service
public class ConfigProxyService {

    private final DiscoveryClient discoveryClient;
    private final ConfigServerProperties configServerProperties;
    private final ConfigProxyProperties configProxyProperties;
    private final ObjectMapper objectMapper;
    private final RestClient loadBalancedRestClient;
    private final RestClient directRestClient;
    private final ConfigSnapshotBuilder snapshotBuilder;

    public ConfigProxyService(
            DiscoveryClient discoveryClient,
            ConfigServerProperties configServerProperties,
            ConfigProxyProperties configProxyProperties,
            ObjectMapper objectMapper,
            @Qualifier("loadBalancedConfigServerRestClient") RestClient loadBalancedRestClient,
            @Qualifier("configServerRestClient") RestClient directRestClient) {
        this.discoveryClient = discoveryClient;
        this.configServerProperties = configServerProperties;
        this.configProxyProperties = configProxyProperties;
        this.objectMapper = objectMapper;
        this.loadBalancedRestClient = loadBalancedRestClient;
        this.directRestClient = directRestClient;
        this.snapshotBuilder = new ConfigSnapshotBuilder();
    }

    /**
     * Prefix for mock hash generation to distinguish from real hashes.
     */
    private static final String MOCK_PREFIX = "mock-";

    /**
     * Get effective configuration hash for a service in an environment.
     * <p>
     * If mock mode is enabled and service is not whitelisted, returns a mock hash.
     * Otherwise, fetches config from Config Server and computes SHA-256 hash.
     *
     * @param serviceName service name
     * @param profile     environment profile (e.g., dev, prod)
     * @return SHA-256 hash of effective configuration (or mock hash)
     */
    @Cacheable(value = "config-hashes", key = "#serviceName + ':' + #profile")
    @NewSpan("config.get_effective_hash")
    public String getEffectiveConfigHash(
            @SpanTag("service.name") String serviceName,
            @SpanTag("profile") String profile) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }

        // Check if mock mode is enabled and service is not whitelisted
        if (configProxyProperties.isMockModeEnabled() &&
                !configProxyProperties.isWhitelisted(serviceName)) {
            return getMockConfigHash(serviceName, profile);
        }

        // Fetch real config from Config Server
        return fetchRealConfigHash(serviceName, profile);
    }

    /**
     * Generates a mock config hash based on configured strategy.
     *
     * @param serviceName service name
     * @param profile     environment profile
     * @return mock config hash
     */
    private String getMockConfigHash(String serviceName, String profile) {
        String mockHash;

        switch (configProxyProperties.getMockStrategy()) {
            case DETERMINISTIC:
                // Generate stable hash from serviceName + profile
                String input = serviceName + ":" + (profile != null ? profile : "default");
                mockHash = ConfigHashCalculator.hash(MOCK_PREFIX + input);
                break;

            case RANDOM:
                // Generate random hash each time (includes timestamp)
                String randomInput = serviceName + ":" +
                        (profile != null ? profile : "default") + ":" + System.currentTimeMillis();
                mockHash = ConfigHashCalculator.hash(MOCK_PREFIX + randomInput);
                break;

            case STATIC:
                // Return fixed hash
                mockHash = configProxyProperties.getStaticMockHash();
                break;

            default:
                // Fallback to deterministic
                String fallbackInput = serviceName + ":" + (profile != null ? profile : "default");
                mockHash = ConfigHashCalculator.hash(MOCK_PREFIX + fallbackInput);
        }

        if (configProxyProperties.isLogMockUsage()) {
            log.debug("Returning mock config hash for {}:{} -> {} (strategy: {})",
                    serviceName, profile, mockHash, configProxyProperties.getMockStrategy());
        }

        return mockHash;
    }

    /**
     * Fetches real config hash from Config Server.
     * <p>
     * Uses service discovery if enabled and instances are available,
     * otherwise falls back to direct URL.
     * </p>
     *
     * @param serviceName service name
     * @param profile     environment profile
     * @return SHA-256 hash of effective configuration
     */
    private String fetchRealConfigHash(String serviceName, String profile) {
        try {
            log.debug("Fetching effective config from Config Server for {}:{}", serviceName, profile);

            String path = "/" + serviceName + "/" +
                    (profile != null && !profile.trim().isEmpty() ? profile : "default");
            String configJson = callConfigServer(path);

            if (configJson == null || configJson.trim().isEmpty()) {
                log.warn("Empty config response from Config Server for {}:{}", serviceName, profile);
                return null;
            }

            JsonNode configNode = objectMapper.readTree(configJson);
            ConfigSnapshot snapshot = snapshotBuilder.build(serviceName, profile, null, configNode);
            String hash = ConfigHashCalculator.hash(snapshot.toCanonicalString());
            log.debug("Computed config hash for {}:{} keys={} hash={}", serviceName, profile,
                    snapshot.getProperties().size(), hash);
            return hash;

        } catch (Exception e) {
            log.error("Failed to compute effective config hash for {}:{}", serviceName, profile, e);
            throw new ExternalServiceException("config-server",
                    "Failed to compute effective config hash: " + e.getMessage(), e);
        }
    }

    /**
     * Call Config Server with service discovery fallback to direct URL.
     *
     * @param path request path
     * @return response body
     */
    private String callConfigServer(String path) {
        // Try service discovery first if enabled
        if (configServerProperties.getServiceDiscovery().isEnabled()) {
            try {
                List<ServiceInstance> instances = discoveryClient.getInstances(
                        configServerProperties.getServiceDiscovery().getServiceName());

                if (instances != null && !instances.isEmpty()) {
                    // Use load-balanced RestClient with service name
                    String serviceUrl = "http://" + configServerProperties.getServiceDiscovery().getServiceName() + path;
                    log.debug("ConfigServer call via service discovery: {} ({} instances available)",
                            serviceUrl, instances.size());

                    return loadBalancedRestClient.get()
                            .uri(serviceUrl)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(String.class);
                } else {
                    log.debug("No instances found for service: {}. Falling back to direct URL.",
                            configServerProperties.getServiceDiscovery().getServiceName());
                }
            } catch (Exception e) {
                log.warn("Service discovery failed for config-server, falling back to direct URL: {}",
                        e.getMessage());
            }
        }

        // Fallback to direct URL
        if (configServerProperties.getServiceDiscovery().isFallbackToUrl()) {
            String url = normalizeUrl(configServerProperties.getUrl()) + path;
            log.debug("ConfigServer call via direct URL: {}", url);

            return directRestClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } else {
            throw new ExternalServiceException("config-server",
                    "Service discovery failed and fallback to URL is disabled", null);
        }
    }

    /**
     * Get configuration difference between expected (SoT) and applied.
     *
     * @param serviceName service name
     * @param profile     environment profile
     * @param appliedHash currently applied config hash
     * @return map with drift status and hashes
     */
    public Map<String, Object> getConfigDiff(String serviceName, String profile, String appliedHash) {
        String expectedHash = getEffectiveConfigHash(serviceName, profile);

        boolean hasDrift = expectedHash != null &&
                appliedHash != null &&
                !expectedHash.equals(appliedHash);

        return Map.of(
                "serviceName", serviceName,
                "profile", profile != null ? profile : "default",
                "expectedHash", expectedHash != null ? expectedHash : "unknown",
                "appliedHash", appliedHash != null ? appliedHash : "unknown",
                "hasDrift", hasDrift);
    }

    /**
     * Get list of healthy instances for a service from Consul.
     *
     * @param serviceName service name
     * @return list of service instances
     */
    @Cacheable(value = "consul-services", key = "#serviceName")
    public List<ServiceInstance> getServiceInstances(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }

        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances == null || instances.isEmpty()) {
                throw new ServiceNotFoundException(serviceName);
            }
            return instances;
        } catch (ServiceNotFoundException e) {
            throw e; // Re-throw service not found exceptions
        } catch (Exception e) {
            log.error("Failed to get instances for service: {}", serviceName, e);
            throw new ExternalServiceException("consul",
                    "Failed to get service instances: " + e.getMessage(), e);
        }
    }

    /**
     * Get list of all registered services from Consul.
     *
     * @return list of service names
     */
    @Cacheable(value = "consul-services", key = "'all'")
    public List<String> getAllServices() {
        try {
            return discoveryClient.getServices();
        } catch (Exception e) {
            log.error("Failed to get service list", e);
            throw new ExternalServiceException("consul",
                    "Failed to get service list: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger config refresh via Config Server's /busrefresh endpoint.
     * This uses Spring Cloud Bus to broadcast refresh events.
     * <p>
     * Uses service discovery if enabled, otherwise falls back to direct URL.
     * </p>
     *
     * @param destination optional destination pattern (service:instance or
     *                    service:** for all)
     * @return response from Config Server
     */
    public String triggerBusRefresh(String destination) {
        try {
            log.info("Triggering bus refresh via Config Server for destination: {}", destination);

            String path = "/actuator/busrefresh";
            String response;

            // Try service discovery first if enabled
            if (configServerProperties.getServiceDiscovery().isEnabled()) {
                try {
                    List<ServiceInstance> instances = discoveryClient.getInstances(
                            configServerProperties.getServiceDiscovery().getServiceName());

                    if (instances != null && !instances.isEmpty()) {
                        String serviceUrl = "http://" + configServerProperties.getServiceDiscovery().getServiceName() + path;
                        log.debug("Bus refresh via service discovery: {} ({} instances available)",
                                serviceUrl, instances.size());

                        response = loadBalancedRestClient.post()
                                .uri(serviceUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .body(String.class);

                        log.info("Bus refresh triggered successfully via service discovery for destination: {}", destination);
                        return response;
                    }
                } catch (Exception e) {
                    log.warn("Service discovery failed for bus refresh, falling back to direct URL: {}",
                            e.getMessage());
                }
            }

            // Fallback to direct URL
            if (configServerProperties.getServiceDiscovery().isFallbackToUrl()) {
                String url = normalizeUrl(configServerProperties.getUrl()) + path;
                log.debug("Bus refresh via direct URL: {}", url);

                response = directRestClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

                log.info("Bus refresh triggered successfully via direct URL for destination: {}", destination);
                return response;
            } else {
                throw new ExternalServiceException("config-server",
                        "Service discovery failed and fallback to URL is disabled", null);
            }

        } catch (Exception e) {
            log.error("Failed to trigger bus refresh for destination: {}", destination, e);
            throw new ExternalServiceException("config-server",
                    "Failed to trigger bus refresh: " + e.getMessage(), e);
        }
    }

    private String normalizeUrl(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
