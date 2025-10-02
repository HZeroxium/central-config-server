package com.example.control.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration providing a NoOp CacheManager when caching is disabled.
 */
@Configuration
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    return new NoOpCacheManager();
  }
}
