package com.example.control.application.service;

import com.example.control.domain.ServiceInstance;
import com.example.control.infrastructure.repository.ServiceInstanceDocument;
import com.example.control.infrastructure.repository.ServiceInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service layer responsible for managing {@link ServiceInstance} entities.
 * <p>
 * Provides business logic for persistence, drift tracking, and lifecycle operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceInstanceService {

  private final ServiceInstanceRepository repository;

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
    ServiceInstanceDocument document = ServiceInstanceDocument.fromDomain(instance);
    if (document.getCreatedAt() == null) {
      document.setCreatedAt(LocalDateTime.now());
    }
    document.setUpdatedAt(LocalDateTime.now());
    repository.save(document);
    return document.toDomain();
  }

  /**
   * Retrieves all instances belonging to a service.
   *
   * @param serviceName the service name
   * @return list of instances
   */
  @Cacheable(value = "service-instances", key = "#serviceName")
  public List<ServiceInstance> findByServiceName(String serviceName) {
    return repository.findByServiceName(serviceName)
        .stream()
        .map(ServiceInstanceDocument::toDomain)
        .collect(Collectors.toList());
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
    return repository.findByServiceNameAndInstanceId(serviceName, instanceId)
        .map(ServiceInstanceDocument::toDomain);
  }

  /**
   * Returns all instances currently marked as drifted.
   *
   * @return list of drifted instances
   */
  public List<ServiceInstance> findAllWithDrift() {
    return repository.findAllWithDrift()
        .stream()
        .map(ServiceInstanceDocument::toDomain)
        .collect(Collectors.toList());
  }

  /**
   * Returns drifted instances for a specific service.
   *
   * @param serviceName service name
   * @return list of drifted instances
   */
  public List<ServiceInstance> findByServiceWithDrift(String serviceName) {
    return repository.findByServiceNameWithDrift(serviceName)
        .stream()
        .map(ServiceInstanceDocument::toDomain)
        .collect(Collectors.toList());
  }

  /**
   * Finds instances that have become stale (inactive).
   *
   * @param threshold timestamp cutoff
   * @return list of stale instances
   */
  public List<ServiceInstance> findStaleInstances(LocalDateTime threshold) {
    return repository.findStaleInstances(threshold)
        .stream()
        .map(ServiceInstanceDocument::toDomain)
        .collect(Collectors.toList());
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
    String id = serviceName + ":" + instanceId;
    ServiceInstanceDocument document = repository.findById(id)
        .orElseGet(() -> ServiceInstanceDocument.builder()
            .id(id)
            .serviceName(serviceName)
            .instanceId(instanceId)
            .createdAt(LocalDateTime.now())
            .build());

    document.setStatus(status != null ? status.name() : null);
    document.setHasDrift(hasDrift);
    document.setConfigHash(expectedHash);
    document.setLastAppliedHash(lastAppliedHash);
    document.setUpdatedAt(LocalDateTime.now());

    repository.save(document);
    return document.toDomain();
  }
}
