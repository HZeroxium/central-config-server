package com.example.control.application.command;

import com.example.control.domain.valueobject.id.DriftEventId;
import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.port.repository.DriftEventRepositoryPort;
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
 * Command service for DriftEvent write operations.
 * <p>
 * Handles all write operations (save, update, delete) for DriftEvent domain
 * objects.
 * This service is responsible for:
 * <ul>
 * <li>CRUD operations with validation</li>
 * <li>Cache eviction for write operations</li>
 * <li>Transaction management</li>
 * </ul>
 * <p>
 * Does NOT handle:
 * <ul>
 * <li>Business logic or permission checks (delegated to orchestrator
 * services)</li>
 * <li>Cross-domain operations (use orchestrator services)</li>
 * <li>Read operations (use
 * {@link com.example.control.application.query.DriftEventQueryService})</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class DriftEventCommandService {

    private final DriftEventRepositoryPort repository;
    private final CacheManager cacheManager;

    /**
     * Saves a drift event (create or update).
     * <p>
     * Automatically generates ID if null (new entity).
     * Evicts specific drift-events cache entry by ID.
     *
     * @param event the drift event to save (must be valid)
     * @return the saved drift event with generated/updated fields
     */
    @CacheEvict(value = "drift-events", key = "#event.id")
    public DriftEvent save(@Valid DriftEvent event) {
        log.debug("Saving drift event: {}", event.getId());

        // Generate UUID if ID is null (new event)
        if (event.getId() == null) {
            event.setId(DriftEventId.of(UUID.randomUUID().toString()));
            log.debug("Generated new ID for drift event: {}", event.getId());
        }

        DriftEvent saved = repository.save(event);
        log.info("Saved drift event: {} for service: {}", saved.getId(), saved.getServiceName());
        return saved;
    }

    /**
     * Deletes a drift event by ID.
     * <p>
     * Evicts specific drift-events cache entry by ID.
     *
     * @param id the drift event ID to delete
     */
    @CacheEvict(value = "drift-events", key = "#id")
    public void deleteById(DriftEventId id) {
        log.info("Deleting drift event: {}", id);
        repository.deleteById(id);
    }

    /**
     * Resolves all unresolved drift events for a specific instance.
     * <p>
     * Updates status to RESOLVED and sets resolvedBy/resolvedAt fields.
     * Typically called after verifying instance configuration is up-to-date.
     * <p>
     * Evicts all drift-events cache entries since we don't know which events were
     * affected.
     *
     * @param serviceName the service name
     * @param instanceId  the instance identifier
     * @param resolvedBy  identifier of who/what resolved the drift
     */
    @CacheEvict(value = "drift-events", allEntries = true)
    public void resolveForInstance(String serviceName, String instanceId, String resolvedBy) {
        log.info("Resolving drift events for instance: {}/{}, resolved by: {}",
                serviceName, instanceId, resolvedBy);
        repository.resolveForInstance(serviceName, instanceId, resolvedBy);
    }

    /**
     * Bulk updates teamId for all drift events of a specific service.
     * <p>
     * Used during ownership transfer to propagate new team ownership.
     * Evicts all drift-events cache entries since we don't know which events were
     * affected.
     *
     * @param serviceId the service ID to match
     * @param newTeamId the new team ID to set
     * @return number of drift events updated
     */
    @CacheEvict(value = "drift-events", allEntries = true)
    public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
        log.info("Bulk updating teamId to {} for all drift events of service: {}",
                newTeamId, serviceId);
        long count = repository.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
        log.info("Updated {} drift events for service: {}", count, serviceId);
        return count;
    }

    /**
     * Bulk save drift events.
     * <p>
     * Efficiently saves multiple drift events in a single MongoDB bulk operation.
     * Used for batch heartbeat processing to reduce write overhead.
     * <p>
     * Evicts cache entries for specific event IDs instead of clearing entire cache.
     *
     * @param events list of drift events to save
     * @return bulk write result with counts of inserted/updated documents
     */
    public BulkWriteResult bulkSave(List<DriftEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("Empty events list, skipping bulk save");
            return null;
        }

        log.info("Bulk saving {} drift events", events.size());

        // Generate IDs for events that don't have one
        for (DriftEvent event : events) {
            if (event.getId() == null) {
                event.setId(DriftEventId.of(UUID.randomUUID().toString()));
            }
        }

        BulkWriteResult result = repository.bulkSave(events);

        // Programmatically evict cache entries for specific event IDs
        Cache cache = cacheManager.getCache("drift-events");
        if (cache != null) {
            int evictedCount = 0;
            for (DriftEvent event : events) {
                if (event.getId() != null) {
                    try {
                        cache.evict(event.getId());
                        evictedCount++;
                    } catch (Exception e) {
                        log.warn("Failed to evict cache for drift event: {}", event.getId(), e);
                    }
                }
            }
            log.debug("Evicted {} cache entries for drift events", evictedCount);
        }

        log.info("Bulk save completed: {} inserted, {} modified",
                result != null ? result.getInsertedCount() : 0,
                result != null ? result.getModifiedCount() : 0);
        return result;
    }
}
