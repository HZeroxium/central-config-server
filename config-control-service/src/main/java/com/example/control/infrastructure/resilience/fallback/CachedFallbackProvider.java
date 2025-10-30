package com.example.control.infrastructure.resilience.fallback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Generic cache-based fallback provider for GET operations.
 * <p>
 * Provides fallback values from cache when external services are unavailable.
 * Uses Spring Cache abstraction to support multiple cache implementations
 * (Caffeine, Redis, etc.).
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CachedFallbackProvider {

  private final CacheManager cacheManager;

  /**
   * Get value from cache.
   *
   * @param cacheName Cache name
   * @param key       Cache key
   * @param valueType Expected value type
   * @param <T>       Value type
   * @return Optional containing cached value if present
   */
  public <T> Optional<T> getFromCache(String cacheName, Object key, Class<T> valueType) {
    try {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache == null) {
        log.warn("Cache {} not found", cacheName);
        return Optional.empty();
      }

      Cache.ValueWrapper wrapper = cache.get(key);
      if (wrapper == null) {
        log.debug("Cache miss for {}: {}", cacheName, key);
        return Optional.empty();
      }

      Object value = wrapper.get();
      if (value != null && valueType.isInstance(value)) {
        log.debug("Cache hit for {}: {}", cacheName, key);
        return Optional.of(valueType.cast(value));
      } else {
        log.warn("Cache value type mismatch for {}: {}, expected {}, got {}",
            cacheName, key, valueType.getSimpleName(),
            value != null ? value.getClass().getSimpleName() : "null");
        return Optional.empty();
      }
    } catch (Exception e) {
      log.error("Error retrieving from cache {}: {}", cacheName, key, e);
      return Optional.empty();
    }
  }

  /**
   * Save value to cache.
   *
   * @param cacheName Cache name
   * @param key       Cache key
   * @param value     Value to cache
   */
  public void saveToCache(String cacheName, Object key, Object value) {
    try {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache == null) {
        log.warn("Cache {} not found, cannot save", cacheName);
        return;
      }

      cache.put(key, value);
      log.debug("Saved to cache {}: {}", cacheName, key);
    } catch (Exception e) {
      log.error("Error saving to cache {}: {}", cacheName, key, e);
    }
  }

  /**
   * Evict value from cache.
   *
   * @param cacheName Cache name
   * @param key       Cache key
   */
  public void evictFromCache(String cacheName, Object key) {
    try {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache == null) {
        log.warn("Cache {} not found, cannot evict", cacheName);
        return;
      }

      cache.evict(key);
      log.debug("Evicted from cache {}: {}", cacheName, key);
    } catch (Exception e) {
      log.error("Error evicting from cache {}: {}", cacheName, key, e);
    }
  }

  /**
   * Clear all entries from a cache.
   *
   * @param cacheName Cache name
   */
  public void clearCache(String cacheName) {
    try {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache == null) {
        log.warn("Cache {} not found, cannot clear", cacheName);
        return;
      }

      cache.clear();
      log.info("Cleared cache: {}", cacheName);
    } catch (Exception e) {
      log.error("Error clearing cache {}", cacheName, e);
    }
  }
}
