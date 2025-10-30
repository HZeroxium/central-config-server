package com.example.control.infrastructure.config.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis pub/sub listener for cache invalidation events.
 * <p>
 * Receives invalidation messages from Redis and invalidates the corresponding
 * entries in the local L1 cache (Caffeine) of the TwoLevelCacheManager.
 * <p>
 * This ensures that when one instance updates the cache, all other instances
 * are notified to invalidate their local cache, maintaining consistency.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationListener implements MessageListener {

  private final CacheManager cacheManager;
  private final ObjectMapper objectMapper;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      CacheInvalidationMessage invalidationMessage = objectMapper.readValue(json, CacheInvalidationMessage.class);

      log.debug("Received cache invalidation: cache={}, key={}, pattern={}",
          invalidationMessage.cacheName(), invalidationMessage.key(), invalidationMessage.pattern());

      Cache cache = cacheManager.getCache(invalidationMessage.cacheName());
      if (cache == null) {
        log.warn("Cache not found for invalidation: {}", invalidationMessage.cacheName());
        return;
      }

      // For TwoLevelCacheManager, evicting from the composite cache will evict L1
      if (invalidationMessage.pattern()) {
        // Pattern-based invalidation - clear all entries (for now)
        // TODO: Implement pattern matching if needed
        log.debug("Pattern-based invalidation for cache: {}, clearing all", invalidationMessage.cacheName());
        cache.clear();
      } else if (invalidationMessage.key() == null) {
        // Clear all entries
        cache.clear();
        log.debug("Cleared all entries in cache: {}", invalidationMessage.cacheName());
      } else {
        // Exact key invalidation
        cache.evict(invalidationMessage.key());
        log.debug("Evicted key: {} from cache: {}", invalidationMessage.key(), invalidationMessage.cacheName());
      }

    } catch (Exception e) {
      log.error("Failed to process cache invalidation message", e);
    }
  }
}
