package com.example.control.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for creating different types of cache managers based on
 * configuration.
 * Supports dynamic switching between cache providers with fallback mechanisms.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheManagerFactory {

  private final CacheProperties cacheProperties;
  private final Optional<RedisConnectionFactory> redisConnectionFactory;

  /**
   * Creates a cache manager based on the configured provider with fallback
   * support.
   */
  public CacheManager createCacheManager() {
    CacheProperties.CacheProvider provider = cacheProperties.getProvider();

    try {
      log.info("Creating cache manager for provider: {}", provider);

      switch (provider) {
        case CAFFEINE:
          return createCaffeineCacheManager();

        case REDIS:
          return createRedisCacheManagerWithFallback();

        case TWO_LEVEL:
          return createTwoLevelCacheManager();

        case NOOP:
          return createNoOpCacheManager();

        default:
          log.warn("Unknown cache provider: {}, falling back to Caffeine", provider);
          return createCaffeineCacheManager();
      }
    } catch (Exception e) {
      log.error("Failed to create cache manager for provider: {}, falling back to NoOp", provider, e);
      if (cacheProperties.isEnableFallback()) {
        return createNoOpCacheManager();
      }
      throw e;
    }
  }

  /**
   * Creates a Caffeine-based cache manager.
   */
  public CacheManager createCaffeineCacheManager() {
    log.info("Creating Caffeine cache manager");

    CacheProperties.CaffeineConfig config = cacheProperties.getCaffeine();

    Caffeine<Object, Object> builder = Caffeine.newBuilder()
        .maximumSize(config.getMaximumSize())
        .expireAfterWrite(config.getExpireAfterWrite())
        .expireAfterAccess(config.getExpireAfterAccess());

    if (config.isRecordStats()) {
      builder.recordStats();
    }

    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(builder);

    // Configure cache names
    manager.setCacheNames(cacheProperties.getCaches().keySet());

    return manager;
  }

  /**
   * Creates a Redis-based cache manager with Caffeine fallback.
   */
  public CacheManager createRedisCacheManagerWithFallback() {
    if (redisConnectionFactory.isEmpty()) {
      log.warn("Redis connection factory not available, falling back to Caffeine");
      if (cacheProperties.isEnableFallback()) {
        return createCaffeineCacheManager();
      }
      throw new IllegalStateException("Redis connection factory not available and fallback disabled");
    }

    log.info("Creating Redis cache manager");

    try {
      return createRedisCacheManager(redisConnectionFactory.get());
    } catch (Exception e) {
      log.error("Failed to create Redis cache manager", e);
      if (cacheProperties.getRedis().isFallbackToCaffeine() && cacheProperties.isEnableFallback()) {
        log.info("Falling back to Caffeine cache manager");
        return createCaffeineCacheManager();
      }
      throw e;
    }
  }

  /**
   * Creates a Redis cache manager.
   */
  private CacheManager createRedisCacheManager(RedisConnectionFactory connectionFactory) {
    CacheProperties.RedisConfig config = cacheProperties.getRedis();

    // Configure JSON serialization
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Object.class)
        .build();
    mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

    // Default config allows null values by default, only disable if explicitly configured
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
        .entryTtl(config.getDefaultTtl());

    // Configure per-cache settings
    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
    cacheProperties.getCaches().forEach((cacheName, cacheConfig) -> {
      RedisCacheConfiguration specificConfig = defaultConfig.entryTtl(cacheConfig.getTtl());
      if (!cacheConfig.isAllowNullValues()) {
        specificConfig = specificConfig.disableCachingNullValues();
      }
      cacheConfigs.put(cacheName, specificConfig);
    });

    RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigs);

    if (config.isEnableStatistics()) {
      builder.enableStatistics();
    }

    if (config.isTransactionAware()) {
      builder.transactionAware();
    }

    return builder.build();
  }

  /**
   * Creates a two-level cache manager (L1: Caffeine, L2: Redis).
   */
  public CacheManager createTwoLevelCacheManager() {
    log.info("Creating two-level cache manager");

    CacheManager l1Cache = createCaffeineCacheManager();
    CacheManager l2Cache = null;

    if (redisConnectionFactory.isPresent()) {
      try {
        l2Cache = createRedisCacheManager(redisConnectionFactory.get());
      } catch (Exception e) {
        log.warn("Failed to create L2 (Redis) cache, using L1 (Caffeine) only", e);
      }
    }

    return new TwoLevelCacheManager(l1Cache, l2Cache, cacheProperties.getTwoLevel());
  }

  /**
   * Creates a no-op cache manager (disables caching).
   */
  public CacheManager createNoOpCacheManager() {
    log.info("Creating NoOp cache manager (caching disabled)");
    return new NoOpCacheManager();
  }

  /**
   * Validates if the specified provider is available.
   */
  public boolean isProviderAvailable(CacheProperties.CacheProvider provider) {
    switch (provider) {
      case CAFFEINE:
      case NOOP:
        return true;

      case REDIS:
      case TWO_LEVEL:
        return redisConnectionFactory.isPresent();

      default:
        return false;
    }
  }
}
