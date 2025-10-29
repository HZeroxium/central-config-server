//package com.example.control.config.cache;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.cache.Cache;
//import org.springframework.cache.CacheManager;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//
/// **
// * REST controller exposing administrative and observability endpoints for the cache layer.
// *
// * <p><strong>Responsibilities</strong></p>
// * <ul>
// *   <li>Report runtime cache health and configuration {@code (/api/cache/health, /api/cache/status)}.</li>
// *   <li>Inspect provider availability and switch cache provider at runtime {@code (/api/cache/providers/**)}.</li>
// *   <li>Inspect basic per-cache statistics (Caffeine-only, when stats are enabled) {@code (/api/cache/stats/{name})}.</li>
// *   <li>Administrative operations: clear a cache or all caches {@code (DELETE /api/cache/...)}.</li>
// * </ul>
// *
// * <p><strong>Notes</strong></p>
// * <ul>
// *   <li>Cache names come from the underlying {@link CacheManager}; names may be lazily created depending on the provider,
// *       as per Spring's cache abstraction contract. </li>
// *   <li>Caffeine statistics require {@code Caffeine.recordStats()} at build time; otherwise counts/rates will be zeros.</li>
// *   <li>Health details can also be obtained via Spring Boot Actuator. Exposing details depends on
// *       {@code management.endpoint.health.show-details}. </li>
// * </ul>
// *
// * <p><strong>Security</strong>: these endpoints perform administrative actions (e.g., provider switch, cache clear) and
// * should be protected by authentication/authorization in production environments.</p>
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/cache")
//@RequiredArgsConstructor
//public class CacheManagementController {
//
//  /**
//   * Factory that knows how to build cache managers for configured providers and can report provider availability.
//   */
//  private final CacheManagerFactory cacheManagerFactory;
//
//  /**
//   * Strongly-typed cache configuration properties (source of truth for provider intent, TTL overrides, fallback flags).
//   */
//  private final CacheProperties cacheProperties;
//
//  /**
//   * Primary cache manager wrapper that allows atomic runtime switching of the underlying provider.
//   */
//  @Qualifier("delegatingCacheManager")
//  private final DelegatingCacheManager delegatingCacheManager;
//
//  /**
//   * Health indicator that assembles diagnostics about the cache layer (configured vs. actual provider, availability, etc.).
//   */
//  private final CacheHealthIndicator cacheHealthIndicator;
//
//  /**
//   * Return a diagnostic map describing the current cache health (suitable for UI/ops dashboards).
//   *
//   * <p>For full health aggregation and standardized JSON format, consider using Spring Boot Actuator's
//   * {@code /actuator/health} (ensure details exposure is configured). </p>
//   *
//   * @return HTTP 200 with health details; never throws
//   */
//  @GetMapping("/health")
//  public ResponseEntity<Map<String, Object>> getCacheHealth() {
//    Map<String, Object> health = cacheHealthIndicator.getHealthDetails();
//    return ResponseEntity.ok(health);
//  }
//
//  /**
//   * Return current cache configuration and lightweight status indicators.
//   *
//   * <p>Includes:</p>
//   * <ul>
//   *   <li><b>currentProvider</b>: detected from the current delegate type of {@link DelegatingCacheManager}.</li>
//   *   <li><b>configuredProvider</b>: desired provider from {@link CacheProperties}.</li>
//   *   <li><b>fallbackEnabled</b>: whether provider fallbacks are allowed.</li>
//   *   <li><b>cacheNames</b>: names known to the current {@link CacheManager}. Caches may be lazily created by providers. </li>
//   *   <li><b>availableProviders</b>: availability map for all providers.</li>
//   *   <li><b>cacheStats</b>: a summary of cache types/native types currently in use.</li>
//   * </ul>
//   *
//   * @return HTTP 200 with status; HTTP 500 if an unexpected error occurs
//   */
//  @GetMapping("/status")
//  public ResponseEntity<Map<String, Object>> getCacheStatus() {
//    Map<String, Object> status = new HashMap<>();
//
//    try {
//      // Get the actual current provider from the DelegatingCacheManager
//      String actualProvider = getCurrentProviderFromManager();
//      status.put("currentProvider", actualProvider);
//      status.put("configuredProvider", cacheProperties.getProvider().name());
//      status.put("fallbackEnabled", cacheProperties.isEnableFallback());
//      status.put("cacheNames", delegatingCacheManager.getCacheNames());
//      status.put("availableProviders", getAvailableProviders());
//      status.put("cacheStats", getCacheStatistics());
//
//      return ResponseEntity.ok(status);
//    } catch (Exception e) {
//      log.error("Error getting cache status", e);
//      status.put("error", e.getMessage());
//      return ResponseEntity.internalServerError().body(status);
//    }
//  }
//
//  /**
//   * Return provider-specific statistics for a single cache (Caffeine only, when statistics are enabled).
//   *
//   * <p><strong>Caffeine statistics</strong>: requires {@code Caffeine.recordStats()} during cache build.
//   * When enabled, {@code cache.getNativeCache()} returns the underlying Caffeine {@code Cache} and
//   * {@code Cache.stats()} provides hit/miss/eviction metrics. </p>
//   *
//   * @param cacheName logical cache name
//   * @return HTTP 200 with stats or HTTP 404 if the cache name is unknown
//   */
//  @GetMapping("/stats/{cacheName}")
//  public ResponseEntity<Map<String, Object>> getCacheStats(@PathVariable String cacheName) {
//    try {
//      Cache cache = delegatingCacheManager.getCache(cacheName);
//      if (cache == null) {
//        return ResponseEntity.notFound().build();
//        }
//
//      Map<String, Object> stats = new HashMap<>();
//      stats.put("cacheName", cacheName);
//      stats.put("cacheType", cache.getClass().getSimpleName());
//
//      // Add provider-specific statistics if available (Caffeine)
//      Object nativeCache = cache.getNativeCache();
//      if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
//        var caffeineCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
//        var caffeineStats = caffeineCache.stats();
//
//        stats.put("hitCount", caffeineStats.hitCount());
//        stats.put("missCount", caffeineStats.missCount());
//        stats.put("hitRate", caffeineStats.hitRate());
//        stats.put("evictionCount", caffeineStats.evictionCount());
//        stats.put("estimatedSize", caffeineCache.estimatedSize());
//      }
//
//      return ResponseEntity.ok(stats);
//    } catch (Exception e) {
//      log.error("Error getting cache stats for: {}", cacheName, e);
//      Map<String, Object> error = Map.of("error", e.getMessage());
//      return ResponseEntity.internalServerError().body(error);
//    }
//  }
//
//  /**
//   * Clear the contents of a specific cache by name.
//   *
//   * <p>Semantics follow the underlying provider; for example, {@code clear()} may be asynchronous
//   * or immediate depending on implementation details of the native cache. </p>
//   *
//   * @param cacheName logical cache name
//   * @return HTTP 200 on success, HTTP 404 if the cache does not exist
//   */
//  @DeleteMapping("/{cacheName}")
//  public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
//    try {
//      Cache cache = delegatingCacheManager.getCache(cacheName);
//      if (cache == null) {
//        return ResponseEntity.notFound().build();
//      }
//
//      cache.clear();
//      log.info("Cleared cache: {}", cacheName);
//
//      Map<String, String> response = Map.of(
//          "message", "Cache cleared successfully",
//          "cacheName", cacheName);
//
//      return ResponseEntity.ok(response);
//    } catch (Exception e) {
//      log.error("Error clearing cache: {}", cacheName, e);
//      Map<String, String> error = Map.of("error", e.getMessage());
//      return ResponseEntity.internalServerError().body(error);
//    }
//  }
//
//  /**
//   * Clear all known caches as reported by the current {@link CacheManager}.
//   *
//   * @return HTTP 200 with number of caches cleared; HTTP 500 on error
//   */
//  @DeleteMapping("/all")
//  public ResponseEntity<Map<String, Object>> clearAllCaches() {
//    try {
//      var cacheNames = delegatingCacheManager.getCacheNames();
//      int clearedCount = 0;
//
//      for (String cacheName : cacheNames) {
//        Cache cache = delegatingCacheManager.getCache(cacheName);
//        if (cache != null) {
//          cache.clear();
//          clearedCount++;
//        }
//      }
//
//      log.info("Cleared {} caches", clearedCount);
//
//      Map<String, Object> response = Map.of(
//          "message", "All caches cleared successfully",
//          "clearedCount", clearedCount,
//          "cacheNames", cacheNames);
//
//      return ResponseEntity.ok(response);
//    } catch (Exception e) {
//      log.error("Error clearing all caches", e);
//      Map<String, Object> error = Map.of("error", e.getMessage());
//      return ResponseEntity.internalServerError().body(error);
//    }
//  }
//
//  /**
//   * Return a map of cache providers to their availability flags and descriptions.
//   *
//   * <p>Availability is determined by {@link CacheManagerFactory#isProviderAvailable(CacheProperties.CacheProvider)}
//   * (e.g., Redis requires a {@code RedisConnectionFactory} to be present). </p>
//   *
//   * @return HTTP 200 with current configured provider and availability map
//   */
//  @GetMapping("/providers")
//  public ResponseEntity<Map<String, Object>> getAvailableProviders() {
//    Map<String, Object> providers = new HashMap<>();
//
//    for (CacheProperties.CacheProvider provider : CacheProperties.CacheProvider.values()) {
//      boolean available = cacheManagerFactory.isProviderAvailable(provider);
//      providers.put(provider.name(), Map.of(
//          "available", available,
//          "description", getProviderDescription(provider)));
//    }
//
//    return ResponseEntity.ok(Map.of(
//        "current", cacheProperties.getProvider().name(),
//        "providers", providers));
//  }
//
//  /**
//   * Switch the cache provider at runtime.
//   *
//   * <p><strong>Current behavior</strong> (by design of this controller):
//   * updates {@link CacheProperties#setProvider(CacheProperties.CacheProvider)} first, then creates a new manager
//   * and atomically switches the delegate. If manager creation fails, the switch is not applied and the old manager
//   * stays active; however the configured provider value may already have been updated (see suggestions). </p>
//   *
//   * @param provider provider name (case-insensitive): CAFFEINE | REDIS | TWO_LEVEL | NOOP
//   * @return HTTP 200 on success; HTTP 400 for invalid/unavailable provider; HTTP 500 on error
//   */
//  @PostMapping("/providers/{provider}")
//  public ResponseEntity<Map<String, Object>> switchCacheProvider(@PathVariable String provider) {
//    try {
//      CacheProperties.CacheProvider newProvider = CacheProperties.CacheProvider.valueOf(provider.toUpperCase());
//
//      // Check if provider is available
//      if (!cacheManagerFactory.isProviderAvailable(newProvider)) {
//        return ResponseEntity.badRequest()
//            .body(Map.of("error", "Provider not available: " + provider,
//                        "reason", getProviderUnavailableReason(newProvider)));
//      }
//
//      // Create new manager and switch the delegate atomically
//      CacheManager newManager = cacheManagerFactory.createCacheManager();
//
//      CacheProperties.CacheProvider oldProvider = cacheProperties.getProvider();
//      delegatingCacheManager.switchCacheManager(newManager);
//      log.info("Switching cache provider from {} to {}", oldProvider, newProvider);
//
//      // Update the provider configuration
//      cacheProperties.setProvider(newProvider);
//
//      log.info("Successfully switched cache provider from {} to {}", oldProvider, newProvider);
//
//      Map<String, Object> response = Map.of(
//          "message", "Cache provider switched successfully",
//          "oldProvider", oldProvider.name(),
//          "newProvider", newProvider.name(),
//          "timestamp", System.currentTimeMillis(),
//          "cacheManagerType", newManager.getClass().getSimpleName());
//
//      return ResponseEntity.ok(response);
//    } catch (IllegalArgumentException iae) {
//      return ResponseEntity.badRequest().body(Map.of("error", "Invalid provider: " + provider));
//    } catch (Exception e) {
//      log.error("Error switching cache provider to: {}", provider, e);
//      return ResponseEntity.internalServerError().body(Map.of(
//          "error", "Failed to switch cache provider",
//          "provider", provider,
//          "details", e.getMessage()));
//    }
//  }
//
//  /**
//   * Update portions of the cache configuration at runtime.
//   *
//   * <p>Supported updates:</p>
//   * <ul>
//   *   <li><b>provider</b>: switch desired provider in {@link CacheProperties} (availability checked).</li>
//   *   <li><b>enableFallback</b>: toggle global fallback flag.</li>
//   *   <li><b>cacheTtl</b>: map of {@code cacheName -> ISO-8601 Duration} (e.g., {@code PT2M}, {@code PT30S}).
//   *       Parsed via {@link java.time.Duration#parse(CharSequence)}. </li>
//   * </ul>
//   *
//   * <p><strong>Important</strong>: this endpoint mutates configuration but does <em>not</em> rebuild the current
//   * providers. TTL/provider changes take effect after you create/switch a manager accordingly. </p>
//   *
//   * @param configUpdates JSON map of updates
//   * @return HTTP 200 with applied changes; HTTP 400 for invalid input; HTTP 500 on error
//   */
//  @PutMapping("/config")
//  public ResponseEntity<Map<String, Object>> updateCacheConfig(@RequestBody Map<String, Object> configUpdates) {
//    try {
//      Map<String, Object> appliedChanges = new HashMap<>();
//
//      // Update provider if specified
//      if (configUpdates.containsKey("provider")) {
//        String providerStr = configUpdates.get("provider").toString();
//        try {
//          CacheProperties.CacheProvider newProvider = CacheProperties.CacheProvider.valueOf(providerStr.toUpperCase());
//          if (cacheManagerFactory.isProviderAvailable(newProvider)) {
//            CacheProperties.CacheProvider oldProvider = cacheProperties.getProvider();
//            cacheProperties.setProvider(newProvider);
//            appliedChanges.put("provider", Map.of("old", oldProvider.name(), "new", newProvider.name()));
//          } else {
//            return ResponseEntity.badRequest()
//                .body(Map.of("error", "Provider not available: " + providerStr));
//          }
//        } catch (IllegalArgumentException e) {
//          return ResponseEntity.badRequest()
//              .body(Map.of("error", "Invalid provider: " + providerStr));
//        }
//      }
//
//      // Update fallback setting if specified
//      if (configUpdates.containsKey("enableFallback")) {
//        boolean oldFallback = cacheProperties.isEnableFallback();
//        boolean newFallback = Boolean.parseBoolean(configUpdates.get("enableFallback").toString());
//        cacheProperties.setEnableFallback(newFallback);
//        appliedChanges.put("enableFallback", Map.of("old", oldFallback, "new", newFallback));
//      }
//
//      // Update TTL for specific caches if specified
//      Object cacheTtlObj = configUpdates.get("cacheTtl");
//      if (cacheTtlObj instanceof Map<?, ?> rawTtlUpdates) {
//        Map<String, Object> ttlChanges = new HashMap<>();
//
//        for (Map.Entry<?, ?> entry : rawTtlUpdates.entrySet()) {
//          Object key = entry.getKey();
//          Object value = entry.getValue();
//          if (key instanceof String cacheName) {
//            if (cacheProperties.getCaches().containsKey(cacheName)) {
//              try {
//                String ttlStr = value.toString();
//                Duration newTtl = Duration.parse(ttlStr); // ISO-8601 (e.g., PT10M, PT30S)
//                Duration oldTtl = cacheProperties.getCaches().get(cacheName).getTtl();
//                cacheProperties.getCaches().get(cacheName).setTtl(newTtl);
//                ttlChanges.put(cacheName, Map.of("old", oldTtl.toString(), "new", newTtl.toString()));
//              } catch (Exception e) {
//                log.warn("Failed to update TTL for cache {}: {}", cacheName, e.getMessage());
//              }
//            }
//          }
//        }
//        if (!ttlChanges.isEmpty()) {
//          appliedChanges.put("cacheTtl", ttlChanges);
//        }
//      }
//
//      Map<String, Object> response = Map.of(
//          "message", "Cache configuration updated successfully",
//          "appliedChanges", appliedChanges,
//          "timestamp", System.currentTimeMillis(),
//          "currentProvider", cacheProperties.getProvider().name());
//
//      return ResponseEntity.ok(response);
//    } catch (Exception e) {
//      log.error("Error updating cache configuration", e);
//      Map<String, Object> error = Map.of(
//          "error", "Failed to update cache configuration",
//          "details", e.getMessage());
//      return ResponseEntity.internalServerError().body(error);
//    }
//  }
//
//  /**
//   * Perform a simple put/get/evict cycle against the {@code service-instances} cache to verify that
//   * the current provider is functioning end-to-end.
//   *
//   * <p>Returns the generated key/value and the retrieved value (if any). </p>
//   *
//   * @return HTTP 200 with test results; HTTP 400 if the test cache does not exist; HTTP 500 on error
//   */
//  @PostMapping("/test")
//  public ResponseEntity<Map<String, Object>> testCache() {
//    String testKey = "test-key-" + System.currentTimeMillis();
//    String testValue = "test-value-" + System.currentTimeMillis();
//
//    try {
//      // Test with service-instances cache
//      Cache cache = delegatingCacheManager.getCache("service-instances");
//      if (cache == null) {
//        return ResponseEntity.badRequest()
//            .body(Map.of("error", "service-instances cache not found"));
//      }
//
//      // Test put and get
//      cache.put(testKey, testValue);
//      Cache.ValueWrapper retrieved = cache.get(testKey);
//
//      boolean success = retrieved != null && testValue.equals(retrieved.get());
//
//      // Clean up
//      cache.evict(testKey);
//
//      Map<String, Object> result = Map.of(
//          "success", success,
//          "provider", cacheProperties.getProvider().name(),
//          "testKey", testKey,
//          "testValue", testValue,
//          "retrieved", retrieved != null ? retrieved.get() : null);
//
//      return ResponseEntity.ok(result);
//    } catch (Exception e) {
//      log.error("Error testing cache", e);
//      Map<String, Object> error = Map.of(
//          "success", false,
//          "error", e.getMessage());
//      return ResponseEntity.internalServerError().body(error);
//    }
//  }
//
//  /**
//   * Build a summary of cache types and their native cache types, as returned by the current manager.
//   *
//   * <p>When the underlying cache is Spring's Caffeine adapter, {@code getNativeCache()} returns the
//   * actual Caffeine {@code Cache} implementation class. </p>
//   *
//   * @return a map from cacheName → {type, nativeType}
//   */
//  private Map<String, Object> getCacheStatistics() {
//    Map<String, Object> stats = new HashMap<>();
//
//    for (String cacheName : delegatingCacheManager.getCacheNames()) {
//      Cache cache = delegatingCacheManager.getCache(cacheName);
//      if (cache != null) {
//        stats.put(cacheName, Map.of(
//            "type", cache.getClass().getSimpleName(),
//            "nativeType", cache.getNativeCache().getClass().getSimpleName()));
//      }
//    }
//
//    return stats;
//  }
//
//  /**
//   * Determine the current provider by mapping the delegate type of the active {@link CacheManager}.
//   *
//   * @return one of CAFFEINE | REDIS | TWO_LEVEL | NOOP | or the raw delegate type if unknown
//   */
//  private String getCurrentProviderFromManager() {
//    String managerType = delegatingCacheManager.getCurrentDelegateType();
//
//    // Map manager types to provider names
//    switch (managerType) {
//      case "CaffeineCacheManager":
//        return "CAFFEINE";
//      case "RedisCacheManager":
//        return "REDIS";
//      case "TwoLevelCacheManager":
//        return "TWO_LEVEL";
//      case "NoOpCacheManager":
//        return "NOOP";
//      default:
//        return managerType; // Return the type if we don't recognize it
//    }
//  }
//
//  /**
//   * Human-readable description for each provider.
//   *
//   * @param provider enum value
//   * @return short description
//   */
//  private String getProviderDescription(CacheProperties.CacheProvider provider) {
//    switch (provider) {
//      case CAFFEINE:
//        return "Local in-memory cache using Caffeine";
//      case REDIS:
//        return "Distributed cache using Redis";
//      case TWO_LEVEL:
//        return "Two-level cache (L1: Caffeine, L2: Redis)";
//      case NOOP:
//        return "No-operation cache (caching disabled)";
//      default:
//        return "Unknown provider";
//    }
//  }
//
//  /**
//   * Reason string for unavailability of a provider, primarily used for HTTP 400 responses.
//   *
//   * @param provider enum value
//   * @return textual reason
//   */
//  private String getProviderUnavailableReason(CacheProperties.CacheProvider provider) {
//    switch (provider) {
//      case REDIS:
//      case TWO_LEVEL:
//        return "Redis connection factory not available";
//      default:
//        return "Provider not supported";
//    }
//  }
//}
