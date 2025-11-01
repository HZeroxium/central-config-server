package com.example.control.infrastructure.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message DTO for cache invalidation events published via Redis pub/sub.
 * <p>
 * Used to notify all cache instances when a cache entry needs to be
 * invalidated,
 * ensuring consistency across the distributed cache topology.
 *
 * @param cacheName the cache name (e.g., "service-instances", "iam-users")
 * @param key       the cache key to invalidate (may be null for pattern-based
 *                  invalidation)
 * @param pattern   if true, key is treated as a pattern; if false, exact key
 *                  match
 * @since 1.0.0
 */
public record CacheInvalidationMessage(
    String cacheName,
    String key,
    boolean pattern) {
  /**
   * Create a message for exact key invalidation.
   */
  @JsonCreator
  public CacheInvalidationMessage(
      @JsonProperty("cacheName") String cacheName,
      @JsonProperty("key") String key,
      @JsonProperty("pattern") boolean pattern) {
    this.cacheName = cacheName;
    this.key = key;
    this.pattern = pattern;
  }

  /**
   * Create a message for exact key invalidation.
   */
  public static CacheInvalidationMessage exactKey(String cacheName, String key) {
    return new CacheInvalidationMessage(cacheName, key, false);
  }

  /**
   * Create a message for pattern-based invalidation.
   */
  public static CacheInvalidationMessage pattern(String cacheName, String keyPattern) {
    return new CacheInvalidationMessage(cacheName, keyPattern, true);
  }

  /**
   * Create a message for clearing all entries in a cache.
   */
  public static CacheInvalidationMessage clearAll(String cacheName) {
    return new CacheInvalidationMessage(cacheName, null, false);
  }
}
