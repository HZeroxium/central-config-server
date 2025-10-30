package com.example.control.infrastructure.config.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

  /**
   * Initialize metrics for all caches.
   */
  public void initialize() {
    cacheManager.getCacheNames().forEach(this::initializeCacheMetrics);
    log.info("Initialized cache metrics for {} caches", cacheManager.getCacheNames().size());
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
    sample.stop(Timer.builder(METRIC_PREFIX + ".load.duration")
        .description("Cache load duration")
        .tag(CACHE_TAG, cacheName)
        .register(meterRegistry));
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
   * Register hit ratio gauge for a cache.
   */
  public void registerHitRatioGauge(String cacheName) {
    Gauge.builder(METRIC_PREFIX + ".hit.ratio", () -> getHitRatio(cacheName))
        .description("Cache hit ratio")
        .tag(CACHE_TAG, cacheName)
        .register(meterRegistry);
  }
}
