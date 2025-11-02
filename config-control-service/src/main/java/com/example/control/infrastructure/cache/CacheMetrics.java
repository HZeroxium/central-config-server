package com.example.control.infrastructure.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom cache metrics collector for L1/L2 hit tracking and error tracking.
 * <p>
 * This class provides custom metrics that are NOT covered by Spring Boot's
 * auto-instrumentation:
 * <ul>
 * <li><b>Custom:</b> L1/L2 hit ratios for two-level cache (business
 * requirement)</li>
 * <li><b>Custom:</b> Error rate tracking per cache</li>
 * </ul>
 * <p>
 * <b>Standard metrics (auto-instrumented by Spring Boot):</b>
 * <ul>
 * <li>{@code cache.gets} - Total cache get operations</li>
 * <li>{@code cache.puts} - Total cache put operations</li>
 * <li>{@code cache.evictions} - Total cache evictions</li>
 * <li>{@code cache.size} - Current cache size</li>
 * <li>{@code cache.hit.ratio} - Overall hit ratio (L1+L2 combined)</li>
 * </ul>
 * <p>
 * <b>Why custom L1/L2 metrics?</b> Business requirement to track multi-level
 * cache
 * performance separately for optimization decisions.
 * <p>
 * Methods like {@code getHitRatio()}, {@code getOverallHitRatio()}, and
 * {@code getLatencyPercentile()} are provided for backward compatibility with
 * existing code. They query from Spring Boot's auto-instrumented metrics when
 * available.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheMetrics {

  private static final String METRIC_PREFIX = "cache.custom";
  private static final String CACHE_TAG = "cache";

  private final MeterRegistry meterRegistry;
  private final CacheManager cacheManager;

  // Per-cache L1/L2 hit tracking for two-level cache
  private final Map<String, AtomicLong> l1Hits = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> l2Hits = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> totalHits = new ConcurrentHashMap<>();

  // Per-cache error tracking
  private final Map<String, AtomicLong> cacheErrors = new ConcurrentHashMap<>();

  // Cached error counters per cache and error type
  private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();

  // Initialization flag to prevent duplicate initialization
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  /**
   * Initialize custom metrics for all caches.
   * <p>
   * This method is idempotent and safe to call multiple times. It will only
   * initialize once.
   * <p>
   * Initialization is triggered automatically via {@link ApplicationReadyEvent}
   * listener after the application context is fully initialized and the
   * {@link CacheManager} is ready.
   */
  public void initialize() {
    if (!initialized.compareAndSet(false, true)) {
      log.debug("Cache metrics already initialized, skipping");
      return;
    }

    try {
      cacheManager.getCacheNames().forEach(this::initializeCacheMetrics);
      log.info("Initialized custom cache metrics (L1/L2, errors) for {} caches",
          cacheManager.getCacheNames().size());
    } catch (Exception e) {
      log.error("Error initializing cache metrics", e);
      // Reset flag on error to allow retry
      initialized.set(false);
      throw e;
    }
  }

  /**
   * Event listener that initializes cache metrics after the application is ready.
   * <p>
   * This ensures that the {@link CacheManager} is fully initialized before
   * attempting to access cache names and register metrics.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    log.debug("Application ready event received, initializing custom cache metrics");
    initialize();
  }

  /**
   * Initialize custom metrics for a specific cache.
   * Only registers L1/L2 hit ratio gauges and error rate gauges.
   */
  private void initializeCacheMetrics(String cacheName) {
    // Register L1 hit ratio gauge (for two-level cache)
    Gauge.builder(METRIC_PREFIX + ".l1.hit.ratio", () -> getL1HitRatio(cacheName))
        .description("L1 cache hit ratio (for two-level cache)")
        .tag(CACHE_TAG, cacheName)
        .register(meterRegistry);

    // Register L2 hit ratio gauge (for two-level cache)
    Gauge.builder(METRIC_PREFIX + ".l2.hit.ratio", () -> getL2HitRatio(cacheName))
        .description("L2 cache hit ratio (for two-level cache)")
        .tag(CACHE_TAG, cacheName)
        .register(meterRegistry);

    // Register error rate gauge
    Gauge.builder(METRIC_PREFIX + ".error.ratio", () -> getErrorRate(cacheName))
        .description("Cache error rate")
        .tag(CACHE_TAG, cacheName)
        .register(meterRegistry);

    log.debug("Initialized custom metrics for cache: {}", cacheName);
  }

  /**
   * Record L1 cache hit (for two-level cache).
   * This is a custom metric not provided by Spring Boot auto-instrumentation.
   */
  public void recordL1Hit(String cacheName) {
    l1Hits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    totalHits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
  }

  /**
   * Record L2 cache hit (for two-level cache).
   * This is a custom metric not provided by Spring Boot auto-instrumentation.
   */
  public void recordL2Hit(String cacheName) {
    l2Hits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    totalHits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
  }

  /**
   * Get L1 hit ratio for a cache (for two-level cache).
   */
  public double getL1HitRatio(String cacheName) {
    long l1HitsCount = l1Hits.getOrDefault(cacheName, new AtomicLong(0)).get();
    long totalHitsCount = totalHits.getOrDefault(cacheName, new AtomicLong(0)).get();
    return totalHitsCount > 0 ? (double) l1HitsCount / totalHitsCount : 0.0;
  }

  /**
   * Get L2 hit ratio for a cache (for two-level cache).
   */
  public double getL2HitRatio(String cacheName) {
    long l2HitsCount = l2Hits.getOrDefault(cacheName, new AtomicLong(0)).get();
    long totalHitsCount = totalHits.getOrDefault(cacheName, new AtomicLong(0)).get();
    return totalHitsCount > 0 ? (double) l2HitsCount / totalHitsCount : 0.0;
  }

  /**
   * Get error rate for a cache.
   */
  public double getErrorRate(String cacheName) {
    long errors = cacheErrors.getOrDefault(cacheName, new AtomicLong(0)).get();
    long totalHitsCount = totalHits.getOrDefault(cacheName, new AtomicLong(0)).get();
    long total = totalHitsCount + errors;
    return total > 0 ? (double) errors / total : 0.0;
  }

  /**
   * Get hit ratio for a cache.
   * <p>
   * This method queries Spring Boot's auto-instrumented cache metrics.
   * Returns hit ratio from {@code cache.hit.ratio} metric if available,
   * otherwise returns 0.0.
   * </p>
   *
   * @param cacheName the cache name
   * @return hit ratio (0.0 to 1.0)
   */
  public double getHitRatio(String cacheName) {
    try {
      // Query Spring Boot's auto-instrumented cache hit ratio metric
      return meterRegistry.find("cache.hit.ratio")
          .tag(CACHE_TAG, cacheName)
          .gauge() != null ? meterRegistry.find("cache.hit.ratio").tag(CACHE_TAG, cacheName).gauge().value() : 0.0;
    } catch (Exception e) {
      log.debug("Could not get hit ratio for cache: {}", cacheName, e);
      return 0.0;
    }
  }

  /**
   * Get overall hit ratio across all caches.
   * <p>
   * This method calculates the average hit ratio across all caches.
   * </p>
   *
   * @return overall hit ratio (0.0 to 1.0)
   */
  public double getOverallHitRatio() {
    try {
      double sum = 0.0;
      int count = 0;

      for (String cacheName : cacheManager.getCacheNames()) {
        double hitRatio = getHitRatio(cacheName);
        if (hitRatio > 0.0) {
          sum += hitRatio;
          count++;
        }
      }

      return count > 0 ? sum / count : 0.0;
    } catch (Exception e) {
      log.debug("Could not calculate overall hit ratio", e);
      return 0.0;
    }
  }

  /**
   * Get latency percentile for a cache.
   * <p>
   * Note: This method is provided for backward compatibility but returns 0.0
   * as we no longer track latency metrics in this class. Latency metrics are
   * available via Spring Boot's auto-instrumentation.
   * </p>
   *
   * @param cacheName  the cache name
   * @param percentile the percentile (0.0 to 1.0)
   * @return latency in milliseconds (0.0 if not available)
   */
  // public double getLatencyPercentile(String cacheName, double percentile) {
  // // Latency tracking has been removed - Spring Boot auto-instrumentation
  // // provides cache metrics via cache.gets timer
  // log.debug("Latency percentile not available - use Spring Boot
  // auto-instrumented metrics");
  // return 0.0;
  // }

  /**
   * Record a cache error.
   * Uses cached Counter instance to avoid re-registration.
   */
  public void recordError(String cacheName, String errorType) {
    cacheErrors.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();

    // Cache Counter instance to avoid re-registration
    String counterKey = cacheName + ":" + errorType;
    Counter counter = errorCounters.computeIfAbsent(counterKey, key -> Counter.builder(METRIC_PREFIX + ".errors.total")
        .description("Total cache operation errors")
        .tag(CACHE_TAG, cacheName)
        .tag("error_type", errorType)
        .register(meterRegistry));

    counter.increment();
  }
}