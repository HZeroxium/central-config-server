package com.example.control.domain.port;

import com.example.control.domain.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.id.DriftEventId;

/**
 * Port (hexagonal architecture) for persisting and querying {@link DriftEvent}.
 */
public interface DriftEventRepositoryPort extends RepositoryPort<DriftEvent, DriftEventId, DriftEventCriteria> {

  /** Resolve all events for an instance. */
  void resolveForInstance(String serviceName, String instanceId, String resolvedBy);

  long countByStatus(DriftEvent.DriftStatus status);
}


