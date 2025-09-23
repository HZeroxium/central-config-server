package com.example.rest.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Two-level cache manager implementation.
 * L1: Local cache (Caffeine) - fast access
 * L2: Distributed cache (Redis) - shared across instances
 */
@Slf4j
@RequiredArgsConstructor
public class TwoLevelCacheManager implements CacheManager {

  private final CacheManager l1CacheManager; // Local cache (Caffeine)
  private final CacheManager l2CacheManager; // Distributed cache (Redis), can be null
  private final CacheProperties.TwoLevelConfig config;

  @Override
  public Cache getCache(String name) {
    Cache l1Cache = l1CacheManager.getCache(name);
    Cache l2Cache = l2CacheManager != null ? l2CacheManager.getCache(name) : null;

    if (l1Cache == null) {
      log.warn("L1 cache not found for name: {}", name);
      return l2Cache; // Fallback to L2 only
    }

    return new TwoLevelCache(name, l1Cache, l2Cache, config);
  }

  @Override
  public Collection<String> getCacheNames() {
    // Return union of cache names from both levels
    Collection<String> l1Names = l1CacheManager.getCacheNames();
    if (l2CacheManager != null) {
      Collection<String> l2Names = l2CacheManager.getCacheNames();
      // In practice, both should have the same cache names
      return l1Names.isEmpty() ? l2Names : l1Names;
    }
    return l1Names;
  }

  /**
   * Two-level cache implementation.
   */
  @Slf4j
  @RequiredArgsConstructor
  public static class TwoLevelCache implements Cache {

    private final String name;
    private final Cache l1Cache; // Local cache (required)
    private final Cache l2Cache; // Distributed cache (optional)
    private final CacheProperties.TwoLevelConfig config;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Object getNativeCache() {
      return this; // Return self as native cache
    }

    @Override
    public ValueWrapper get(Object key) {
      try {
        // Try L1 first (fastest)
        ValueWrapper l1Value = l1Cache.get(key);
        if (l1Value != null) {
          log.debug("Cache hit in L1 for key: {}", key);
          return l1Value;
        }

        // Try L2 if available
        if (l2Cache != null) {
          ValueWrapper l2Value = l2Cache.get(key);
          if (l2Value != null) {
            log.debug("Cache hit in L2 for key: {}, promoting to L1", key);
            // Promote to L1 for faster future access
            l1Cache.put(key, l2Value.get());
            return l2Value;
          }
        }

        log.debug("Cache miss for key: {}", key);
        return null;
      } catch (Exception e) {
        log.error("Error retrieving from cache for key: {}", key, e);
        return null;
      }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
      ValueWrapper wrapper = get(key);
      return wrapper != null ? (T) wrapper.get() : null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
      try {
        // Try to get from cache first
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
          return (T) wrapper.get();
        }

        // Load value and cache it
        T value = valueLoader.call();
        put(key, value);
        return value;
      } catch (Exception e) {
        log.error("Error loading value for key: {}", key, e);
        throw new RuntimeException("Failed to load value", e);
      }
    }

    @Override
    public void put(Object key, Object value) {
      try {
        // Always write to L1
        l1Cache.put(key, value);
        log.debug("Cached value in L1 for key: {}", key);

        // Write to L2 if available and write-through is enabled
        if (l2Cache != null && config.isWriteThrough()) {
          l2Cache.put(key, value);
          log.debug("Cached value in L2 for key: {}", key);
        }
      } catch (Exception e) {
        log.error("Error caching value for key: {}", key, e);
      }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
      try {
        // Check L1 first
        ValueWrapper existing = l1Cache.get(key);
        if (existing != null) {
          return existing;
        }

        // Check L2 if available
        if (l2Cache != null) {
          existing = l2Cache.get(key);
          if (existing != null) {
            // Promote to L1
            l1Cache.put(key, existing.get());
            return existing;
          }

          // Put in L2 if absent
          existing = l2Cache.putIfAbsent(key, value);
          if (existing != null) {
            l1Cache.put(key, existing.get());
            return existing;
          }
        }

        // Put in L1
        l1Cache.put(key, value);
        log.debug("Put if absent: cached value for key: {}", key);
        return null;
      } catch (Exception e) {
        log.error("Error in putIfAbsent for key: {}", key, e);
        return null;
      }
    }

    @Override
    public void evict(Object key) {
      try {
        // Evict from both levels
        l1Cache.evict(key);
        log.debug("Evicted from L1 cache for key: {}", key);

        if (l2Cache != null) {
          l2Cache.evict(key);
          log.debug("Evicted from L2 cache for key: {}", key);
        }
      } catch (Exception e) {
        log.error("Error evicting key: {}", key, e);
      }
    }

    @Override
    public boolean evictIfPresent(Object key) {
      boolean evicted = false;
      try {
        // Evict from L1
        evicted = l1Cache.evictIfPresent(key);

        // Evict from L2 if available
        if (l2Cache != null) {
          evicted = l2Cache.evictIfPresent(key) || evicted;
        }

        if (evicted) {
          log.debug("Evicted from cache for key: {}", key);
        }

        return evicted;
      } catch (Exception e) {
        log.error("Error evicting key: {}", key, e);
        return evicted;
      }
    }

    @Override
    public void clear() {
      try {
        // Clear both levels
        l1Cache.clear();
        log.debug("Cleared L1 cache: {}", name);

        if (l2Cache != null) {
          l2Cache.clear();
          log.debug("Cleared L2 cache: {}", name);
        }
      } catch (Exception e) {
        log.error("Error clearing cache: {}", name, e);
      }
    }

    @Override
    public boolean invalidate() {
      try {
        boolean result = l1Cache.invalidate();

        if (l2Cache != null) {
          result = l2Cache.invalidate() && result;
        }

        log.debug("Invalidated cache: {}", name);
        return result;
      } catch (Exception e) {
        log.error("Error invalidating cache: {}", name, e);
        return false;
      }
    }
  }
}
