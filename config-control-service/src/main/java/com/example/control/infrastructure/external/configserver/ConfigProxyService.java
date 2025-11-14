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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigProxyService {

    private final DiscoveryClient discoveryClient;
    private final ConfigServerProperties configServerProperties;
    private final ConfigProxyProperties configProxyProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final ConfigSnapshotBuilder snapshotBuilder = new ConfigSnapshotBuilder();

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
     *
     * @param serviceName service name
     * @param profile     environment profile
     * @return SHA-256 hash of effective configuration
     */
    private String fetchRealConfigHash(String serviceName, String profile) {
        try {
            log.debug("Fetching effective config from Config Server for {}:{}", serviceName, profile);

            // Call Config Server: GET /serviceName/profile
            String configUrl = normalizeUrl(configServerProperties.getUrl()) + "/" + serviceName + "/" +
                    (profile != null && !profile.trim().isEmpty() ? profile : "default");

            String configJson = restClient.get()
                    .uri(configUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

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
     *
     * @param destination optional destination pattern (service:instance or
     *                    service:** for all)
     * @return response from Config Server
     */
    public String triggerBusRefresh(String destination) {
        try {
            log.info("Triggering bus refresh via Config Server for destination: {}", destination);

            String base = normalizeUrl(configServerProperties.getUrl()) + "/actuator/busrefresh";
            // String busRefreshUrl = (destination != null && !destination.trim().isEmpty())
            // ? base + "/" + destination
            // : base;

            String busRefreshUrl = base;

            String response;
            response = restClient.post()
                    .uri(busRefreshUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.info("Bus refresh triggered successfully for destination: {}", destination);
            return response;

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
