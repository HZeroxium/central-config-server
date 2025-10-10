package com.example.control.application.service;

import com.example.control.domain.DriftEvent;
import com.example.control.infrastructure.repository.DriftEventDocument;
import com.example.control.infrastructure.repository.DriftEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for managing {@link DriftEvent} lifecycle operations.
 * <p>
 * Provides high-level operations for persistence, retrieval, and auto-resolution logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriftEventService {

  private final DriftEventRepository repository;

  /**
   * Saves a new drift event to the database.
   *
   * @param event domain drift event
   * @return persisted {@link DriftEvent}
   */
  public DriftEvent save(DriftEvent event) {
    DriftEventDocument document = DriftEventDocument.fromDomain(event);
    repository.save(document);
    return document.toDomain();
  }

  /**
   * Retrieves all drift events that are not resolved.
   *
   * @return list of unresolved drift events
   */
  public List<DriftEvent> findUnresolved() {
    return repository.findUnresolvedEvents().stream()
        .map(DriftEventDocument::toDomain)
        .collect(Collectors.toList());
  }

  /**
   * Retrieves unresolved drift events for a specific service.
   *
   * @param serviceName service name
   * @return list of unresolved events
   */
  public List<DriftEvent> findUnresolvedByService(String serviceName) {
    return repository.findUnresolvedEventsByService(serviceName).stream()
        .map(DriftEventDocument::toDomain)
        .collect(Collectors.toList());
  }

  /**
   * Finds all drift events by service name.
   *
   * @param serviceName service name
   * @return list of drift events
   */
  public List<DriftEvent> findByService(String serviceName) {
    return repository.findByServiceName(serviceName).stream()
        .map(DriftEventDocument::toDomain)
        .collect(Collectors.toList());
  }

  /**
   * Finds all drift events for a given service instance.
   *
   * @param serviceName service name
   * @param instanceId  instance identifier
   * @return list of drift events
   */
  public List<DriftEvent> findByServiceAndInstance(String serviceName, String instanceId) {
    return repository.findByServiceNameAndInstanceId(serviceName, instanceId).stream()
        .map(DriftEventDocument::toDomain)
        .collect(Collectors.toList());
  }

  /**
   * Counts all drift events.
   *
   * @return total drift event count
   */
  public long countAll() {
    return repository.count();
  }

  /**
   * Counts drift events by their current status.
   *
   * @param status drift status
   * @return count of events with the given status
   */
  public long countByStatus(DriftEvent.DriftStatus status) {
    return repository.countByStatus(status.name());
  }

  /**
   * Resolves all unresolved drift events for a specific instance.
   * <p>
   * This method is typically triggered by the heartbeat service after verifying
   * that the instance configuration hash is up-to-date.
   *
   * @param serviceName service name
   * @param instanceId  instance identifier
   */
  public void resolveForInstance(String serviceName, String instanceId) {
    repository.findByServiceNameAndInstanceId(serviceName, instanceId).forEach(doc -> {
      if (!DriftEvent.DriftStatus.RESOLVED.name().equals(doc.getStatus())) {
        doc.setStatus(DriftEvent.DriftStatus.RESOLVED.name());
        doc.setResolvedAt(LocalDateTime.now());
        doc.setResolvedBy("heartbeat-service");
        doc.setNotes((doc.getNotes() != null ? doc.getNotes() : "") + " | Auto-resolved via heartbeat");
        repository.save(doc);
      }
    });
  }
}
