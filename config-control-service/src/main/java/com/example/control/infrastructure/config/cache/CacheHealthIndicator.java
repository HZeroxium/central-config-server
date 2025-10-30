package com.example.control.infrastructure.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link HealthIndicator} that reports the runtime health of the
 * application's cache layer.
 *
 * <p>
 * <strong>What it checks</strong>
 * </p>
 * <ul>
 * <li><b>Configured provider</b> from {@link CacheProperties#getProvider()}
 * (desired state).</li>
 * <li><b>Actual provider</b> resolved from the current
 * {@link DelegatingCacheManager} delegate type (observed state).</li>
 * <li><b>Availability map</b> for all known providers using
 * {@link CacheManagerFactory#isProviderAvailable(CacheProperties.CacheProvider)}.</li>
 * <li><b>Fallback flags</b> that indicate whether automatic fallback is enabled
 * at runtime.</li>
 * </ul>
 *
 * <p>
 * <strong>Health semantics</strong>
 * </p>
 * <ul>
 * <li>Returns {@code Health.up()} if the configured provider is considered
 * healthy by
 * {@link #checkProviderHealth(CacheProperties.CacheProvider)}.</li>
 * <li>Returns {@code Health.down()} otherwise, and includes diagnostic details
 * (configured/actual providers,
 * availability of each provider, and fallback configuration).</li>
 * <li>Details are attached via {@link Health.Builder#withDetails(Map)} and are
 * visible when
 * health details are exposed in Actuator configuration.
 * :contentReference[oaicite:1]{index=1}</li>
 * </ul>
 *
 * <p>
 * <strong>Notes</strong>
 * </p>
 * <ul>
 * <li>This indicator does not attempt to migrate or warm caches. It only
 * reports status.</li>
 * <li>Lifecycle management (closing old managers when switching) is the
 * caller's responsibility.</li>
 * </ul>
 *
 * @see HealthIndicator
 * @see Health
 * @see DelegatingCacheManager
 * @see CacheManagerFactory
 * @since 1.0.0
 */
@Slf4j
@Component
public class CacheHealthIndicator implements HealthIndicator {

    private final CacheManagerFactory cacheManagerFactory;
    private final CacheProperties cacheProperties;
    private final DelegatingCacheManager delegatingCacheManager;
    private final CacheMetrics cacheMetrics; // Optional - may be null if metrics not enabled

    public CacheHealthIndicator(CacheManagerFactory cacheManagerFactory,
            CacheProperties cacheProperties,
            DelegatingCacheManager delegatingCacheManager) {
        this(cacheManagerFactory, cacheProperties, delegatingCacheManager, null);
    }

    public CacheHealthIndicator(CacheManagerFactory cacheManagerFactory,
            CacheProperties cacheProperties,
            DelegatingCacheManager delegatingCacheManager,
            CacheMetrics cacheMetrics) {
        this.cacheManagerFactory = cacheManagerFactory;
        this.cacheProperties = cacheProperties;
        this.delegatingCacheManager = delegatingCacheManager;
        this.cacheMetrics = cacheMetrics;
    }

    /**
     * Compute and return the overall cache health.
     * <p>
     * The method evaluates the configured provider and underlying runtime
     * capabilities to
     * determine {@code UP} or {@code DOWN}. All diagnostics are provided as details
     * and can be
     * inspected via the Actuator health endpoints.
     * :contentReference[oaicite:2]{index=2}
     *
     * @return a {@link Health} instance with {@code UP}/{@code DOWN} status and
     *         diagnostic details
     */
    @Override
    public Health health() {
        Map<String, Object> details = getHealthDetails();
        boolean healthy = Boolean.TRUE.equals(details.get("healthy"));

        return (healthy ? Health.up() : Health.down())
                .withDetails(details)
                .build();
    }

    /**
     * Build a diagnostic map describing the current cache health context.
     * <p>
     * The map includes: configured provider, detected provider (delegate type),
     * fallback flags,
     * per-provider availability, an overall {@code healthy} flag, and a
     * human-readable {@code status} message.
     *
     * @return immutable-like map of diagnostic details for inclusion in
     *         {@link Health}
     */
    public Map<String, Object> getHealthDetails() {
        try {
            CacheProperties.CacheProvider currentProvider = cacheProperties.getProvider();
            boolean isHealthy = checkProviderHealth(currentProvider);

            Map<String, Object> details = new HashMap<>();
            details.put("provider", currentProvider.name());
            details.put("actualProvider", getCurrentProviderFromManager());
            details.put("fallbackEnabled", cacheProperties.isEnableFallback());
            details.put("availableProviders", getAvailableProviders());
            details.put("healthy", isHealthy);
            details.put("cacheManagerType", delegatingCacheManager.getCurrentDelegateType());

            // Add detailed metrics if available
            if (cacheMetrics != null) {
                details.put("metrics", getCacheMetricsDetails());
            }

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
            error.put("actualProvider", safeGetCurrentProviderFromManager());
            return error;
        }
    }

    /**
     * Detect the active provider by inspecting the simple class name of the current
     * {@link CacheManager}
     * delegate exposed by {@link DelegatingCacheManager}.
     *
     * @return a best-effort mapping to a {@link CacheProperties.CacheProvider}
     *         name; otherwise the raw type name
     */
    private String getCurrentProviderFromManager() {
        String managerType = delegatingCacheManager.getCurrentDelegateType();

        // Map manager types to provider names
        switch (managerType) {
            case "CaffeineCacheManager":
                return "CAFFEINE";
            case "RedisCacheManager":
                return "REDIS";
            case "TwoLevelCacheManager":
                return "TWO_LEVEL";
            case "NoOpCacheManager":
                return "NOOP";
            default:
                return managerType; // Return the type if we don't recognize it
        }
    }

    /**
     * Safe variant of {@link #getCurrentProviderFromManager()} used for error-path
     * details.
     *
     * @return detected provider or {@code "UNKNOWN"} if unavailable
     */
    private String safeGetCurrentProviderFromManager() {
        try {
            return getCurrentProviderFromManager();
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    /**
     * Determine if the given cache provider should be considered healthy in the
     * current environment.
     * <p>
     * <ul>
     * <li>{@code CAFFEINE}/{@code NOOP}: always available in-process.</li>
     * <li>{@code REDIS}: healthy if the factory reports availability of a
     * Redis-backed provider
     * (i.e., a {@code RedisConnectionFactory} is present/usable).
     * :contentReference[oaicite:3]{index=3}</li>
     * <li>{@code TWO_LEVEL}: considered healthy if at least L1 (Caffeine) is
     * available. This indicator
     * does not fail the overall health when L2 is temporarily unavailable; rely on
     * details to inspect degradation.</li>
     * </ul>
     *
     * @param provider configured provider
     * @return {@code true} if healthy; {@code false} otherwise
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
                    return true;

                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("Error checking provider health for: {}", provider, e);
            return false;
        }
    }

    /**
     * Build a map of all providers to a boolean indicating whether each is
     * available in this runtime.
     * The decision is delegated to
     * {@link CacheManagerFactory#isProviderAvailable(CacheProperties.CacheProvider)}.
     *
     * @return a map of provider name → availability flag
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

    /**
     * Get detailed cache metrics for health reporting.
     */
    private Map<String, Object> getCacheMetricsDetails() {
        Map<String, Object> metrics = new HashMap<>();

        if (cacheMetrics == null) {
            return metrics;
        }

        try {
            metrics.put("overallHitRatio", cacheMetrics.getOverallHitRatio());

            Map<String, Object> perCacheMetrics = new HashMap<>();
            delegatingCacheManager.getCacheNames().forEach(cacheName -> {
                Map<String, Object> cacheMetricDetails = new HashMap<>();
                cacheMetricDetails.put("hitRatio", this.cacheMetrics.getHitRatio(cacheName));
                cacheMetricDetails.put("missRate", 1.0 - this.cacheMetrics.getHitRatio(cacheName));
                perCacheMetrics.put(cacheName, cacheMetricDetails);
            });

            metrics.put("perCache", perCacheMetrics);
        } catch (Exception e) {
            log.warn("Error gathering cache metrics", e);
            metrics.put("error", "Failed to gather metrics: " + e.getMessage());
        }

        return metrics;
    }
}
