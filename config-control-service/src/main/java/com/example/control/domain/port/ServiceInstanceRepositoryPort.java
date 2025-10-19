package com.example.control.domain.port;

import com.example.control.domain.ServiceInstance;
import com.example.control.domain.id.ServiceInstanceId;
import com.example.control.domain.criteria.ServiceInstanceCriteria;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ServiceInstance}.
 * Implementations reside in the infrastructure layer (e.g., MongoDB adapter).
 */
public interface ServiceInstanceRepositoryPort extends RepositoryPort<ServiceInstance, ServiceInstanceId, ServiceInstanceCriteria> {

  /** Count instances by service name. */
  long countByServiceName(String serviceName);
}


