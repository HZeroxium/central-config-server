package com.example.rest.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for cache management and monitoring.
 * Provides endpoints for runtime cache configuration and statistics.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
public class CacheManagementController {

  private final CacheManagerFactory cacheManagerFactory;
  private final CacheProperties cacheProperties;
  private final CacheManager currentCacheManager;
  private final CacheHealthIndicator cacheHealthIndicator;

  /**
   * Get cache health status.
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> getCacheHealth() {
    Map<String, Object> health = cacheHealthIndicator.getHealthStatus();
    return ResponseEntity.ok(health);
  }

  /**
   * Get current cache configuration and statistics.
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getCacheStatus() {
    Map<String, Object> status = new HashMap<>();

    try {
      status.put("currentProvider", cacheProperties.getProvider().name());
      status.put("fallbackEnabled", cacheProperties.isEnableFallback());
      status.put("cacheNames", currentCacheManager.getCacheNames());
      status.put("availableProviders", getAvailableProviders());
      status.put("cacheStats", getCacheStatistics());

      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("Error getting cache status", e);
      status.put("error", e.getMessage());
      return ResponseEntity.internalServerError().body(status);
    }
  }

  /**
   * Get detailed statistics for a specific cache.
   */
  @GetMapping("/stats/{cacheName}")
  public ResponseEntity<Map<String, Object>> getCacheStats(@PathVariable String cacheName) {
    try {
      Cache cache = currentCacheManager.getCache(cacheName);
      if (cache == null) {
        return ResponseEntity.notFound().build();
      }

      Map<String, Object> stats = new HashMap<>();
      stats.put("cacheName", cacheName);
      stats.put("cacheType", cache.getClass().getSimpleName());

      // Add provider-specific statistics if available
      Object nativeCache = cache.getNativeCache();
      if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
        var caffeineCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
        var caffeineStats = caffeineCache.stats();

        stats.put("hitCount", caffeineStats.hitCount());
        stats.put("missCount", caffeineStats.missCount());
        stats.put("hitRate", caffeineStats.hitRate());
        stats.put("evictionCount", caffeineStats.evictionCount());
        stats.put("estimatedSize", caffeineCache.estimatedSize());
      }

      return ResponseEntity.ok(stats);
    } catch (Exception e) {
      log.error("Error getting cache stats for: {}", cacheName, e);
      Map<String, Object> error = Map.of("error", e.getMessage());
      return ResponseEntity.internalServerError().body(error);
    }
  }

  /**
   * Clear a specific cache.
   */
  @DeleteMapping("/{cacheName}")
  public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
    try {
      Cache cache = currentCacheManager.getCache(cacheName);
      if (cache == null) {
        return ResponseEntity.notFound().build();
      }

      cache.clear();
      log.info("Cleared cache: {}", cacheName);

      Map<String, String> response = Map.of(
          "message", "Cache cleared successfully",
          "cacheName", cacheName);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error clearing cache: {}", cacheName, e);
      Map<String, String> error = Map.of("error", e.getMessage());
      return ResponseEntity.internalServerError().body(error);
    }
  }

  /**
   * Clear all caches.
   */
  @DeleteMapping("/all")
  public ResponseEntity<Map<String, Object>> clearAllCaches() {
    try {
      var cacheNames = currentCacheManager.getCacheNames();
      int clearedCount = 0;

      for (String cacheName : cacheNames) {
        Cache cache = currentCacheManager.getCache(cacheName);
        if (cache != null) {
          cache.clear();
          clearedCount++;
        }
      }

      log.info("Cleared {} caches", clearedCount);

      Map<String, Object> response = Map.of(
          "message", "All caches cleared successfully",
          "clearedCount", clearedCount,
          "cacheNames", cacheNames);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error clearing all caches", e);
      Map<String, Object> error = Map.of("error", e.getMessage());
      return ResponseEntity.internalServerError().body(error);
    }
  }

  /**
   * Get available cache providers.
   */
  @GetMapping("/providers")
  public ResponseEntity<Map<String, Object>> getAvailableProviders() {
    Map<String, Object> providers = new HashMap<>();

    for (CacheProperties.CacheProvider provider : CacheProperties.CacheProvider.values()) {
      boolean available = cacheManagerFactory.isProviderAvailable(provider);
      providers.put(provider.name(), Map.of(
          "available", available,
          "description", getProviderDescription(provider)));
    }

    return ResponseEntity.ok(Map.of(
        "current", cacheProperties.getProvider().name(),
        "providers", providers));
  }

  /**
   * Switch cache provider at runtime.
   */
  @PostMapping("/providers/{provider}")
  public ResponseEntity<Map<String, Object>> switchCacheProvider(@PathVariable String provider) {
    try {
      CacheProperties.CacheProvider newProvider;
      try {
        newProvider = CacheProperties.CacheProvider.valueOf(provider.toUpperCase());
      } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Invalid provider: " + provider,
                "availableProviders", Arrays.stream(CacheProperties.CacheProvider.values())
                    .map(Enum::name).toList()));
      }

      // Check if provider is available
      if (!cacheManagerFactory.isProviderAvailable(newProvider)) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Provider not available: " + provider,
                "reason", getProviderUnavailableReason(newProvider)));
      }

      CacheProperties.CacheProvider oldProvider = cacheProperties.getProvider();

      // Update the provider
      cacheProperties.setProvider(newProvider);
      log.info("Switching cache provider from {} to {}", oldProvider, newProvider);

      // Test the new provider by creating a cache manager
      CacheManager testManager = cacheManagerFactory.createCacheManager();
      log.info("Successfully created new cache manager with provider: {}", newProvider);

      Map<String, Object> response = Map.of(
          "message", "Cache provider switched successfully",
          "oldProvider", oldProvider.name(),
          "newProvider", newProvider.name(),
          "timestamp", System.currentTimeMillis(),
          "cacheManagerType", testManager.getClass().getSimpleName());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error switching cache provider to: {}", provider, e);

      // Rollback on error
      Map<String, Object> error = Map.of(
          "error", "Failed to switch cache provider",
          "provider", provider,
          "details", e.getMessage(),
          "currentProvider", cacheProperties.getProvider().name());

      return ResponseEntity.internalServerError().body(error);
    }
  }

  /**
   * Update cache configuration at runtime.
   */
  @PutMapping("/config")
  public ResponseEntity<Map<String, Object>> updateCacheConfig(@RequestBody Map<String, Object> configUpdates) {
    try {
      Map<String, Object> appliedChanges = new HashMap<>();

      // Update provider if specified
      if (configUpdates.containsKey("provider")) {
        String providerStr = configUpdates.get("provider").toString();
        try {
          CacheProperties.CacheProvider newProvider = CacheProperties.CacheProvider.valueOf(providerStr.toUpperCase());
          if (cacheManagerFactory.isProviderAvailable(newProvider)) {
            CacheProperties.CacheProvider oldProvider = cacheProperties.getProvider();
            cacheProperties.setProvider(newProvider);
            appliedChanges.put("provider", Map.of("old", oldProvider.name(), "new", newProvider.name()));
          } else {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Provider not available: " + providerStr));
          }
        } catch (IllegalArgumentException e) {
          return ResponseEntity.badRequest()
              .body(Map.of("error", "Invalid provider: " + providerStr));
        }
      }

      // Update fallback setting if specified
      if (configUpdates.containsKey("enableFallback")) {
        boolean oldFallback = cacheProperties.isEnableFallback();
        boolean newFallback = Boolean.parseBoolean(configUpdates.get("enableFallback").toString());
        cacheProperties.setEnableFallback(newFallback);
        appliedChanges.put("enableFallback", Map.of("old", oldFallback, "new", newFallback));
      }

      // Update TTL for specific caches if specified
      Object cacheTtlObj = configUpdates.get("cacheTtl");
      if (cacheTtlObj instanceof Map<?, ?> rawTtlUpdates) {
        Map<String, Object> ttlChanges = new HashMap<>();

        for (Map.Entry<?, ?> entry : rawTtlUpdates.entrySet()) {
          Object key = entry.getKey();
          Object value = entry.getValue();
          if (key instanceof String cacheName) {
            if (cacheProperties.getCaches().containsKey(cacheName)) {
              try {
                String ttlStr = value.toString();
                java.time.Duration newTtl = java.time.Duration.parse(ttlStr);
                java.time.Duration oldTtl = cacheProperties.getCaches().get(cacheName).getTtl();
                cacheProperties.getCaches().get(cacheName).setTtl(newTtl);
                ttlChanges.put(cacheName, Map.of("old", oldTtl.toString(), "new", newTtl.toString()));
              } catch (Exception e) {
                log.warn("Failed to update TTL for cache {}: {}", cacheName, e.getMessage());
              }
            }
          }
        }
        if (!ttlChanges.isEmpty()) {
          appliedChanges.put("cacheTtl", ttlChanges);
        }
      }

      Map<String, Object> response = Map.of(
          "message", "Cache configuration updated successfully",
          "appliedChanges", appliedChanges,
          "timestamp", System.currentTimeMillis(),
          "currentProvider", cacheProperties.getProvider().name());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error updating cache configuration", e);
      Map<String, Object> error = Map.of(
          "error", "Failed to update cache configuration",
          "details", e.getMessage());
      return ResponseEntity.internalServerError().body(error);
    }
  }

  /**
   * Test cache functionality.
   */
  @PostMapping("/test")
  public ResponseEntity<Map<String, Object>> testCache() {
    String testKey = "test-key-" + System.currentTimeMillis();
    String testValue = "test-value-" + System.currentTimeMillis();

    try {
      // Test with userById cache
      Cache cache = currentCacheManager.getCache("userById");
      if (cache == null) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "userById cache not found"));
      }

      // Test put and get
      cache.put(testKey, testValue);
      Cache.ValueWrapper retrieved = cache.get(testKey);

      boolean success = retrieved != null && testValue.equals(retrieved.get());

      // Clean up
      cache.evict(testKey);

      Map<String, Object> result = Map.of(
          "success", success,
          "provider", cacheProperties.getProvider().name(),
          "testKey", testKey,
          "testValue", testValue,
          "retrieved", retrieved != null ? retrieved.get() : null);

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Error testing cache", e);
      Map<String, Object> error = Map.of(
          "success", false,
          "error", e.getMessage());
      return ResponseEntity.internalServerError().body(error);
    }
  }

  private Map<String, Object> getCacheStatistics() {
    Map<String, Object> stats = new HashMap<>();

    for (String cacheName : currentCacheManager.getCacheNames()) {
      Cache cache = currentCacheManager.getCache(cacheName);
      if (cache != null) {
        stats.put(cacheName, Map.of(
            "type", cache.getClass().getSimpleName(),
            "nativeType", cache.getNativeCache().getClass().getSimpleName()));
      }
    }

    return stats;
  }

  private String getProviderDescription(CacheProperties.CacheProvider provider) {
    switch (provider) {
      case CAFFEINE:
        return "Local in-memory cache using Caffeine";
      case REDIS:
        return "Distributed cache using Redis";
      case TWO_LEVEL:
        return "Two-level cache (L1: Caffeine, L2: Redis)";
      case NOOP:
        return "No-operation cache (caching disabled)";
      default:
        return "Unknown provider";
    }
  }

  private String getProviderUnavailableReason(CacheProperties.CacheProvider provider) {
    switch (provider) {
      case REDIS:
      case TWO_LEVEL:
        return "Redis connection factory not available";
      default:
        return "Provider not supported";
    }
  }
}
