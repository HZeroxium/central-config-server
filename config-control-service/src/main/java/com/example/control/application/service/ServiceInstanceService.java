package com.example.control.application.service;

import com.example.control.domain.ServiceInstance;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
      instance.setCreatedAt(LocalDateTime.now());
    }
    instance.setUpdatedAt(LocalDateTime.now());
    return repository.saveOrUpdate(instance);
  }

  /**
   * Retrieves all instances belonging to a service.
   *
   * @param serviceName the service name
   * @return list of instances
   */
  @Cacheable(value = "service-instances", key = "#serviceName")
  public List<ServiceInstance> findByServiceName(String serviceName) {
    // Backward-compatible convenience: use filter via port
    ServiceInstanceRepositoryPort.ServiceInstanceFilter filter = new ServiceInstanceRepositoryPort.ServiceInstanceFilter(
        serviceName, null, null, null, null, null, null, null);
    Page<ServiceInstance> page = repository.list(filter, Pageable.unpaged());
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
    return repository.findById(serviceName, instanceId);
  }

  /**
   * Returns all instances currently marked as drifted.
   *
   * @return list of drifted instances
   */
  public List<ServiceInstance> findAllWithDrift() {
    ServiceInstanceRepositoryPort.ServiceInstanceFilter filter = new ServiceInstanceRepositoryPort.ServiceInstanceFilter(
        null, null, ServiceInstance.InstanceStatus.DRIFT, true, null, null, null, null);
    return repository.list(filter, Pageable.unpaged()).getContent();
  }

  /**
   * Returns drifted instances for a specific service.
   *
   * @param serviceName service name
   * @return list of drifted instances
   */
  public List<ServiceInstance> findByServiceWithDrift(String serviceName) {
    ServiceInstanceRepositoryPort.ServiceInstanceFilter filter = new ServiceInstanceRepositoryPort.ServiceInstanceFilter(
        serviceName, null, ServiceInstance.InstanceStatus.DRIFT, true, null, null, null, null);
    return repository.list(filter, Pageable.unpaged()).getContent();
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
  public Page<ServiceInstance> list(ServiceInstanceRepositoryPort.ServiceInstanceFilter filter, Pageable pageable) {
    return repository.list(filter, pageable);
  }

  /**
   * Finds instances that have become stale (inactive).
   *
   * @param threshold timestamp cutoff
   * @return list of stale instances
   */
  public List<ServiceInstance> findStaleInstances(LocalDateTime threshold) {
    ServiceInstanceRepositoryPort.ServiceInstanceFilter filter = new ServiceInstanceRepositoryPort.ServiceInstanceFilter(
        null, null, null, null, null, null, threshold, null);
    return repository.list(filter, Pageable.unpaged()).getContent();
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
    repository.delete(serviceName, instanceId);
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
    ServiceInstance instance = repository.findById(serviceName, instanceId).orElse(
        ServiceInstance.builder()
            .serviceName(serviceName)
            .instanceId(instanceId)
            .createdAt(LocalDateTime.now())
            .build()
    );
    instance.setStatus(status);
    instance.setHasDrift(hasDrift);
    instance.setConfigHash(expectedHash);
    instance.setLastAppliedHash(lastAppliedHash);
    instance.setUpdatedAt(LocalDateTime.now());
    return repository.saveOrUpdate(instance);
  }
}
