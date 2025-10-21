package com.example.control.application.service;

import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.id.DriftEventId;
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
  private final ApplicationServiceService applicationServiceService;

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
        
        // System admins can see all events
        if (userContext.isSysAdmin()) {
            return repository.findAll(criteria, pageable);
        }
        
        // Pre-compute accessible serviceIds for efficient querying
        List<String> accessibleServiceIds = getAccessibleServiceIds(userContext);
        
        // If no accessible services, return empty page
        if (accessibleServiceIds.isEmpty()) {
            log.debug("No accessible services for user: {}, returning empty page", userContext.getUserId());
            return new org.springframework.data.domain.PageImpl<>(
                    List.of(), 
                    pageable, 
                    0
            );
        }
        
        // Build enriched criteria with accessible serviceIds
        DriftEventCriteria enrichedCriteria = criteria.toBuilder()
                .userTeamIds(userContext.getTeamIds())
                .build();
        
        // Query with team-based filtering and serviceId filtering
        Page<DriftEvent> events = repository.findAll(enrichedCriteria, pageable);
        
        // Additional filtering for shared services (if any)
        List<DriftEvent> filteredEvents = events.getContent().stream()
                .filter(event -> {
                    // Team-owned events are already filtered by repository
                    if (event.getTeamId() != null && userContext.isMemberOfTeam(event.getTeamId())) {
                        return true;
                    }
                    // Check shared services
                    return event.getServiceId() != null && accessibleServiceIds.contains(event.getServiceId());
                })
                .toList();
        
        log.debug("Found {} drift events for user: {} (team-owned + shared)", 
                filteredEvents.size(), userContext.getUserId());
        
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
    
    if (event.isPresent() && !canViewDriftEvent(userContext, event.get())) {
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
   * @param resolvedBy  identifier of who/what resolved the drift
   */
  @CacheEvict(value = "drift-events", allEntries = true)
  public void resolveForInstance(String serviceName, String instanceId, String resolvedBy) {
    repository.resolveForInstance(serviceName, instanceId, resolvedBy);
  }

  /**
   * Bulk update teamId for all drift events with the given serviceId.
   * <p>
   * Used during ownership transfer to ensure all drift events are updated
   * to reflect the new team ownership.
   *
   * @param serviceId the service ID to match
   * @param newTeamId the new team ID to set
   * @return number of drift events updated
   */
  @CacheEvict(value = "drift-events", allEntries = true)
  public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
    log.info("Bulk updating teamId to {} for all drift events of service: {}", newTeamId, serviceId);
    return repository.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
  }

  /**
   * Get list of serviceIds that the user has access to through team ownership or sharing.
   * <p>
   * This method pre-computes accessible services to optimize database queries
   * by avoiding the need to check permissions for each individual event.
   *
   * @param userContext the user context
   * @return list of accessible service IDs
   */
  private List<String> getAccessibleServiceIds(UserContext userContext) {
    log.debug("Getting accessible serviceIds for user: {}", userContext.getUserId());
    
    List<String> accessibleServiceIds = new java.util.ArrayList<>();
    
    // Get team-owned services
    if (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty()) {
      for (String teamId : userContext.getTeamIds()) {
        List<ApplicationService> teamServices = applicationServiceService.findByOwnerTeam(teamId);
        List<String> teamServiceIds = teamServices.stream()
                .map(service -> service.getId().id())
                .collect(java.util.stream.Collectors.toList());
        accessibleServiceIds.addAll(teamServiceIds);
      }
    }
    
    // Get shared services (services shared with user's teams or directly with user)
    // This is a simplified implementation - in practice, you'd query ServiceShareService
    // for services shared with the user's teams or directly with the user
    
    log.debug("Found {} accessible serviceIds for user: {}", accessibleServiceIds.size(), userContext.getUserId());
    return accessibleServiceIds;
  }

  /**
   * Check if user can view a drift event.
   * <p>
   * Users can view drift events if:
   * - They are system admins
   * - They belong to the team that owns the service
   * - They have VIEW_INSTANCE permission via service shares
   *
   * @param userContext the user context
   * @param driftEvent the drift event
   * @return true if user can view the event
   */
  private boolean canViewDriftEvent(UserContext userContext, DriftEvent driftEvent) {
    log.debug("Checking if user {} can view drift event {} for service {}", 
            userContext.getUserId(), driftEvent.getId(), driftEvent.getServiceName());
    
    // System admins can view all drift events
    if (userContext.isSysAdmin()) {
      return true;
    }
    
    // Team members can view drift events of services owned by their team
    if (driftEvent.getTeamId() != null && userContext.isMemberOfTeam(driftEvent.getTeamId())) {
      return true;
    }
    
    // Check if user has access through service shares
    if (driftEvent.getServiceId() != null) {
      List<String> accessibleServiceIds = getAccessibleServiceIds(userContext);
      return accessibleServiceIds.contains(driftEvent.getServiceId());
    }
    
    return false;
  }
}
