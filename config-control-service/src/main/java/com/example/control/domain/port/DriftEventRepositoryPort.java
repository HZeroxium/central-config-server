package com.example.control.domain.port;

import com.example.control.domain.DriftEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link DriftEvent}.
 */
public interface DriftEventRepositoryPort {

  /** Persist a drift event. */
  DriftEvent save(DriftEvent event);

  /** Find event by id. */
  Optional<DriftEvent> findById(String id);

  /** List with filtering and pagination. */
  Page<DriftEvent> list(DriftEventFilter filter, Pageable pageable);

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
      LocalDateTime detectedAtFrom,
      LocalDateTime detectedAtTo,
      Boolean unresolvedOnly
  ) {}
}


