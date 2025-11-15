package com.example.control.infrastructure.external.configserver;

import com.example.control.api.http.exception.exceptions.ExternalServiceException;
import com.example.control.infrastructure.external.fallback.ConfigServerFallback;
import com.example.control.infrastructure.config.misc.ConfigServerProperties;
import com.example.control.infrastructure.resilience.ResilienceDecoratorsFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Resilience-enabled client for Config Server with service discovery support.
 * <p>
 * All calls are protected with Circuit Breaker, Retry, Bulkhead, and
 * TimeLimiter.
 * Supports service discovery via Consul with fallback to direct URL.
 * Falls back to cached configuration when available.
 * </p>
 */
@Slf4j
@Component
public class ConfigServerClient {

    private static final String SERVICE_NAME = "configserver";

    private final ConfigServerProperties properties;
    private final ResilienceDecoratorsFactory resilienceFactory;
    private final RestClient loadBalancedRestClient;
    private final RestClient directRestClient;
    private final ConfigServerFallback configServerFallback;
    private final DiscoveryClient discoveryClient;

    public ConfigServerClient(
            ConfigServerProperties properties,
            ResilienceDecoratorsFactory resilienceFactory,
            @Qualifier("loadBalancedConfigServerRestClient") RestClient loadBalancedRestClient,
            @Qualifier("configServerRestClient") RestClient directRestClient,
            ConfigServerFallback configServerFallback,
            DiscoveryClient discoveryClient) {
        this.properties = properties;
        this.resilienceFactory = resilienceFactory;
        this.loadBalancedRestClient = loadBalancedRestClient;
        this.directRestClient = directRestClient;
        this.configServerFallback = configServerFallback;
        this.discoveryClient = discoveryClient;
    }

    /**
     * Get actuator index with resilience.
     */
    public String getActuatorIndex() {
        return executeWithResilience("/actuator", null, null, null);
    }

    /**
     * Get actuator endpoint path with resilience.
     */
    public String getActuatorPath(String path) {
        return executeWithResilience("/actuator/" + path, null, null, null);
    }

    /**
     * Get environment configuration with resilience and caching.
     */
    @Cacheable(value = "config-hashes", key = "#application + ':' + #profiles")
    public String getEnvironment(String application, String profiles) {
        String path = "/" + application + "/" + (profiles == null || profiles.isBlank() ? "default" : profiles);
        String prof = profiles == null || profiles.isBlank() ? "default" : profiles;
        return executeWithResilience(path, application, prof, null);
    }

    /**
     * Get environment with label, with resilience and caching.
     */
    @Cacheable(value = "config-hashes", key = "#application + ':' + #profiles + ':' + #label")
    public String getEnvironmentWithLabel(String application, String profiles, String label) {
        String prof = (profiles == null || profiles.isBlank()) ? "default" : profiles;
        String path = "/" + application + "/" + prof + "/" + label;
        String result = executeWithResilience(path, application, prof, label);
        return result;
    }

    /**
     * Execute GET request with full resilience patterns.
     * <p>
     * Uses service discovery if enabled and instances are available,
     * otherwise falls back to direct URL.
     * </p>
     *
     * @param path        Request path
     * @param application Application name (for fallback)
     * @param profiles    Profiles (for fallback)
     * @param label       Label (for fallback, may be null)
     * @return Response body
     */
    private String executeWithResilience(String path, String application, String profiles, String label) {
        Supplier<String> apiCall = () -> {
            try {
                // Try service discovery first if enabled
                if (properties.getServiceDiscovery().isEnabled()) {
                    try {
                        List<ServiceInstance> instances = discoveryClient.getInstances(
                                properties.getServiceDiscovery().getServiceName());
                        
                        if (instances != null && !instances.isEmpty()) {
                            // Use load-balanced RestClient with service name
                            String serviceUrl = "http://" + properties.getServiceDiscovery().getServiceName() + path;
                            log.debug("ConfigServer GET via service discovery: {} ({} instances available)", 
                                    serviceUrl, instances.size());

                            String result = loadBalancedRestClient.get()
                                    .uri(serviceUrl)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .retrieve()
                                    .body(String.class);

                            // Cache successful response
                            cacheResult(application, profiles, label, result);
                            return result;
                        } else {
                            log.debug("No instances found for service: {}. Falling back to direct URL.",
                                    properties.getServiceDiscovery().getServiceName());
                        }
                    } catch (Exception e) {
                        log.warn("Service discovery failed for config-server, falling back to direct URL: {}", 
                                e.getMessage());
                    }
                }

                // Fallback to direct URL
                if (properties.getServiceDiscovery().isFallbackToUrl()) {
                    String url = normalize(properties.getUrl()) + path;
                    log.debug("ConfigServer GET via direct URL: {}", url);

                    String result = directRestClient.get()
                            .uri(url)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(String.class);

                    // Cache successful response
                    cacheResult(application, profiles, label, result);
                    return result;
                } else {
                    throw new ExternalServiceException("config-server",
                            "Service discovery failed and fallback to URL is disabled", null);
                }
            } catch (Exception e) {
                log.error("ConfigServer call failed: {}", path, e);
                throw new ExternalServiceException("config-server", e.getMessage(), e);
            }
        };

        // Decorate with Circuit Breaker, Retry (with budget), Bulkhead, TimeLimiter
        Function<Throwable, String> fallbackFunction = (Throwable t) -> {
            log.warn("ConfigServer fallback triggered for path: {} due to: {}", path, t.getMessage());

            // Try to get cached fallback if we have application/profile context
            if (application != null && profiles != null) {
                try {
                    if (label != null) {
                        String cached = configServerFallback.getFallbackConfigWithLabel(application, profiles, label);
                        if (cached != null && !cached.equals("{}")) {
                            return cached;
                        }
                    } else {
                        String cached = configServerFallback.getFallbackConfig(application, profiles);
                        if (cached != null && !cached.equals("{}")) {
                            return cached;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get cached fallback for {}:{}, using default", application, profiles, e);
                }
            }

            // Return empty JSON if no cached fallback available
            return "{}";
        };
        Supplier<String> decoratedCall = resilienceFactory.decorateSupplier(
                SERVICE_NAME,
                apiCall,
                fallbackFunction);

        return decoratedCall.get();
    }

    /**
     * Cache successful response for future fallback.
     */
    private void cacheResult(String application, String profiles, String label, String result) {
        if (result != null && application != null && profiles != null) {
            if (label != null) {
                // Cache with label - we'd need a method in ConfigServerFallback for this
                // For now, just cache the regular way
                configServerFallback.saveConfigToCache(application, profiles, result);
            } else {
                configServerFallback.saveConfigToCache(application, profiles, result);
            }
        }
    }

    private String normalize(String base) {
        if (base.endsWith("/"))
            return base.substring(0, base.length() - 1);
        return base;
    }
}
