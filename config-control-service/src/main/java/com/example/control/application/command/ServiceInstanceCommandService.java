package com.example.control.application.command;

import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.example.control.domain.model.ServiceInstance;
import com.example.control.domain.port.repository.ServiceInstanceRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Command service for ServiceInstance write operations.
 * <p>
 * Handles all write operations (save, update, delete) for ServiceInstance
 * domain objects.
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
 * {@link com.example.control.application.query.ServiceInstanceQueryService})</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class ServiceInstanceCommandService {

    private final ServiceInstanceRepositoryPort repository;

    /**
     * Saves a service instance (create or update).
     * <p>
     * Automatically generates ID if null (new entity).
     * Evicts specific service-instances cache entry by ID.
     *
     * @param instance the service instance to save (must be valid)
     * @return the saved service instance with generated/updated fields
     */
    @CacheEvict(value = "service-instances", key = "#instance.id")
    public ServiceInstance save(@Valid ServiceInstance instance) {
        log.debug("Saving service instance: {}", instance.getId());

        // Generate UUID if ID is null (new instance)
        if (instance.getId() == null) {
            instance.setId(ServiceInstanceId.of(UUID.randomUUID().toString()));
            log.debug("Generated new ID for service instance: {}", instance.getId());
        }

        ServiceInstance saved = repository.save(instance);
        log.info("Saved service instance: {} for service: {}", saved.getId(), saved.getServiceId());
        return saved;
    }

    /**
     * Deletes a service instance by ID.
     * <p>
     * Evicts specific service-instances cache entry by ID.
     *
     * @param id the service instance ID to delete
     */
    @CacheEvict(value = "service-instances", key = "#id")
    public void deleteById(ServiceInstanceId id) {
        log.info("Deleting service instance: {}", id);
        repository.deleteById(id);
    }

    /**
     * Bulk updates teamId for all service instances of a specific service.
     * <p>
     * Used during ownership transfer to propagate new team ownership.
     * Evicts all service-instances cache entries since we don't know which
     * instances were affected.
     *
     * @param serviceId the service ID to match
     * @param newTeamId the new team ID to set
     * @return number of service instances updated
     */
    @CacheEvict(value = "service-instances", allEntries = true)
    public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
        log.info("Bulk updating teamId to {} for all instances of service: {}",
                newTeamId, serviceId);
        long count = repository.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
        log.info("Updated {} service instances for service: {}", count, serviceId);
        return count;
    }
}
