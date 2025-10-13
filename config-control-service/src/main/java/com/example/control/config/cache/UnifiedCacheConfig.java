package com.example.control.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Arrays;
import java.util.Optional;

/**
 * Unified cache configuration that supports dynamic provider selection.
 * This replaces the need for multiple profile-based configurations.
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
public class UnifiedCacheConfig {

  private final CacheProperties cacheProperties;
  private final Optional<RedisConnectionFactory> redisConnectionFactory;

  /**
   * Primary cache manager based on configuration.
   * This will be used when no specific cache manager is requested.
   * Returns a DelegatingCacheManager that supports runtime provider switching.
   */
  @Bean("delegatingCacheManager")
  @Primary
  public DelegatingCacheManager delegatingCacheManager() {
    CacheManagerFactory factory = new CacheManagerFactory(cacheProperties, redisConnectionFactory);
    CacheManager initialManager = factory.createCacheManager();

    log.info("Initialized DelegatingCacheManager with provider: {}",
        cacheProperties.getProvider());

    return new DelegatingCacheManager(initialManager);
  }

  /**
   * Cache manager factory for dynamic creation and runtime switching.
   */
  @Bean
  public CacheManagerFactory cacheManagerFactory() {
    return new CacheManagerFactory(cacheProperties, redisConnectionFactory);
  }

  /**
   * Custom key generator for consistent cache key generation.
   */
  @Bean
  public KeyGenerator keyGenerator() {
    return (target, method, params) -> {
      StringBuilder keyBuilder = new StringBuilder();
      keyBuilder.append(target.getClass().getSimpleName())
          .append(":")
          .append(method.getName())
          .append(":v1:");

      if (params.length > 0) {
        keyBuilder.append(Arrays.deepToString(params));
      }

      String key = keyBuilder.toString();
      log.debug("Generated cache key: {}", key);
      return key;
    };
  }

  /**
   * Cache health indicator for monitoring cache provider availability.
   */
  @Bean
  public CacheHealthIndicator cacheHealthIndicator(CacheManagerFactory cacheManagerFactory, 
                                                   @org.springframework.beans.factory.annotation.Qualifier("delegatingCacheManager") DelegatingCacheManager delegatingCacheManager) {
    return new CacheHealthIndicator(cacheManagerFactory, cacheProperties, delegatingCacheManager);
  }
}
