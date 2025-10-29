package com.example.control.application.command;

import com.example.control.domain.id.DriftEventId;
import com.example.control.domain.object.DriftEvent;
import com.example.control.domain.port.DriftEventRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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

  /**
   * Saves a drift event (create or update).
   * <p>
   * Automatically generates ID if null (new entity).
   * Evicts all drift-events cache entries.
   *
   * @param event the drift event to save (must be valid)
   * @return the saved drift event with generated/updated fields
   */
  @CacheEvict(value = "drift-events", allEntries = true)
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
   * Evicts all drift-events cache entries.
   *
   * @param id the drift event ID to delete
   */
  @CacheEvict(value = "drift-events", allEntries = true)
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
   * Evicts all drift-events cache entries.
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
   * Evicts all drift-events cache entries.
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
}
