package com.example.control.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;

/**
 * Wrapper around {@link RedisCacheManager} that wraps all {@link RedisCache} instances
 * with {@link ResilientRedisCache} to handle deserialization errors gracefully.
 * <p>
 * This ensures that corrupted cache entries are automatically evicted and don't break
 * the application.
 * </p>
 */
@Slf4j
public class ResilientRedisCacheManager implements CacheManager {

    private final RedisCacheManager delegate;

    public ResilientRedisCacheManager(RedisCacheManager delegate) {
        this.delegate = delegate;
        log.info("Created ResilientRedisCacheManager wrapping RedisCacheManager");
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        if (cache == null) {
            return null;
        }
        // Wrap RedisCache instances with ResilientRedisCache
        if (cache instanceof RedisCache) {
            return new ResilientRedisCache((RedisCache) cache);
        }
        // For non-RedisCache instances (shouldn't happen, but be safe)
        return cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}

