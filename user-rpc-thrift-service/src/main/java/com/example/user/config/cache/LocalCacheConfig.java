package com.example.user.config.cache;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;

@Configuration
@Profile("local-cache")
@EnableCaching
public class LocalCacheConfig {

    @Bean
    public CacheManager caffeineCacheManager() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats();
        CaffeineCacheManager manager = new CaffeineCacheManager("userById", "usersByCriteria", "countByCriteria");
        manager.setCaffeine(builder);
        return manager;
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, params) ->
            target.getClass().getSimpleName() + ":" + method.getName() + ":v1:" + Arrays.deepToString(params);
    }
}


