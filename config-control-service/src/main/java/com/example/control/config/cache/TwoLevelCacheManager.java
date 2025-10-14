package com.example.control.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Two-level {@link CacheManager} implementation that composes an L1 and an L2 cache manager.
 * <p>
 * <strong>Topology:</strong>
 * <ul>
 *   <li><b>L1</b>: Local in-memory cache (e.g., Caffeine) for ultra-low latency lookups.</li>
 *   <li><b>L2</b>: Distributed cache (e.g., Redis) shared across application instances.</li>
 * </ul>
 * <p>
 * <strong>Behavior:</strong>
 * <ul>
 *   <li>{@link #getCache(String)} returns a composite {@link Cache} that first attempts L1,
 *   falls back to L2 on miss, and optionally promotes values from L2 to L1 for faster subsequent access.</li>
 *   <li>Writes go to L1 and—if configured via {@link CacheProperties.TwoLevelConfig#isWriteThrough()}—also to L2.</li>
 * </ul>
 * <p>
 * This manager does <em>not</em> change semantics of Spring's caching abstraction; it only orchestrates
 * the delegation to two underlying managers. For method-level semantics, see Spring's {@link Cache}
 * and {@link CacheManager} contracts.
 *
 * @see org.springframework.cache.Cache
 * @see org.springframework.cache.CacheManager
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class TwoLevelCacheManager implements CacheManager {

  /**
   * L1 (local, in-memory) cache manager, typically backed by Caffeine.
   * This manager is expected to be non-null and provide the primary fast path.
   */
  private final CacheManager l1CacheManager; // Local cache (Caffeine)

  /**
   * L2 (distributed) cache manager, typically backed by Redis. May be {@code null} when an L2
   * tier is not configured or intentionally disabled.
   */
  private final CacheManager l2CacheManager; // Distributed cache (Redis), can be null

  /**
   * Two-level cache configuration that defines behaviors such as write-through
   * and L1 invalidation on L2 updates.
   */
  private final CacheProperties.TwoLevelConfig config;

  /**
   * Obtain a composite {@link Cache} by its logical name.
   * <p>
   * The resulting cache instance will:
   * <ol>
   *   <li>Attempt to read from L1 first.</li>
   *   <li>On L1 miss, attempt to read from L2 (if available); on L2 hit, promote the value to L1.</li>
   *   <li>On writes, always write to L1; optionally write to L2 when {@code writeThrough} is enabled.</li>
   * </ol>
   * If the L1 cache cannot be resolved, the method logs a warning and returns the L2 cache as the fallback.
   *
   * @param name the cache name
   * @return a composite two-level {@link Cache} when L1 is available; otherwise the L2 cache (may be {@code null})
   */
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

  /**
   * Return the set of cache names known to this manager.
   * <p>
   * <strong>Note:</strong> This implementation returns L1 names when present; otherwise it returns L2 names
   * if available. It does <em>not</em> compute a strict union. In typical deployments, both managers are
   * initialized with the same cache names. For a strict union, consider merging the two name collections.
   *
   * @return the collection of cache names as reported by the underlying managers
   * @see CacheManager#getCacheNames()
   */
  @Override
  public Collection<String> getCacheNames() {
    Set<String> names = new LinkedHashSet<>(l1CacheManager.getCacheNames());
    if (l2CacheManager != null) {
      names.addAll(l2CacheManager.getCacheNames());
    }
    return Collections.unmodifiableSet(names);
  }

  /**
   * Two-level {@link Cache} that orchestrates L1 (local) and L2 (distributed) tiers.
   * <p>
   * <strong>Read path:</strong> try L1 → on miss try L2 → on L2 hit, promote to L1. <br/>
   * <strong>Write path:</strong> write to L1; if {@code writeThrough} is enabled and L2 is present, also write to L2.
   * <p>
   * Method semantics follow Spring's {@link Cache} contract, including:
   * <ul>
   *   <li>{@link #get(Object)} and {@link #get(Object, Class)} read semantics and type checks,</li>
   *   <li>{@link #get(Object, Callable)} single-load semantics with error propagation via {@code RuntimeException},</li>
   *   <li>{@link #put(Object, Object)}, {@link #putIfAbsent(Object, Object)},</li>
   *   <li>{@link #evict(Object)}, {@link #evictIfPresent(Object)}, {@link #clear()}, {@link #invalidate()}.</li>
   * </ul>
   * See the official Spring Javadoc for details of each method's contract (e.g., {@code evictIfPresent} vs {@code evict},
   * immediate visibility guarantees of {@code invalidate}, etc.). 
   */
  @Slf4j
  @RequiredArgsConstructor
  public static class TwoLevelCache implements Cache {

    /** Logical cache name shared by L1/L2. */
    private final String name;

    /** L1 cache (required, local in-memory). */
    private final Cache l1Cache; // Local cache (required)

    /** L2 cache (optional, distributed). */
    private final Cache l2Cache; // Distributed cache (optional)

    /** Two-level configuration controlling write-through and L1 invalidation policy. */
    private final CacheProperties.TwoLevelConfig config;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return name;
    }

    /**
     * Return a handle to the underlying native cache provider.
     * <p>
     * In a composite cache, there is no single native provider; this method returns
     * the composite instance itself. Client code should not rely on provider-specific
     * types when interacting with two-level caches.
     *
     * @return this composite cache instance as the "native" cache
     * @see Cache#getNativeCache()
     */
    @Override
    public Object getNativeCache() {
      return this; // Return self as native cache
    }

    /**
     * Retrieve a value for the given {@code key}, consulting L1 first and then L2 if necessary.
     * <p>
     * On an L2 hit, the value is promoted to L1 to accelerate subsequent reads.
     * Any exception from the underlying caches is caught and logged; the method returns {@code null}
     * in such cases to preserve a non-failing read path.
     *
     * @param key the cache key
     * @return a {@link ValueWrapper} holding the value (which may itself be {@code null}),
     *         or {@code null} if not found at either level
     * @see Cache#get(Object)
     */
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

    /**
     * Type-safe variant of {@link #get(Object)}.
     * <p>
     * Delegates to {@link #get(Object)} and performs a runtime type check in accordance
     * with Spring's {@link Cache} contract; an {@link IllegalStateException} is thrown when
     * a cached value is present but not assignable to {@code type}.
     *
     * @param key the cache key
     * @param type the expected return type (may be {@code null} to bypass a type check)
     * @param <T> inferred result type
     * @return the cached value cast to {@code T}, or {@code null} if not found
     * @throws IllegalStateException if a value is present but not of the required type
     */
    @Override
    public <T> T get(Object key, Class<T> type) {
      ValueWrapper wrapper = get(key);
      if (wrapper == null) {
        return null;
      }
      Object value = wrapper.get();
      if (type != null && value != null && !type.isInstance(value)) {
        throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value.getClass());
      }
      @SuppressWarnings("unchecked")
      T result = (T) value;
      return result;
    }

    /**
     * Retrieve a value for {@code key}, computing and caching it if absent.
     * <p>
     * Follows the standard "if cached, return; otherwise load, cache and return" pattern.
     * Any exception thrown by the provided loader is wrapped in a {@link RuntimeException} here.
     * (Spring's default contract uses {@code Cache.ValueRetrievalException}.)
     *
     * @param key the cache key
     * @param valueLoader a value supplier invoked when the key is not present
     * @param <T> inferred result type
     * @return the resolved (existing or newly computed) value
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
      try {
        // Try to get from cache first
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
          Object value = wrapper.get();
          @SuppressWarnings("unchecked")
          T result = (T) value;
          return result;
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

    /**
     * Put the given {@code value} into the cache for {@code key}.
     * <p>
     * Always writes to L1. If L2 is present and write-through is enabled via configuration,
     * also writes to L2 to keep tiers aligned.
     *
     * @param key the cache key
     * @param value the value to cache (may be {@code null} depending on cache configuration)
     */
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

    /**
     * Atomically associate the given value with the key if not already present.
     * <p>
     * Checks L1, then L2 (if available). If present in L2, promotes to L1. Otherwise,
     * inserts a value (including into L2 via {@code putIfAbsent}) and finally ensures L1
     * contains the value as a fast path. Errors are logged and {@code null} is returned
     * according to the Spring {@link Cache} contract. 
     *
     * @param key the key
     * @param value the candidate value to associate
     * @return a {@link ValueWrapper} containing the existing value if one was present; {@code null} otherwise
     */
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

    /**
     * Evict the entry mapped by {@code key} from both L1 and L2 (if present).
     * <p>
     * This method follows the best-effort semantics of Spring's {@link Cache#evict(Object)}. 
     *
     * @param key the key to evict
     */
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

    /**
     * Evict the entry mapped by {@code key} from both L1 and L2 (if present), returning
     * whether a mapping was known to be present beforehand.
     * <p>
     * For detailed semantics and immediate-visibility expectations, see {@link Cache#evictIfPresent(Object)}.
     *
     * @param key the key to evict
     * @return {@code true} if an entry was known to be present before eviction; {@code false} otherwise
     */
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

    /**
     * Clear the cache on both tiers.
     * <p>
     * Note that {@link #clear()} may be asynchronous or deferred depending on the underlying provider;
     * for guaranteed immediate visibility of removal, see {@link #invalidate()}. 
     */
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

    /**
     * Invalidate the cache on both tiers with immediate post-conditions as defined by Spring:
     * entries should be immediately invisible to subsequent lookups.
     * <p>
     * Returns {@code true} if both tiers report success (or the absent tier is considered a no-op).
     * See {@link Cache#invalidate()} for details.
     *
     * @return {@code true} if the cache was invalidated successfully; {@code false} otherwise
     */
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
