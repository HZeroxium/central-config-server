package com.example.user.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"no-cache", "default"})
@EnableCaching
public class NoCacheConfig {

    @Bean
    public CacheManager noopCacheManager() {
        return new NoOpCacheManager();
    }
}


