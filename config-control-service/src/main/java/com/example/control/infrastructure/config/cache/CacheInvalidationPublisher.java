package com.example.control.infrastructure.config.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Publisher for cache invalidation events via Redis pub/sub.
 * <p>
 * Publishes invalidation messages to Redis channel so that all instances
 * can invalidate their local L1 cache (Caffeine) when L2 cache (Redis) is
 * updated.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class CacheInvalidationPublisher {

  private static final String CHANNEL = "cache:invalidation";

  private final Optional<RedisTemplate<String, String>> redisTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Constructor that accepts nullable RedisTemplate (for environments without
   * Redis).
   */
  public CacheInvalidationPublisher(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = Optional.ofNullable(redisTemplate);
    this.objectMapper = objectMapper;
  }

  /**
   * Publish an invalidation message to Redis pub/sub.
   *
   * @param message the invalidation message
   */
  public void publish(CacheInvalidationMessage message) {
    if (redisTemplate.isEmpty()) {
      log.debug("Redis template not available, skipping cache invalidation publication");
      return;
    }

    try {
      String json = objectMapper.writeValueAsString(message);
      redisTemplate.get().convertAndSend(CHANNEL, json);
      log.debug("Published cache invalidation: cache={}, key={}, pattern={}",
          message.cacheName(), message.key(), message.pattern());
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize cache invalidation message: {}", message, e);
    } catch (Exception e) {
      log.error("Failed to publish cache invalidation message: {}", message, e);
    }
  }

  /**
   * Publish invalidation for a specific cache key.
   */
  public void invalidate(String cacheName, String key) {
    publish(CacheInvalidationMessage.exactKey(cacheName, key));
  }

  /**
   * Publish invalidation for a cache pattern.
   */
  public void invalidatePattern(String cacheName, String keyPattern) {
    publish(CacheInvalidationMessage.pattern(cacheName, keyPattern));
  }

  /**
   * Publish invalidation to clear all entries in a cache.
   */
  public void clearAll(String cacheName) {
    publish(CacheInvalidationMessage.clearAll(cacheName));
  }
}
