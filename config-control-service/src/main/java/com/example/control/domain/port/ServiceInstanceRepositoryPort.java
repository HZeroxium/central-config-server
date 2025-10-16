package com.example.control.domain.port;

import com.example.control.domain.ServiceInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ServiceInstance}.
 * Implementations reside in the infrastructure layer (e.g., MongoDB adapter).
 */
public interface ServiceInstanceRepositoryPort {

  /** Persist or update a service instance. */
  ServiceInstance saveOrUpdate(ServiceInstance instance);

  /** Find one by composite id. */
  Optional<ServiceInstance> findById(String serviceName, String instanceId);

  /** Delete by composite id. */
  void delete(String serviceName, String instanceId);

  /** Count instances by service name. */
  long countByServiceName(String serviceName);

  /**
   * List with filtering and pagination.
   */
  Page<ServiceInstance> list(ServiceInstanceFilter filter, Pageable pageable);

  /** Filter object for querying service instances. */
  record ServiceInstanceFilter(
      String serviceName,
      String instanceId,
      ServiceInstance.InstanceStatus status,
      Boolean hasDrift,
      String environment,
      String version,
      LocalDateTime lastSeenAtFrom,
      LocalDateTime lastSeenAtTo
  ) {}
}


