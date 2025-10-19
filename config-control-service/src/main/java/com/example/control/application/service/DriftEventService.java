package com.example.control.application.service;

import com.example.control.config.security.PermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.DriftEvent;
import com.example.control.domain.id.DriftEventId;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.port.DriftEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service for managing {@link DriftEvent} lifecycle operations.
 * <p>
 * Provides high-level operations for persistence, retrieval, and auto-resolution logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriftEventService {

  private final DriftEventRepositoryPort repository;
  private final PermissionEvaluator permissionEvaluator;

  /**
   * Saves a new drift event to the database.
   *
   * @param event domain drift event
   * @return persisted {@link DriftEvent}
   */
  @CacheEvict(value = "drift-events", allEntries = true)
  public DriftEvent save(DriftEvent event) {
    return repository.save(event);
  }

  /**
   * Retrieves a page of drift events using flexible filters and pagination.
   * <p>
   * Sorting is applied via {@link Pageable#getSort()} and delegated to the
   * Mongo adapter using {@code query.with(pageable)}.
   * <p>
   * Results are filtered by user permissions - users can only see drift events
   * for services they own or have been granted access to via service shares.
   *
   * @param criteria optional filter parameters encapsulated in a record
   * @param pageable pagination and sorting information
   * @param userContext the user context for permission filtering
   * @return a page of {@link DriftEvent}
   */
  @Cacheable(value = "drift-events", key = "'list:' + #criteria.hashCode() + ':' + #pageable + ':' + #userContext.userId")
  public Page<DriftEvent> list(DriftEventCriteria criteria, Pageable pageable, UserContext userContext) {
        log.debug("Listing drift events with criteria: {} for user: {}", criteria, userContext.getUserId());
        
        // Get all drift events first
        Page<DriftEvent> allEvents = repository.findAll(criteria, pageable);
        
        // Filter by permissions
        List<DriftEvent> filteredEvents = allEvents.getContent().stream()
                .filter(event -> permissionEvaluator.canViewDriftEvent(userContext, event))
                .toList();
        
        log.debug("Filtered {} drift events to {} for user: {}", 
                allEvents.getTotalElements(), filteredEvents.size(), userContext.getUserId());
        
        // Create new page with filtered content
        return new org.springframework.data.domain.PageImpl<>(
                filteredEvents, 
                pageable, 
                filteredEvents.size()
        );
  }

  /**
   * Finds a drift event by its identifier.
   * <p>
   * Returns the drift event only if the user has permission to view it.
   *
   * @param id event identifier
   * @param userContext the user context for permission checking
   * @return optional {@link DriftEvent} if found and user has permission
   */
  public Optional<DriftEvent> findById(DriftEventId id, UserContext userContext) {
    log.debug("Finding drift event by ID: {} for user: {}", id, userContext.getUserId());
    
    Optional<DriftEvent> event = repository.findById(id);
    
    if (event.isPresent() && !permissionEvaluator.canViewDriftEvent(userContext, event.get())) {
      log.warn("User {} does not have permission to view drift event {}", userContext.getUserId(), id);
      return Optional.empty();
    }
    
    return event;
  }

  /**
   * Retrieves all drift events that are not resolved.
   *
   * @return list of unresolved drift events
   */
  @Cacheable(value = "drift-events", key = "'unresolved'")
  public List<DriftEvent> findUnresolved() {
    DriftEventCriteria criteria = DriftEventCriteria.builder()
        .status(DriftEvent.DriftStatus.DETECTED)
        .build();
    Page<DriftEvent> page = repository.findAll(criteria, Pageable.unpaged());
    return page.getContent();
  }

  /**
   * Retrieves unresolved drift events for a specific service.
   *
   * @param serviceName service name
   * @return list of unresolved events
   */
  public List<DriftEvent> findUnresolvedByService(String serviceName) {
    DriftEventCriteria criteria = DriftEventCriteria.builder()
        .serviceName(serviceName)
        .status(DriftEvent.DriftStatus.DETECTED)
        .build();
    return repository.findAll(criteria, Pageable.unpaged()).getContent();
  }

  /**
   * Finds all drift events by service name.
   *
   * @param serviceName service name
   * @return list of drift events
   */
  @Cacheable(value = "drift-events", key = "#serviceName")
  public List<DriftEvent> findByService(String serviceName) {
    DriftEventCriteria criteria = DriftEventCriteria.builder()
        .serviceName(serviceName)
        .build();
    return repository.findAll(criteria, Pageable.unpaged()).getContent();
  }

  /**
   * Finds all drift events for a given service instance.
   *
   * @param serviceName service name
   * @param instanceId  instance identifier
   * @return list of drift events
   */
  public List<DriftEvent> findByServiceAndInstance(String serviceName, String instanceId) {
    DriftEventCriteria criteria = DriftEventCriteria.builder()
        .serviceName(serviceName)
        .instanceId(instanceId)
        .build();
    return repository.findAll(criteria, Pageable.unpaged()).getContent();
  }

  /**
   * Counts all drift events.
   *
   * @return total drift event count
   */
  public long countAll() {
    return repository.countAll();
  }

  /**
   * Counts drift events by their current status.
   *
   * @param status drift status
   * @return count of events with the given status
   */
  public long countByStatus(DriftEvent.DriftStatus status) {
    return repository.countByStatus(status);
  }

  /**
   * Resolves all unresolved drift events for a specific instance.
   * <p>
   * This method is typically triggered by the heartbeat service after verifying
   * that the instance configuration hash is up-to-date.
   *
   * @param serviceName service name
   * @param instanceId  instance identifier
   */
  @CacheEvict(value = "drift-events", allEntries = true)
  public void resolveForInstance(String serviceName, String instanceId) {
    repository.resolveForInstance(serviceName, instanceId, "heartbeat-service");
  }
}
