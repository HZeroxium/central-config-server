package com.example.control.application.service;

import com.example.control.application.command.ServiceInstanceCommandService;
import com.example.control.application.query.ServiceInstanceQueryService;
import com.example.control.config.security.DomainPermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.id.ServiceInstanceId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrator service for managing ServiceInstance entities.
 * <p>
 * Coordinates between CommandService and QueryService for service instance
 * operations.
 * Handles business logic, permission checks, and team-based filtering.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceInstanceService {

  private final ServiceInstanceCommandService commandService;
  private final ServiceInstanceQueryService queryService;
  private final DomainPermissionEvaluator permissionEvaluator;

  /**
   * Saves or updates a {@link ServiceInstance} record.
   * <p>
   * Handles business logic for timestamp initialization and delegates to
   * CommandService.
   * If the instance does not exist, it initializes creation and update
   * timestamps.
   *
   * @param instance the instance to save or update
   * @return persisted {@link ServiceInstance}
   */
  @Transactional
  public ServiceInstance save(ServiceInstance instance) {
    log.debug("Orchestrating save for service instance: {}", instance.getId());

    // Business logic: Initialize timestamps if new instance
    if (instance.getCreatedAt() == null) {
      instance.setCreatedAt(Instant.now());
    }
    instance.setUpdatedAt(Instant.now());

    return commandService.save(instance);
  }

  /**
   * Retrieves all instances belonging to a service.
   *
   * @param serviceId the service ID
   * @return list of instances
   */
  public List<ServiceInstance> findByServiceId(String serviceId) {
    return queryService.findByServiceId(serviceId);
  }

  /**
   * Retrieves a single instance by ID.
   *
   * @param id the service instance ID
   * @return optional instance
   */
  public Optional<ServiceInstance> findById(ServiceInstanceId id) {
    return queryService.findById(id);
  }

  /**
   * Returns all instances currently marked as drifted.
   *
   * @return list of drifted instances
   */
  public List<ServiceInstance> findAllWithDrift() {
    return queryService.findAllWithDrift();
  }

  /**
   * Returns drifted instances for a specific service.
   *
   * @param serviceId service ID
   * @return list of drifted instances
   */
  public List<ServiceInstance> findByServiceWithDrift(String serviceId) {
    return queryService.findByServiceWithDrift(serviceId);
  }

  /**
   * Retrieves a page of service instances using flexible filters and pagination.
   * <p>
   * Sorting is applied via {@link Pageable#getSort()} and delegated to the
   * Mongo adapter using {@code query.with(pageable)}.
   * Raw data query - no permission filtering.
   *
   * @param criteria optional filter parameters encapsulated in a record
   * @param pageable pagination and sorting information
   * @return a page of {@link ServiceInstance}
   */
  public Page<ServiceInstance> findAll(ServiceInstanceCriteria criteria, Pageable pageable) {
    return queryService.findAll(criteria, pageable);
  }

  /**
   * Retrieves a page of service instances with permission-aware filtering.
   * <p>
   * Business logic: Applies team-based access control.
   * Results are filtered by user permissions - users can only see instances
   * for services they own or have been granted access to via service shares.
   *
   * @param criteria    optional filter parameters encapsulated in a record
   * @param pageable    pagination and sorting information
   * @param userContext the user context for permission filtering
   * @return a page of {@link ServiceInstance}
   */
  public Page<ServiceInstance> findAll(ServiceInstanceCriteria criteria, Pageable pageable, UserContext userContext) {
    log.debug("Orchestrating findAll with permission filtering for user: {}", userContext.getUserId());

    // System admins can see all instances
    if (userContext.isSysAdmin()) {
      return queryService.findAll(criteria, pageable);
    }

    // Apply team-based filtering (business logic)
    ServiceInstanceCriteria filteredCriteria = applyUserFilter(criteria, userContext);
    return queryService.findAll(filteredCriteria, pageable);
  }

  /**
   * Applies user-based team filtering to criteria.
   * <p>
   * This method ensures that team-based access control is enforced at the service
   * layer.
   * If userContext is null or user has no teams, returns criteria as-is (admin
   * access).
   *
   * @param criteria    the base criteria
   * @param userContext the user context for team filtering
   * @return criteria with team filtering applied
   */
  public ServiceInstanceCriteria applyUserFilter(ServiceInstanceCriteria criteria, UserContext userContext) {
    if (userContext == null || userContext.isSysAdmin()) {
      return criteria;
    }
    List<String> teams = userContext.getTeamIds();
    if (teams == null || teams.isEmpty()) {
      return criteria.toBuilder().userTeamIds(List.of("__none__")).build(); // yields empty result
    }
    return criteria.toBuilder().userTeamIds(teams).build();
  }

  /**
   * Finds instances that have become stale (inactive).
   *
   * @param threshold timestamp cutoff
   * @return list of stale instances
   */
  public List<ServiceInstance> findStaleInstances(java.time.Instant threshold) {
    return queryService.findStaleInstances(threshold);
  }

  /**
   * Counts instances for a given service.
   *
   * @param serviceId the service ID
   * @return count of instances
   */
  public long countByServiceId(String serviceId) {
    return queryService.countByServiceId(serviceId);
  }

  /**
   * Deletes a service instance by ID.
   *
   * @param id the service instance ID
   */
  @Transactional
  public void delete(ServiceInstanceId id) {
    log.debug("Orchestrating delete for service instance: {}", id);
    commandService.deleteById(id);
  }

  /**
   * Updates an instance's status, drift flag, and hash information.
   * <p>
   * Business logic: Creates a new record if one does not already exist.
   * Enriches with timestamps and delegates to CommandService.
   *
   * @param instanceId      instance ID
   * @param status          new status
   * @param hasDrift        whether drift detected
   * @param expectedHash    expected config hash
   * @param lastAppliedHash last applied config hash
   * @return updated {@link ServiceInstance}
   */
  @Transactional
  public ServiceInstance updateStatusAndDrift(String instanceId,
      ServiceInstance.InstanceStatus status,
      boolean hasDrift,
      String expectedHash,
      String lastAppliedHash) {
    log.debug("Orchestrating status/drift update for instance: {}", instanceId);

    // Business logic: Find or create instance
    ServiceInstance instance = queryService.findById(ServiceInstanceId.of(instanceId)).orElse(
        ServiceInstance.builder()
            .id(ServiceInstanceId.of(instanceId))
            .createdAt(Instant.now())
            .build());

    // Business logic: Update fields
    instance.setStatus(status);
    instance.setHasDrift(hasDrift);
    instance.setExpectedHash(expectedHash);
    instance.setLastAppliedHash(lastAppliedHash);
    instance.setUpdatedAt(Instant.now());

    return commandService.save(instance);
  }

  /**
   * Bulk update teamId for all service instances with the given serviceId.
   * <p>
   * Used during ownership transfer to ensure all instances are updated
   * to reflect the new team ownership.
   * Delegates to CommandService for the bulk operation.
   *
   * @param serviceId the service ID to match
   * @param newTeamId the new team ID to set
   * @return number of instances updated
   */
  @Transactional
  public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
    log.info("Orchestrating bulk teamId update for instances of service: {}", serviceId);
    return commandService.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
  }

  /**
   * Creates a new service instance with permission validation.
   * <p>
   * Validates that the user can create instances for the specified service
   * and sets the teamId from the ApplicationService.
   *
   * @param instance    the instance to create
   * @param userContext the user context for permission checking
   * @return the created instance
   * @throws SecurityException if user lacks permission to create instance
   */
  // @Transactional
  // @CacheEvict(value = "service-instances", allEntries = true)
  // public ServiceInstance create(ServiceInstance instance, UserContext
  // userContext) {
  // log.debug("Creating service instance {} for user {}", instance.getId(),
  // userContext.getUserId());

  // // Validate serviceId exists and get ApplicationService
  // if (instance.getServiceId() == null) {
  // throw new IllegalArgumentException("ServiceId is required for instance
  // creation");
  // }

  // ApplicationService service =
  // applicationServiceService.findById(ApplicationServiceId.of(instance.getServiceId()))
  // .orElseThrow(() -> new IllegalArgumentException("ApplicationService not
  // found: " + instance.getServiceId()));

  // // Set teamId from ApplicationService
  // instance.setTeamId(service.getOwnerTeamId());

  // // Check if user can edit instances for this service
  // if (!permissionEvaluator.canEditInstance(userContext, instance)) {
  // log.warn("User {} denied permission to create instance for service {}",
  // userContext.getUserId(), instance.getServiceId());
  // throw new SecurityException("Insufficient permissions to create instance for
  // service: " + instance.getServiceId());
  // }

  // // Initialize timestamps
  // if (instance.getCreatedAt() == null) {
  // instance.setCreatedAt(Instant.now());
  // }
  // instance.setUpdatedAt(Instant.now());

  // return repository.save(instance);
  // }

  /**
   * Updates an existing service instance with permission validation.
   * <p>
   * Business logic: Validates permissions and delegates to CommandService.
   *
   * @param id          the instance ID
   * @param updates     the updates to apply
   * @param userContext the user context for permission checking
   * @return the updated instance
   * @throws SecurityException if user lacks permission to edit instance
   */
  @Transactional
  public ServiceInstance update(ServiceInstanceId id, ServiceInstance updates, UserContext userContext) {
    log.debug("Orchestrating update for service instance {} by user {}", id, userContext.getUserId());

    // Fetch existing instance
    ServiceInstance existing = queryService.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("ServiceInstance not found: " + id));

    // Business logic: Check if user can edit this instance
    if (!permissionEvaluator.canEditInstance(userContext, existing)) {
      log.warn("User {} denied permission to edit instance {}", userContext.getUserId(), id);
      throw new SecurityException("Insufficient permissions to edit instance: " + id);
    }

    // Business logic: Apply updates (preserve ID and teamId)
    updates.setId(id);
    updates.setTeamId(existing.getTeamId());
    updates.setServiceId(existing.getServiceId());
    updates.setUpdatedAt(Instant.now());

    return commandService.save(updates);
  }

  /**
   * Deletes a service instance with permission validation.
   * <p>
   * Business logic: Validates permissions and delegates to CommandService.
   *
   * @param id          the instance ID
   * @param userContext the user context for permission checking
   * @throws SecurityException if user lacks permission to delete instance
   */
  @Transactional
  public void delete(ServiceInstanceId id, UserContext userContext) {
    log.debug("Orchestrating delete for service instance {} by user {}", id, userContext.getUserId());

    // Fetch existing instance
    ServiceInstance existing = queryService.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("ServiceInstance not found: " + id));

    // Business logic: Check if user can edit this instance
    if (!permissionEvaluator.canEditInstance(userContext, existing)) {
      log.warn("User {} denied permission to delete instance {}", userContext.getUserId(), id);
      throw new SecurityException("Insufficient permissions to delete instance: " + id);
    }

    commandService.deleteById(id);
  }

  /**
   * Retrieves a service instance with permission validation.
   * <p>
   * Business logic: Applies permission filtering.
   *
   * @param id          the instance ID
   * @param userContext the user context for permission checking
   * @return the instance if found and user has permission, empty otherwise
   */
  public Optional<ServiceInstance> findById(ServiceInstanceId id, UserContext userContext) {
    log.debug("Orchestrating findById with permission filtering for instance {} and user {}", id,
        userContext.getUserId());

    Optional<ServiceInstance> instance = queryService.findById(id);

    // Business logic: Filter by permissions
    if (instance.isPresent() && !permissionEvaluator.canViewInstance(userContext, instance.get())) {
      log.warn("User {} does not have permission to view instance {}", userContext.getUserId(), id);
      return Optional.empty();
    }

    return instance;
  }
}
