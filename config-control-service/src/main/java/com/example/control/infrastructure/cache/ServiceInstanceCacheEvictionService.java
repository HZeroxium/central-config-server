package com.example.control.infrastructure.cache;

import com.example.control.domain.model.ServiceInstance;
import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.example.control.infrastructure.config.cache.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Helper service for evicting ServiceInstance-related cache entries.
 * <p>
 * Provides methods to evict findAll caches, count caches, and individual instance
 * caches based on different scenarios (single updates, batch operations, status changes).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceInstanceCacheEvictionService {

    private static final String CACHE_NAME = "service-instances";

    private final CacheManager cacheManager;
    private final CacheProperties cacheProperties;

    /**
     * Evicts cache entries for a single instance update.
     * <p>
     * Evicts:
     * <ul>
     * <li>Individual instance cache entry</li>
     * <li>All findAll caches (since criteria may include this instance)</li>
     * <li>All count caches (since counts may change)</li>
     * </ul>
     *
     * @param instanceId the instance ID to evict
     */
    public void evictForInstanceUpdate(ServiceInstanceId instanceId) {
        Cache cache = getCache();
        if (cache == null) {
            return;
        }

        try {
            // Evict individual instance
            cache.evict(instanceId);
            log.debug("Evicted cache for instance: {}", instanceId);

            // Evict all findAll and count caches by clearing entire cache
            // This is safe for single updates as cache will be repopulated on next query
            cache.clear();
            log.debug("Cleared all service-instances cache entries for instance update");
        } catch (Exception e) {
            log.warn("Failed to evict cache for instance: {}", instanceId, e);
        }
    }

    /**
     * Evicts cache entries for a single instance deletion.
     * <p>
     * Evicts:
     * <ul>
     * <li>Individual instance cache entry</li>
     * <li>All findAll caches (since instance is removed from results)</li>
     * <li>All count caches (since count decreases)</li>
     * </ul>
     *
     * @param instanceId the instance ID to evict
     */
    public void evictForInstanceDelete(ServiceInstanceId instanceId) {
        Cache cache = getCache();
        if (cache == null) {
            return;
        }

        try {
            // Evict individual instance
            cache.evict(instanceId);
            log.debug("Evicted cache for deleted instance: {}", instanceId);

            // Evict all findAll and count caches
            cache.clear();
            log.debug("Cleared all service-instances cache entries for instance deletion");
        } catch (Exception e) {
            log.warn("Failed to evict cache for deleted instance: {}", instanceId, e);
        }
    }

    /**
     * Evicts cache entries for batch operations.
     * <p>
     * Uses threshold-based strategy:
     * <ul>
     * <li>If batch size > threshold: Clear entire cache</li>
     * <li>If batch size <= threshold: Evict individual instances + clear findAll/count caches</li>
     * </ul>
     *
     * @param instances collection of instances that were updated
     */
    public void evictForBatchUpdate(Collection<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return;
        }

        Cache cache = getCache();
        if (cache == null) {
            return;
        }

        int batchSize = instances.size();
        int threshold = cacheProperties.getEviction().getBatchThreshold();

        try {
            if (batchSize > threshold) {
                // Large batch: clear entire cache for better performance
                cache.clear();
                log.debug("Cleared entire service-instances cache (batch size {} > threshold {})",
                        batchSize, threshold);
            } else {
                // Small batch: evict individual instances, then clear findAll/count caches
                int evictedCount = 0;
                for (ServiceInstance instance : instances) {
                    if (instance.getId() != null) {
                        cache.evict(instance.getId());
                        evictedCount++;
                    }
                }
                // Clear findAll and count caches
                cache.clear();
                log.debug("Evicted {} individual instances and cleared findAll/count caches (batch size {} <= threshold {})",
                        evictedCount, batchSize, threshold);
            }
        } catch (Exception e) {
            log.warn("Failed to evict cache for batch update (size: {})", batchSize, e);
        }
    }

    /**
     * Evicts cache entries when instance status changes.
     * <p>
     * Status changes affect findAll queries with status filters, so we evict:
     * <ul>
     * <li>Individual instance cache</li>
     * <li>All findAll caches (status-filtered queries may include/exclude this instance)</li>
     * <li>All count caches with status criteria</li>
     * </ul>
     *
     * @param instanceId the instance ID whose status changed
     */
    public void evictForStatusChange(ServiceInstanceId instanceId) {
        Cache cache = getCache();
        if (cache == null) {
            return;
        }

        try {
            // Evict individual instance
            cache.evict(instanceId);
            log.debug("Evicted cache for status change: {}", instanceId);

            // Clear all findAll and count caches since status filters may be affected
            cache.clear();
            log.debug("Cleared all service-instances cache entries for status change");
        } catch (Exception e) {
            log.warn("Failed to evict cache for status change: {}", instanceId, e);
        }
    }

    /**
     * Evicts all cache entries (for bulk operations or when precise eviction is not feasible).
     *
     * @param reason reason for clearing cache (for logging)
     */
    public void evictAll(String reason) {
        Cache cache = getCache();
        if (cache == null) {
            return;
        }

        try {
            cache.clear();
            log.debug("Cleared all service-instances cache entries. Reason: {}", reason);
        } catch (Exception e) {
            log.warn("Failed to clear service-instances cache. Reason: {}", reason, e);
        }
    }

    /**
     * Gets the service-instances cache from cache manager.
     *
     * @return the cache, or null if not found
     */
    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.warn("Cache '{}' not found in CacheManager", CACHE_NAME);
        }
        return cache;
    }
}

