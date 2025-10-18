package com.example.control.domain.port;

import com.example.control.domain.DriftEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link DriftEvent}.
 */
public interface DriftEventRepositoryPort extends RepositoryPort<DriftEvent, String> {

  /** Resolve all events for an instance. */
  void resolveForInstance(String serviceName, String instanceId, String resolvedBy);

  long countAll();

  long countByStatus(DriftEvent.DriftStatus status);

  /** Filter object for querying drift events. */
  record DriftEventFilter(
      String serviceName,
      String instanceId,
      DriftEvent.DriftStatus status,
      DriftEvent.DriftSeverity severity,
      Instant detectedAtFrom,
      Instant detectedAtTo,
      Boolean unresolvedOnly,
      java.util.List<String> userTeamIds
  ) {}
}


