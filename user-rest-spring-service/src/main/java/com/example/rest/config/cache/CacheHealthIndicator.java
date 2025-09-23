package com.example.rest.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for cache providers.
 * Monitors the health of the current cache provider and reports availability.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheHealthIndicator {

    private final CacheManagerFactory cacheManagerFactory;
    private final CacheProperties cacheProperties;

    public Map<String, Object> getHealthStatus() {
        try {
            CacheProperties.CacheProvider currentProvider = cacheProperties.getProvider();
            boolean isHealthy = checkProviderHealth(currentProvider);

            Map<String, Object> details = new HashMap<>();
            details.put("provider", currentProvider.name());
            details.put("fallbackEnabled", cacheProperties.isEnableFallback());
            details.put("availableProviders", getAvailableProviders());
            details.put("healthy", isHealthy);

            if (isHealthy) {
                details.put("status", "Cache provider is healthy");
            } else {
                details.put("status", "Cache provider is down");
                if (cacheProperties.isEnableFallback()) {
                    details.put("fallbackStatus", "Fallback mechanisms available");
                }
            }

            return details;
        } catch (Exception e) {
            log.error("Error checking cache health", e);
            Map<String, Object> error = new HashMap<>();
            error.put("healthy", false);
            error.put("error", e.getMessage());
            error.put("provider", cacheProperties.getProvider().name());
            return error;
        }
    }

    /**
     * Checks if the specified cache provider is healthy.
     */
    private boolean checkProviderHealth(CacheProperties.CacheProvider provider) {
        try {
            switch (provider) {
                case CAFFEINE:
                case NOOP:
                    return true; // These are always available

                case REDIS:
                    return cacheManagerFactory.isProviderAvailable(CacheProperties.CacheProvider.REDIS);

                case TWO_LEVEL:
                    // Two-level is healthy if at least L1 (Caffeine) is available
                    return true; // L1 is always available

                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("Error checking provider health for: {}", provider, e);
            return false;
        }
    }

    /**
     * Returns a map of available cache providers.
     */
    private Map<String, Boolean> getAvailableProviders() {
        Map<String, Boolean> providers = new HashMap<>();

        for (CacheProperties.CacheProvider provider : CacheProperties.CacheProvider.values()) {
            try {
                providers.put(provider.name(), cacheManagerFactory.isProviderAvailable(provider));
            } catch (Exception e) {
                log.debug("Error checking availability for provider: {}", provider, e);
                providers.put(provider.name(), false);
            }
        }

        return providers;
    }
}