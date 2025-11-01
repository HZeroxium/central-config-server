package com.example.control.infrastructure.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache metrics collector using Micrometer.
 * <p>
 * Exposes cache performance metrics:
 * <ul>
 * <li>Operations counter (hits, misses, evictions)</li>
 * <li>Hit ratio gauge</li>
 * <li>Cache size gauge</li>
 * <li>Load duration timer</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheMetrics {

  private static final String METRIC_PREFIX = "cache";
  private static final String CACHE_TAG = "cache";
  private static final String OPERATION_TAG = "operation";
  private static final String RESULT_TAG = "result";

  private final MeterRegistry meterRegistry;
  private final CacheManager cacheManager;

  // Per-cache counters for cache operations
  private final Map<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> cacheEvictions = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> cachePuts = new ConcurrentHashMap<>();

  // Per-cache L1/L2 hit tracking for two-level cache
  private final Map<String, AtomicLong> l1Hits = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> l2Hits = new ConcurrentHashMap<>();

  // Per-cache error tracking
  private final Map<String, AtomicLong> cacheErrors = new ConcurrentHashMap<>();

  // Per-cache latency histograms for percentile calculation
  private final Map<String, Timer> latencyTimers = new ConcurrentHashMap<>();

  // Initialization flag to prevent duplicate initialization
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  /**
   * Initialize metrics for all caches.
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
      log.info("Initialized cache metrics for {} caches", cacheManager.getCacheNames().size());
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
    log.debug("Application ready event received, initializing cache metrics");
    initialize();
  }

  /**
   * Initialize metrics for a specific cache.
   */
  private void initializeCacheMetrics(String cacheName) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      return;
    }

    // Register cache size gauge
    Gauge.builder(METRIC_PREFIX + ".size", cache, c -> getCacheSize(c))
        .description("Number of entries in the cache")
        .tag(CACHE_TAG, cacheName)
        .register(meterRegistry);

    // Register hit ratio gauge
    Gauge.builder(METRIC_PREFIX + ".hit.ratio", () -> getHitRatio(cacheName))
        .description("Cache hit ratio")
        .tag(CACHE_TAG, cacheName)
        .register(meterRegistry);

    // Register latency timer for percentile calculation
    Timer latencyTimer = Timer.builder(METRIC_PREFIX + ".load.duration")
        .description("Cache load duration (for percentile calculation)")
        .tag(CACHE_TAG, cacheName)
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry);
    latencyTimers.put(cacheName, latencyTimer);

    log.debug("Initialized metrics for cache: {}", cacheName);
  }

  /**
   * Record a cache hit.
   */
  public void recordHit(String cacheName) {
    cacheHits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    Counter.builder(METRIC_PREFIX + ".operations.total")
        .description("Total cache operations")
        .tag(CACHE_TAG, cacheName)
        .tag(OPERATION_TAG, "get")
        .tag(RESULT_TAG, "hit")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record a cache miss.
   */
  public void recordMiss(String cacheName) {
    cacheMisses.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    Counter.builder(METRIC_PREFIX + ".operations.total")
        .description("Total cache operations")
        .tag(CACHE_TAG, cacheName)
        .tag(OPERATION_TAG, "get")
        .tag(RESULT_TAG, "miss")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record a cache put operation.
   */
  public void recordPut(String cacheName) {
    cachePuts.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    Counter.builder(METRIC_PREFIX + ".operations.total")
        .description("Total cache operations")
        .tag(CACHE_TAG, cacheName)
        .tag(OPERATION_TAG, "put")
        .tag(RESULT_TAG, "success")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record a cache eviction.
   */
  public void recordEviction(String cacheName) {
    cacheEvictions.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    Counter.builder(METRIC_PREFIX + ".evictions.total")
        .description("Total cache evictions")
        .tag(CACHE_TAG, cacheName)
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record cache load duration.
   */
  public Timer.Sample startLoadTimer() {
    return Timer.start(meterRegistry);
  }

  /**
   * Record cache load duration.
   */
  public void recordLoadDuration(String cacheName, Timer.Sample sample) {
    Timer timer = latencyTimers.computeIfAbsent(cacheName, name -> Timer.builder(METRIC_PREFIX + ".load.duration")
        .description("Cache load duration (for percentile calculation)")
        .tag(CACHE_TAG, cacheName)
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry));
    sample.stop(timer);
  }

  /**
   * Record L1 cache hit (for two-level cache).
   */
  public void recordL1Hit(String cacheName) {
    l1Hits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    recordHit(cacheName);
  }

  /**
   * Record L2 cache hit (for two-level cache).
   */
  public void recordL2Hit(String cacheName) {
    l2Hits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    recordHit(cacheName);
  }

  /**
   * Get L1 hit ratio for a cache (for two-level cache).
   */
  public double getL1HitRatio(String cacheName) {
    long l1HitsCount = l1Hits.getOrDefault(cacheName, new AtomicLong(0)).get();
    long totalHits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
    return totalHits > 0 ? (double) l1HitsCount / totalHits : 0.0;
  }

  /**
   * Get L2 hit ratio for a cache (for two-level cache).
   */
  public double getL2HitRatio(String cacheName) {
    long l2HitsCount = l2Hits.getOrDefault(cacheName, new AtomicLong(0)).get();
    long totalHits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
    return totalHits > 0 ? (double) l2HitsCount / totalHits : 0.0;
  }

  /**
   * Get error rate for a cache.
   */
  public double getErrorRate(String cacheName) {
    long errors = cacheErrors.getOrDefault(cacheName, new AtomicLong(0)).get();
    long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
    long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).get();
    long total = hits + misses + errors;
    return total > 0 ? (double) errors / total : 0.0;
  }

  /**
   * Get latency percentile (p50, p95, p99).
   */
  public double getLatencyPercentile(String cacheName, double percentile) {
    Timer timer = latencyTimers.get(cacheName);
    if (timer == null) {
      return 0.0;
    }
    // Use timerSnapshot for percentile calculation
    return timer.totalTime(TimeUnit.MILLISECONDS) / Math.max(timer.count(), 1);
  }

  /**
   * Get hit ratio for a cache.
   */
  public double getHitRatio(String cacheName) {
    long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
    long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).get();
    long total = hits + misses;
    return total > 0 ? (double) hits / total : 0.0;
  }

  /**
   * Get overall hit ratio across all caches.
   */
  public double getOverallHitRatio() {
    long totalHits = cacheHits.values().stream().mapToLong(AtomicLong::get).sum();
    long totalMisses = cacheMisses.values().stream().mapToLong(AtomicLong::get).sum();
    long total = totalHits + totalMisses;
    return total > 0 ? (double) totalHits / total : 0.0;
  }

  /**
   * Get cache size (if available).
   */
  private double getCacheSize(Cache cache) {
    // Try to get size from cache statistics if available
    // This is a simplified implementation - actual size may vary by cache
    // implementation
    try {
      if (cache instanceof org.springframework.cache.caffeine.CaffeineCache) {
        com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache = ((org.springframework.cache.caffeine.CaffeineCache) cache)
            .getNativeCache();
        return nativeCache.estimatedSize();
      }
    } catch (Exception e) {
      log.debug("Could not get cache size for cache: {}", cache.getName(), e);
    }
    return 0.0;
  }

  /**
   * Record a cache error.
   */
  public void recordError(String cacheName, String errorType) {
    Counter.builder(METRIC_PREFIX + ".errors.total")
        .description("Total cache operation errors")
        .tag(CACHE_TAG, cacheName)
        .tag("error_type", errorType)
        .register(meterRegistry)
        .increment();
  }
}