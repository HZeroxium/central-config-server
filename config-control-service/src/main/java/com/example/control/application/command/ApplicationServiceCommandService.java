package com.example.control.application.command;

import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.port.repository.ApplicationServiceRepositoryPort;
import com.mongodb.bulk.BulkWriteResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

/**
 * Command service for ApplicationService write operations.
 * <p>
 * Handles all write operations (save, update, delete) for ApplicationService
 * domain objects.
 * Responsible for CRUD, cache eviction, and transaction management.
 * Does NOT handle business logic, permission checks, or cross-domain
 * operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class ApplicationServiceCommandService {

    private final ApplicationServiceRepositoryPort repository;
    private final CacheManager cacheManager;

    /**
     * Saves an application service (create or update).
     * Automatically generates ID if null.
     * Evicts specific application-services cache entry by ID.
     *
     * @param service the application service to save
     * @return the saved application service
     */
    @CacheEvict(value = "application-services", key = "#service.id")
    public ApplicationService save(@Valid ApplicationService service) {
        log.debug("Saving application service: {}", service.getId());

        if (service.getId() == null) {
            service.setId(ApplicationServiceId.of(UUID.randomUUID().toString()));
            log.debug("Generated new ID for application service: {}", service.getId());
        }

        ApplicationService saved = repository.save(service);
        log.info("Saved application service: {} (displayName: {})", saved.getId(), saved.getDisplayName());
        return saved;
    }

    /**
     * Deletes an application service by ID.
     * Evicts specific application-services cache entry by ID.
     *
     * @param id the application service ID to delete
     */
    @CacheEvict(value = "application-services", key = "#id")
    public void deleteById(ApplicationServiceId id) {
        log.info("Deleting application service: {}", id);
        repository.deleteById(id);
    }

    /**
     * Bulk save application services.
     * <p>
     * Efficiently saves multiple application services in a single MongoDB bulk operation.
     * Used for batch heartbeat processing to reduce write overhead.
     * <p>
     * Evicts cache entries for specific service IDs instead of clearing entire cache.
     *
     * @param services list of application services to save
     * @return bulk write result with counts of inserted/updated documents
     */
    public BulkWriteResult bulkSave(List<ApplicationService> services) {
        if (services == null || services.isEmpty()) {
            log.debug("Empty services list, skipping bulk save");
            return null;
        }

        log.info("Bulk saving {} application services", services.size());

        // Generate IDs for services that don't have one
        for (ApplicationService service : services) {
            if (service.getId() == null) {
                service.setId(ApplicationServiceId.of(UUID.randomUUID().toString()));
            }
        }

        BulkWriteResult result = repository.bulkSave(services);

        // Programmatically evict cache entries for specific service IDs
        Cache cache = cacheManager.getCache("application-services");
        if (cache != null) {
            int evictedCount = 0;
            for (ApplicationService service : services) {
                if (service.getId() != null) {
                    try {
                        cache.evict(service.getId());
                        evictedCount++;
                    } catch (Exception e) {
                        log.warn("Failed to evict cache for application service: {}", service.getId(), e);
                    }
                }
            }
            log.debug("Evicted {} cache entries for application services", evictedCount);
        }

        log.info("Bulk save completed: {} inserted, {} modified",
                result != null ? result.getInsertedCount() : 0,
                result != null ? result.getModifiedCount() : 0);
        return result;
    }
}
