package com.example.control.application.service;

import com.example.control.config.security.DomainPermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.ServiceInstanceId;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Application service layer responsible for managing {@link ServiceInstance} entities.
 * <p>
 * Provides business logic for persistence, drift tracking, and lifecycle operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceInstanceService {

  private final ServiceInstanceRepositoryPort repository;
  private final DomainPermissionEvaluator permissionEvaluator;
  // private final ApplicationServiceService applicationServiceService;

  /**
   * Saves or updates a {@link ServiceInstance} record in MongoDB.
   * <p>
   * If the instance does not exist, it initializes creation and update timestamps.
   *
   * @param instance the instance to save or update
   * @return persisted {@link ServiceInstance}
   */
  @CacheEvict(value = "service-instances", key = "#instance.serviceName + ':' + #instance.instanceId")
  public ServiceInstance save(ServiceInstance instance) {
    if (instance.getCreatedAt() == null) {
      instance.setCreatedAt(Instant.now());
    }
    instance.setUpdatedAt(Instant.now());
        return repository.save(instance);
  }

  /**
   * Retrieves all instances belonging to a service.
   *
   * @param serviceName the service name
   * @return list of instances
   */
  @Cacheable(value = "service-instances", key = "#serviceName")
  public List<ServiceInstance> findByServiceName(String serviceName) {
    // Backward-compatible convenience: use criteria via port
    ServiceInstanceCriteria criteria = ServiceInstanceCriteria.builder()
        .serviceName(serviceName)
        .build();
        Page<ServiceInstance> page = repository.findAll(criteria, Pageable.unpaged());
    return page.getContent();
  }

  /**
   * Retrieves a single instance by ID.
   *
   * @param id the service instance ID
   * @return optional instance
   */
  @Cacheable(value = "service-instances", key = "#id")
  public Optional<ServiceInstance> findById(ServiceInstanceId id) {
    return repository.findById(id);
  }

  /**
   * Returns all instances currently marked as drifted.
   *
   * @return list of drifted instances
   */
  public List<ServiceInstance> findAllWithDrift() {
    ServiceInstanceCriteria criteria = ServiceInstanceCriteria.builder()
        .status(ServiceInstance.InstanceStatus.DRIFT)
        .hasDrift(true)
        .build();
    return repository.findAll(criteria, Pageable.unpaged()).getContent();
  }

  /**
   * Returns drifted instances for a specific service.
   *
   * @param serviceName service name
   * @return list of drifted instances
   */
  public List<ServiceInstance> findByServiceWithDrift(String serviceName) {
    ServiceInstanceCriteria criteria = ServiceInstanceCriteria.builder()
        .serviceName(serviceName)
        .status(ServiceInstance.InstanceStatus.DRIFT)
        .hasDrift(true)
        .build();
    return repository.findAll(criteria, Pageable.unpaged()).getContent();
  }

  /**
   * Retrieves a page of service instances using flexible filters and pagination.
   * <p>
   * Sorting is applied via {@link Pageable#getSort()} and delegated to the
   * Mongo adapter using {@code query.with(pageable)}.
   *
   * @param criteria optional filter parameters encapsulated in a record
   * @param pageable pagination and sorting information
   * @return a page of {@link ServiceInstance}
   */
  @Cacheable(value = "service-instances", key = "'list:' + #criteria.hashCode() + ':' + #pageable")
  public Page<ServiceInstance> findAll(ServiceInstanceCriteria criteria, Pageable pageable) {
    return repository.findAll(criteria, pageable);
  }

  /**
   * Retrieves a page of service instances with permission-aware filtering.
   * <p>
   * Results are filtered by user permissions - users can only see instances
   * for services they own or have been granted access to via service shares.
   *
   * @param criteria optional filter parameters encapsulated in a record
   * @param pageable pagination and sorting information
   * @param userContext the user context for permission filtering
   * @return a page of {@link ServiceInstance}
   */
  @Cacheable(value = "service-instances", key = "'list:' + #criteria.hashCode() + ':' + #pageable + ':' + #userContext.userId")
  public Page<ServiceInstance> findAll(ServiceInstanceCriteria criteria, Pageable pageable, UserContext userContext) {
    log.debug("Listing service instances with criteria: {} for user: {}", criteria, userContext.getUserId());
    
    // System admins can see all instances
    if (userContext.isSysAdmin()) {
      return repository.findAll(criteria, pageable);
    }
    
    // Apply team-based filtering
    ServiceInstanceCriteria filteredCriteria = applyUserFilter(criteria, userContext);
    return repository.findAll(filteredCriteria, pageable);
  }

  /**
   * Applies user-based team filtering to criteria.
   * <p>
   * This method ensures that team-based access control is enforced at the service layer.
   * If userContext is null or user has no teams, returns criteria as-is (admin access).
   *
   * @param criteria the base criteria
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
    ServiceInstanceCriteria criteria = ServiceInstanceCriteria.builder()
        .lastSeenAtTo(threshold)
        .build();
    return repository.findAll(criteria, Pageable.unpaged()).getContent();
  }

  /**
   * Counts instances for a given service.
   *
   * @param serviceName service name
   * @return count of instances
   */
  public long countByServiceName(String serviceName) {
    return repository.countByServiceName(serviceName);
  }
  
  /**
   * Deletes a service instance by ID.
   *
   * @param id the service instance ID
   */
  @CacheEvict(value = "service-instances", allEntries = true)
  public void delete(ServiceInstanceId id) {
    repository.deleteById(id);
  }

  /**
   * Updates an instance's status, drift flag, and hash information.
   * <p>
   * Creates a new record if one does not already exist.
   *
   * @param serviceName     service name
   * @param instanceId      instance ID
   * @param status          new status
   * @param hasDrift        whether drift detected
   * @param expectedHash    expected config hash
   * @param lastAppliedHash last applied config hash
   * @return updated {@link ServiceInstance}
   */
  @CacheEvict(value = "service-instances", allEntries = true)
  public ServiceInstance updateStatusAndDrift(String serviceName,
                                              String instanceId,
                                              ServiceInstance.InstanceStatus status,
                                              boolean hasDrift,
                                              String expectedHash,
                                              String lastAppliedHash) {
    ServiceInstance instance = repository.findById(ServiceInstanceId.of(serviceName, instanceId)).orElse(
        ServiceInstance.builder()
            .id(ServiceInstanceId.of(serviceName, instanceId))
            .createdAt(Instant.now())
            .build()
    );
    instance.setStatus(status);
    instance.setHasDrift(hasDrift);
    instance.setExpectedHash(expectedHash);
    instance.setLastAppliedHash(lastAppliedHash);
    instance.setUpdatedAt(Instant.now());
        return repository.save(instance);
  }

  /**
   * Bulk update teamId for all service instances with the given serviceId.
   * <p>
   * Used during ownership transfer to ensure all instances are updated
   * to reflect the new team ownership.
   *
   * @param serviceId the service ID to match
   * @param newTeamId the new team ID to set
   * @return number of instances updated
   */
  @CacheEvict(value = "service-instances", allEntries = true)
  public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
    log.info("Bulk updating teamId to {} for all instances of service: {}", newTeamId, serviceId);
    return repository.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
  }

  /**
   * Creates a new service instance with permission validation.
   * <p>
   * Validates that the user can create instances for the specified service
   * and sets the teamId from the ApplicationService.
   *
   * @param instance the instance to create
   * @param userContext the user context for permission checking
   * @return the created instance
   * @throws SecurityException if user lacks permission to create instance
   */
  // @Transactional
  // @CacheEvict(value = "service-instances", allEntries = true)
  // public ServiceInstance create(ServiceInstance instance, UserContext userContext) {
  //   log.debug("Creating service instance {} for user {}", instance.getId(), userContext.getUserId());
    
  //   // Validate serviceId exists and get ApplicationService
  //   if (instance.getServiceId() == null) {
  //     throw new IllegalArgumentException("ServiceId is required for instance creation");
  //   }
    
  //   ApplicationService service = applicationServiceService.findById(ApplicationServiceId.of(instance.getServiceId()))
  //       .orElseThrow(() -> new IllegalArgumentException("ApplicationService not found: " + instance.getServiceId()));
    
  //   // Set teamId from ApplicationService
  //   instance.setTeamId(service.getOwnerTeamId());
    
  //   // Check if user can edit instances for this service
  //   if (!permissionEvaluator.canEditInstance(userContext, instance)) {
  //     log.warn("User {} denied permission to create instance for service {}", 
  //         userContext.getUserId(), instance.getServiceId());
  //     throw new SecurityException("Insufficient permissions to create instance for service: " + instance.getServiceId());
  //   }
    
  //   // Initialize timestamps
  //   if (instance.getCreatedAt() == null) {
  //     instance.setCreatedAt(Instant.now());
  //   }
  //   instance.setUpdatedAt(Instant.now());
    
  //   return repository.save(instance);
  // }

  /**
   * Updates an existing service instance with permission validation.
   * <p>
   * Validates that the user can edit the instance before applying updates.
   *
   * @param id the instance ID
   * @param updates the updates to apply
   * @param userContext the user context for permission checking
   * @return the updated instance
   * @throws SecurityException if user lacks permission to edit instance
   */
  @Transactional
  @CacheEvict(value = "service-instances", allEntries = true)
  public ServiceInstance update(ServiceInstanceId id, ServiceInstance updates, UserContext userContext) {
    log.debug("Updating service instance {} for user {}", id, userContext.getUserId());
    
    // Fetch existing instance
    ServiceInstance existing = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("ServiceInstance not found: " + id));
    
    // Check if user can edit this instance
    if (!permissionEvaluator.canEditInstance(userContext, existing)) {
      log.warn("User {} denied permission to edit instance {}", userContext.getUserId(), id);
      throw new SecurityException("Insufficient permissions to edit instance: " + id);
    }
    
    // Apply updates (preserve ID and teamId)
    updates.setId(id);
    updates.setTeamId(existing.getTeamId());
    updates.setServiceId(existing.getServiceId());
    updates.setUpdatedAt(Instant.now());
    
    return repository.save(updates);
  }

  /**
   * Deletes a service instance with permission validation.
   * <p>
   * Validates that the user can edit the instance before deletion.
   *
   * @param id the instance ID
   * @param userContext the user context for permission checking
   * @throws SecurityException if user lacks permission to delete instance
   */
  @Transactional
  @CacheEvict(value = "service-instances", allEntries = true)
  public void delete(ServiceInstanceId id, UserContext userContext) {
    log.debug("Deleting service instance {} for user {}", id, userContext.getUserId());
    
    // Fetch existing instance
    ServiceInstance existing = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("ServiceInstance not found: " + id));
    
    // Check if user can edit this instance
    if (!permissionEvaluator.canEditInstance(userContext, existing)) {
      log.warn("User {} denied permission to delete instance {}", userContext.getUserId(), id);
      throw new SecurityException("Insufficient permissions to delete instance: " + id);
    }
    
    repository.deleteById(id);
  }

  /**
   * Retrieves a service instance with permission validation.
   * <p>
   * Returns the instance only if the user has permission to view it.
   *
   * @param id the instance ID
   * @param userContext the user context for permission checking
   * @return the instance if found and user has permission, empty otherwise
   */
  @Cacheable(value = "service-instances", key = "#id + ':' + #userContext.userId")
  public Optional<ServiceInstance> findById(ServiceInstanceId id, UserContext userContext) {
    log.debug("Finding service instance {} for user {}", id, userContext.getUserId());
    
    Optional<ServiceInstance> instance = repository.findById(id);
    
    if (instance.isPresent() && !permissionEvaluator.canViewInstance(userContext, instance.get())) {
      log.warn("User {} does not have permission to view instance {}", userContext.getUserId(), id);
      return Optional.empty();
    }
    
    return instance;
  }
}
