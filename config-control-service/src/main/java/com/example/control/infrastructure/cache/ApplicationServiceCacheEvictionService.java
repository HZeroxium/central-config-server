package com.example.control.infrastructure.cache;

import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Helper service for evicting ApplicationService-related cache entries.
 * <p>
 * Provides methods to evict findAll caches, displayName lookup caches, and individual service caches.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceCacheEvictionService {

    private static final String CACHE_NAME = "application-services";

    private final CacheManager cacheManager;

    /**
     * Evicts cache entries for a single service update.
     * <p>
     * Evicts:
     * <ul>
     * <li>Individual service cache entry</li>
     * <li>DisplayName lookup cache (if displayName changed)</li>
     * <li>All findAll caches (since criteria may include this service)</li>
     * </ul>
     *
     * @param serviceId   the service ID to evict
     * @param displayName the display name (to evict displayName cache)
     */
    public void evictForServiceUpdate(ApplicationServiceId serviceId, String displayName) {
        Cache cache = getCache();
        if (cache == null) {
            return;
        }

        try {
            // Evict individual service
            cache.evict(serviceId);
            log.debug("Evicted cache for service: {}", serviceId);

            // Evict displayName lookup cache
            if (displayName != null && !displayName.isBlank()) {
                String displayNameKey = "displayName:" + displayName;
                cache.evict(displayNameKey);
                log.debug("Evicted displayName cache for: {}", displayName);
            }

            // Evict all findAll caches
            cache.clear();
            log.debug("Cleared all application-services cache entries for service update");
        } catch (Exception e) {
            log.warn("Failed to evict cache for service: {}", serviceId, e);
        }
    }

    /**
     * Evicts cache entries for a single service deletion.
     * <p>
     * Evicts:
     * <ul>
     * <li>Individual service cache entry</li>
     * <li>DisplayName lookup cache</li>
     * <li>All findAll caches (since service is removed from results)</li>
     * </ul>
     *
     * @param serviceId   the service ID to evict
     * @param displayName the display name (to evict displayName cache)
     */
    public void evictForServiceDelete(ApplicationServiceId serviceId, String displayName) {
        Cache cache = getCache();
        if (cache == null) {
            return;
        }

        try {
            // Evict individual service
            cache.evict(serviceId);
            log.debug("Evicted cache for deleted service: {}", serviceId);

            // Evict displayName lookup cache
            if (displayName != null && !displayName.isBlank()) {
                String displayNameKey = "displayName:" + displayName;
                cache.evict(displayNameKey);
                log.debug("Evicted displayName cache for deleted service: {}", displayName);
            }

            // Evict all findAll caches
            cache.clear();
            log.debug("Cleared all application-services cache entries for service deletion");
        } catch (Exception e) {
            log.warn("Failed to evict cache for deleted service: {}", serviceId, e);
        }
    }

    /**
     * Evicts cache entries for batch operations.
     * <p>
     * Evicts individual service IDs and displayName caches, then clears findAll caches.
     *
     * @param services collection of services that were updated
     */
    public void evictForBatchUpdate(Collection<ApplicationService> services) {
        if (services == null || services.isEmpty()) {
            return;
        }

        Cache cache = getCache();
        if (cache == null) {
            return;
        }

        try {
            int evictedCount = 0;
            int displayNameEvictedCount = 0;

            // Evict individual services and displayName caches
            for (ApplicationService service : services) {
                if (service.getId() != null) {
                    cache.evict(service.getId());
                    evictedCount++;
                }

                // Evict displayName cache
                String displayName = service.getDisplayName();
                if (displayName != null && !displayName.isBlank()) {
                    String displayNameKey = "displayName:" + displayName;
                    cache.evict(displayNameKey);
                    displayNameEvictedCount++;
                }
            }

            // Clear findAll caches
            cache.clear();
            log.debug("Evicted {} services, {} displayName caches, and cleared findAll caches",
                    evictedCount, displayNameEvictedCount);
        } catch (Exception e) {
            log.warn("Failed to evict cache for batch update (size: {})", services.size(), e);
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
            log.debug("Cleared all application-services cache entries. Reason: {}", reason);
        } catch (Exception e) {
            log.warn("Failed to clear application-services cache. Reason: {}", reason, e);
        }
    }

    /**
     * Gets the application-services cache from cache manager.
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

