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
public interface ServiceInstanceRepositoryPort extends RepositoryPort<ServiceInstance, String> {

  /** Find one by composite id. */
  Optional<ServiceInstance> findById(String serviceName, String instanceId);

  /** Delete by composite id. */
  void delete(String serviceName, String instanceId);

  /** Count instances by service name. */
  long countByServiceName(String serviceName);

  /** Filter object for querying service instances. */
  record ServiceInstanceFilter(
      String serviceName,
      String instanceId,
      ServiceInstance.InstanceStatus status,
      Boolean hasDrift,
      String environment,
      String version,
      java.time.Instant lastSeenAtFrom,
      java.time.Instant lastSeenAtTo,
      java.util.List<String> userTeamIds
  ) {}
}


