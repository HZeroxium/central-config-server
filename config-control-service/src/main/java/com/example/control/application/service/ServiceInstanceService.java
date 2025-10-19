package com.example.control.application.service;

import com.example.control.domain.ServiceInstance;
import com.example.control.domain.id.ServiceInstanceId;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

  /**
   * Saves or updates a {@link ServiceInstance} record in MongoDB.
   * <p>
   * If the instance does not exist, it initializes creation and update timestamps.
   *
   * @param instance the instance to save or update
   * @return persisted {@link ServiceInstance}
   */
  @CacheEvict(value = "service-instances", key = "#instance.serviceName + ':' + #instance.instanceId")
  public ServiceInstance saveOrUpdate(ServiceInstance instance) {
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
   * Retrieves a single instance by service and instance ID.
   *
   * @param serviceName service name
   * @param instanceId  instance ID
   * @return optional instance
   */
  @Cacheable(value = "service-instances", key = "#serviceName + ':' + #instanceId")
  public Optional<ServiceInstance> findByServiceAndInstance(String serviceName, String instanceId) {
    return repository.findById(ServiceInstanceId.of(serviceName, instanceId));
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
   * @param filter   optional filter parameters encapsulated in a record
   * @param pageable pagination and sorting information
   * @return a page of {@link ServiceInstance}
   */
  @Cacheable(value = "service-instances", key = "'list:' + #filter.hashCode() + ':' + #pageable")
  public Page<ServiceInstance> list(ServiceInstanceCriteria criteria, Pageable pageable) {
    return repository.findAll(criteria, pageable);
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
   * Deletes a service instance by composite id.
   *
   * @param serviceName service name
   * @param instanceId  instance id
   */
  @CacheEvict(value = "service-instances", allEntries = true)
  public void delete(String serviceName, String instanceId) {
    ServiceInstanceId id = ServiceInstanceId.of(serviceName, instanceId);
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
}
