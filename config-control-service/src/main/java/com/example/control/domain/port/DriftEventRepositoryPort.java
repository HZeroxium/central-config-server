package com.example.control.domain.port;

import com.example.control.domain.object.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.id.DriftEventId;

/**
 * Port (hexagonal architecture) for persisting and querying {@link DriftEvent}.
 */
public interface DriftEventRepositoryPort extends RepositoryPort<DriftEvent, DriftEventId, DriftEventCriteria> {

  /** Resolve all events for an instance. */
  void resolveForInstance(String serviceName, String instanceId, String resolvedBy);

  long countByStatus(DriftEvent.DriftStatus status);

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
  long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId);
}


