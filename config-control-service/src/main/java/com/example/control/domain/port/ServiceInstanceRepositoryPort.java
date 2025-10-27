package com.example.control.domain.port;

import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.id.ServiceInstanceId;

/**
 * Port (hexagonal architecture) for persisting and querying
 * {@link ServiceInstance}.
 * Implementations reside in the infrastructure layer (e.g., MongoDB adapter).
 */
public interface ServiceInstanceRepositoryPort
    extends RepositoryPort<ServiceInstance, ServiceInstanceId, ServiceInstanceCriteria> {

  /** Count instances by service ID. */
  long countByServiceId(String serviceId);

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
  long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId);
}
