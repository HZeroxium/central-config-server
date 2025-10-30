package com.example.control.infrastructure.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazy cache warmer that monitors cache miss rates and triggers warmup when
 * needed.
 * <p>
 * Monitors cache performance and triggers warmup for caches with high miss
 * rates
 * (>50% threshold configurable).
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LazyCacheWarmer {

  private static final double MISS_RATE_THRESHOLD = 0.5; // 50% miss rate threshold

  private final CacheMetrics cacheMetrics;
  private final CacheManager cacheManager;

  // Track which caches have been warmed recently to avoid excessive warmup
  private final Map<String, Long> lastWarmupTime = new ConcurrentHashMap<>();
  private static final long WARMUP_COOLDOWN_MS = 300_000; // 5 minutes

  /**
   * Monitor cache miss rates and trigger warmup if needed.
   * Runs every 5 minutes.
   */
  @Scheduled(fixedRate = 300_000) // 5 minutes
  public void monitorAndWarmCaches() {
    log.debug("Monitoring cache miss rates...");

    cacheManager.getCacheNames().forEach(cacheName -> {
      try {
        double hitRatio = cacheMetrics.getHitRatio(cacheName);
        double missRate = 1.0 - hitRatio;

        if (missRate > MISS_RATE_THRESHOLD) {
          log.info("Cache '{}' has high miss rate: {:.2f}%, considering warmup", cacheName, missRate * 100);

          // Check if we've warmed this cache recently
          Long lastWarmup = lastWarmupTime.get(cacheName);
          long now = System.currentTimeMillis();

          if (lastWarmup == null || (now - lastWarmup) > WARMUP_COOLDOWN_MS) {
            log.info("Triggering warmup for cache: {}", cacheName);
            warmCache(cacheName);
            lastWarmupTime.put(cacheName, now);
          } else {
            log.debug("Skipping warmup for cache '{}' - warmed recently", cacheName);
          }
        }
      } catch (Exception e) {
        log.warn("Error monitoring cache: {}", cacheName, e);
      }
    });
  }

  /**
   * Warm a specific cache.
   * This is a placeholder - actual warmup logic should be implemented per cache
   * type.
   */
  private void warmCache(String cacheName) {
    log.info("Warming cache: {}", cacheName);
    // TODO: Implement cache-specific warmup logic
    // For now, this is a placeholder - actual warmup should be delegated to
    // cache-specific warmers or query services
    log.debug("Cache warmup for '{}' completed (placeholder)", cacheName);
  }

  /**
   * Get overall cache health metrics.
   */
  public Map<String, Object> getCacheHealthMetrics() {
    Map<String, Object> metrics = new java.util.HashMap<>();

    cacheManager.getCacheNames().forEach(cacheName -> {
      Map<String, Object> cacheMetricDetails = new java.util.HashMap<>();
      cacheMetricDetails.put("hitRatio", cacheMetrics.getHitRatio(cacheName));
      cacheMetricDetails.put("missRate", 1.0 - cacheMetrics.getHitRatio(cacheName));
      metrics.put(cacheName, cacheMetricDetails);
    });

    metrics.put("overallHitRatio", cacheMetrics.getOverallHitRatio());
    return metrics;
  }
}
