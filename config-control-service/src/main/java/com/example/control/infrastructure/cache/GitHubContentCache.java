package com.example.control.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Cache service for GitHub file content.
 * <p>
 * Provides caching for GitHub file content to reduce API calls and improve performance.
 * Uses Spring Cache abstraction with Caffeine backend.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubContentCache {

    private static final String CACHE_NAME = "github-content";

    private final CacheManager cacheManager;

    /**
     * Get cached file content.
     *
     * @param path the file path (cache key)
     * @return cached content, or null if not cached
     */
    public String get(String path) {
        Cache cache = getCache();
        if (cache == null) {
            return null;
        }

        Cache.ValueWrapper wrapper = cache.get(path);
        if (wrapper != null) {
            log.debug("Cache hit for GitHub content: {}", path);
            return (String) wrapper.get();
        }

        log.debug("Cache miss for GitHub content: {}", path);
        return null;
    }

    /**
     * Put file content into cache.
     *
     * @param path    the file path (cache key)
     * @param content the file content to cache
     */
    public void put(String path, String content) {
        Cache cache = getCache();
        if (cache != null) {
            cache.put(path, content);
            log.debug("Cached GitHub content: {}", path);
        }
    }

    /**
     * Evict cached file content.
     *
     * @param path the file path to evict
     */
    public void evict(String path) {
        Cache cache = getCache();
        if (cache != null) {
            cache.evict(path);
            log.debug("Evicted GitHub content from cache: {}", path);
        }
    }

    /**
     * Clear all cached GitHub content.
     */
    public void clear() {
        Cache cache = getCache();
        if (cache != null) {
            cache.clear();
            log.info("Cleared all GitHub content cache");
        }
    }

    /**
     * Get the cache instance.
     *
     * @return cache instance, or null if not available
     */
    private Cache getCache() {
        return cacheManager.getCache(CACHE_NAME);
    }
}

