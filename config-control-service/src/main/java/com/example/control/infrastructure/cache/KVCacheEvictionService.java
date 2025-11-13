package com.example.control.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Service for programmatic cache eviction for KV operations.
 * <p>
 * Handles eviction of specific keys and their parent prefixes to ensure
 * list operations return fresh data after writes.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KVCacheEvictionService {

    private static final String CACHE_NAME = "kv-entries";

    private final CacheManager cacheManager;

    /**
     * Evict a specific key and all its parent prefixes.
     * <p>
     * For example, evicting {@code config/db/url} will also evict:
     * - {@code config/db}
     * - {@code config}
     * - root (for list operations)
     * </p>
     *
     * @param serviceId the service ID
     * @param path      the relative path
     */
    public void evictKeyAndParents(String serviceId, String path) {
        if (serviceId == null || serviceId.isBlank()) {
            log.warn("Cannot evict cache: serviceId is null or blank");
            return;
        }

        List<String> keysToEvict = KVCacheKeyGenerator.generateParentPrefixKeys(serviceId, path);
        evictKeys(keysToEvict);

        // Also evict list operation keys for parent prefixes
        List<String> prefixKeys = KVCacheKeyGenerator.generatePrefixEvictionKeys(serviceId, path);
        evictKeys(prefixKeys);

        log.debug("Evicted cache for service: {}, path: {} ({} keys)", serviceId, path, keysToEvict.size() + prefixKeys.size());
    }

    /**
     * Evict all keys under a prefix (for recursive operations).
     * <p>
     * This is a best-effort operation. Since we can't enumerate all cached keys
     * efficiently, we evict known patterns (list operations, view operations).
     * </p>
     *
     * @param serviceId the service ID
     * @param prefix    the relative prefix
     */
    public void evictPrefix(String serviceId, String prefix) {
        if (serviceId == null || serviceId.isBlank()) {
            log.warn("Cannot evict cache: serviceId is null or blank");
            return;
        }

        // Evict list and view operations for this prefix and all parents
        List<String> keysToEvict = KVCacheKeyGenerator.generatePrefixEvictionKeys(serviceId, prefix);
        evictKeys(keysToEvict);

        log.debug("Evicted cache for service: {}, prefix: {} ({} keys)", serviceId, prefix, keysToEvict.size());
    }

    /**
     * Evict multiple specific cache keys.
     *
     * @param keys the cache keys to evict
     */
    public void evictKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.warn("Cache '{}' not found, skipping eviction", CACHE_NAME);
            return;
        }

        int evictedCount = 0;
        for (String key : keys) {
            try {
                cache.evict(key);
                evictedCount++;
            } catch (Exception e) {
                log.warn("Failed to evict cache key: {}", key, e);
            }
        }

        if (evictedCount > 0) {
            log.debug("Evicted {} cache keys from '{}'", evictedCount, CACHE_NAME);
        }
    }

    /**
     * Evict keys from absolute keys by extracting serviceId and relative paths.
     * <p>
     * Useful for transaction operations where we have absolute keys.
     * </p>
     *
     * @param absoluteKeys list of absolute keys
     */
    public void evictKeysFromAbsolute(List<String> absoluteKeys) {
        if (absoluteKeys == null || absoluteKeys.isEmpty()) {
            return;
        }

        Set<String> keysToEvict = absoluteKeys.stream()
                .map(absoluteKey -> {
                    // Extract serviceId and relative path
                    String relativePath = extractRelativePathFromAbsolute(absoluteKey);
                    if (relativePath == null) {
                        return null;
                    }

                    // Try to extract serviceId from absolute key
                    String serviceId = extractServiceIdFromAbsolute(absoluteKey);
                    if (serviceId == null) {
                        return null;
                    }

                    // Generate all parent prefix keys
                    return KVCacheKeyGenerator.generateParentPrefixKeys(serviceId, relativePath);
                })
                .filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .collect(java.util.stream.Collectors.toSet());

        evictKeys(keysToEvict.stream().toList());
    }

    /**
     * Extract serviceId from absolute key.
     * <p>
     * Pattern: {@code apps/{serviceId}/kv/...}
     * </p>
     *
     * @param absoluteKey the absolute key
     * @return serviceId, or null if extraction fails
     */
    private String extractServiceIdFromAbsolute(String absoluteKey) {
        if (absoluteKey == null || !absoluteKey.startsWith("apps/")) {
            return null;
        }

        String remaining = absoluteKey.substring(5); // Remove "apps/"
        int firstSlash = remaining.indexOf('/');
        if (firstSlash < 0) {
            return null;
        }

        return remaining.substring(0, firstSlash);
    }

    /**
     * Extract relative path from absolute key.
     * <p>
     * Pattern: {@code apps/{serviceId}/kv/{relativePath}}
     * </p>
     *
     * @param absoluteKey the absolute key
     * @return relative path, or null if extraction fails
     */
    private String extractRelativePathFromAbsolute(String absoluteKey) {
        if (absoluteKey == null || !absoluteKey.startsWith("apps/")) {
            return null;
        }

        String remaining = absoluteKey.substring(5); // Remove "apps/"
        int firstSlash = remaining.indexOf('/');
        if (firstSlash < 0) {
            return null;
        }

        // Skip serviceId (we don't need to validate it here)
        String kvPart = remaining.substring(firstSlash + 1);

        if (!kvPart.startsWith("kv/")) {
            return null;
        }

        return kvPart.substring(3); // Remove "kv/"
    }
}

