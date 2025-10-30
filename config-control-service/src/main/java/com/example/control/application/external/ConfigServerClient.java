package com.example.control.application.external;

import com.example.control.api.exception.exceptions.ExternalServiceException;
import com.example.control.application.external.fallback.ConfigServerFallback;
import com.example.control.infrastructure.config.ConfigServerProperties;
import com.example.control.infrastructure.resilience.ResilienceDecoratorsFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Resilience-enabled client for Config Server.
 * <p>
 * All calls are protected with Circuit Breaker, Retry, Bulkhead, and
 * TimeLimiter.
 * Falls back to cached configuration when available.
 * </p>
 */
@Slf4j
@Component
public class ConfigServerClient {

    private static final String SERVICE_NAME = "configserver";

    private final ConfigServerProperties properties;
    private final ResilienceDecoratorsFactory resilienceFactory;
    private final RestClient restClient;
    private final ConfigServerFallback configServerFallback;

    public ConfigServerClient(
            ConfigServerProperties properties,
            ResilienceDecoratorsFactory resilienceFactory,
            @Qualifier("configServerRestClient") RestClient restClient,
            ConfigServerFallback configServerFallback) {
        this.properties = properties;
        this.resilienceFactory = resilienceFactory;
        this.restClient = restClient;
        this.configServerFallback = configServerFallback;
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
                String url = normalize(properties.getUrl()) + path;
                log.debug("ConfigServer GET: {}", url);

                String result = restClient.get()
                        .uri(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

                // Cache successful response for future fallback
                if (result != null && application != null && profiles != null) {
                    if (label != null) {
                        // Cache with label - we'd need a method in ConfigServerFallback for this
                        // For now, just cache the regular way
                        configServerFallback.saveConfigToCache(application, profiles, result);
                    } else {
                        configServerFallback.saveConfigToCache(application, profiles, result);
                    }
                }

                return result;
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

    private String normalize(String base) {
        if (base.endsWith("/"))
            return base.substring(0, base.length() - 1);
        return base;
    }
}
