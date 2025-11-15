package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.pingconfig.metrics.PingMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe cache for Kafka configuration fetched from config-control-service.
 * <p>
 * This cache implements lazy initialization with periodic refresh. It fetches
 * Kafka configuration from config-control-service on first access and refreshes
 * it periodically (configurable interval, default 5 minutes). Falls back to
 * property/environment-based configuration if fetch fails.
 */
@Slf4j
public class KafkaConfigCache {

    private final RestClient restClient;
    private final SdkProperties sdkProperties;
    private final Environment environment;
    private final ReentrantLock lock = new ReentrantLock();

    // Optional - only used if MeterRegistry is available
    // This will be injected by Spring after construction via setter injection or @PostConstruct
    private PingMetrics pingMetrics;

    private volatile KafkaConfig cachedConfig;
    private volatile Instant lastFetchTime;
    private volatile boolean initialized = false;

    /**
     * Creates a new KafkaConfigCache.
     *
     * @param restClient    RestClient for HTTP requests
     * @param sdkProperties SDK configuration properties
     * @param environment   Spring environment for env var overrides
     */
    public KafkaConfigCache(RestClient restClient, SdkProperties sdkProperties, Environment environment) {
        this.restClient = restClient;
        this.sdkProperties = sdkProperties;
        this.environment = environment;
    }

    /**
     * Sets the PingMetrics component (called by Spring via setter injection).
     *
     * @param pingMetrics Ping metrics component (can be null)
     */
    @Autowired(required = false)
    public void setPingMetrics(PingMetrics pingMetrics) {
        this.pingMetrics = pingMetrics;
    }

    /**
     * Gets the KafkaConfigFetcher, creating it with PingMetrics if available.
     *
     * @return KafkaConfigFetcher instance
     */
    private KafkaConfigFetcher getFetcher() {
        return new KafkaConfigFetcher(restClient, sdkProperties, environment, pingMetrics);
    }

    /**
     * Gets the cached Kafka configuration, fetching if necessary.
     * <p>
     * This method implements lazy initialization: on first call, it fetches
     * configuration from config-control-service. Subsequent calls return the
     * cached value until the refresh interval expires.
     *
     * @return KafkaConfig with bootstrap servers and topic, never null
     * @throws IllegalStateException if no configuration is available (should not happen with fallback)
     */
    public KafkaConfig get() {
        // Check if cache is stale or not initialized
        long refreshInterval = sdkProperties.getPing().getKafka().getConfigRefreshInterval();
        boolean needsRefresh = !initialized
                || cachedConfig == null
                || (lastFetchTime != null
                        && Instant.now().isAfter(lastFetchTime.plusMillis(refreshInterval)));

        if (!needsRefresh && cachedConfig != null) {
            log.debug("Using cached Kafka config: bootstrapServers={}, topic={}",
                    cachedConfig.bootstrapServers(), cachedConfig.topic());
            return cachedConfig;
        }

        // Acquire lock to prevent concurrent fetches
        lock.lock();
        try {
            // Double-check after acquiring lock
            if (!initialized || cachedConfig == null
                    || (lastFetchTime != null && Instant.now().isAfter(lastFetchTime.plusMillis(refreshInterval)))) {
                log.debug("Fetching Kafka config from config-control-service (cache miss or stale)");
                KafkaConfig fetched = getFetcher().fetch();

                if (fetched != null && fetched.isValid()) {
                    cachedConfig = fetched;
                    lastFetchTime = Instant.now();
                    initialized = true;
                    log.info("Kafka config cached successfully: bootstrapServers={}, topic={}",
                            cachedConfig.bootstrapServers(), cachedConfig.topic());
                } else {
                    // If fetch failed but we have a cached config, keep using it
                    if (cachedConfig != null) {
                        log.warn("Failed to fetch Kafka config, using stale cached config: bootstrapServers={}, topic={}",
                                cachedConfig.bootstrapServers(), cachedConfig.topic());
                        return cachedConfig;
                    }
                    // If no cached config and fetch failed, try fallback one more time
                    throw new IllegalStateException(
                            "No Kafka configuration available. Ensure config-control-service is accessible or configure zcm.sdk.ping.kafka.bootstrap-servers");
                }
            }
            return cachedConfig;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Periodically refreshes the Kafka configuration cache.
     * <p>
     * This scheduled method runs every 5 minutes (or as configured) to refresh
     * the cached configuration from config-control-service in the background.
     */
    @Scheduled(fixedDelayString = "${zcm.sdk.ping.kafka.config-refresh-interval:300000}")
    public void refresh() {
        if (!initialized) {
            // Skip refresh if not initialized yet (will be fetched on first get())
            return;
        }

        log.debug("Periodic refresh of Kafka config cache");
        lock.lock();
        try {
            KafkaConfig fetched = getFetcher().fetch();
            if (fetched != null && fetched.isValid()) {
                cachedConfig = fetched;
                lastFetchTime = Instant.now();
                log.debug("Kafka config cache refreshed successfully: bootstrapServers={}, topic={}",
                        cachedConfig.bootstrapServers(), cachedConfig.topic());
            } else {
                log.warn("Failed to refresh Kafka config cache, keeping existing cached config");
            }
        } catch (Exception e) {
            log.warn("Error during periodic Kafka config refresh: {}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Invalidates the cache, forcing a fresh fetch on next get().
     */
    public void invalidate() {
        lock.lock();
        try {
            log.debug("Invalidating Kafka config cache");
            cachedConfig = null;
            lastFetchTime = null;
            initialized = false;
        } finally {
            lock.unlock();
        }
    }
}

