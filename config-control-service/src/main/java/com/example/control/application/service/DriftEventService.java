package com.example.control.application.service;

import com.example.control.domain.DriftEvent;
import com.example.control.domain.port.DriftEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service for managing {@link DriftEvent} lifecycle operations.
 * <p>
 * Provides high-level operations for persistence, retrieval, and auto-resolution logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriftEventService {

  private final DriftEventRepositoryPort repository;

  /**
   * Saves a new drift event to the database.
   *
   * @param event domain drift event
   * @return persisted {@link DriftEvent}
   */
  @CacheEvict(value = "drift-events", allEntries = true)
  public DriftEvent save(DriftEvent event) {
    return repository.save(event);
  }

  /**
   * Retrieves a page of drift events using flexible filters and pagination.
   * <p>
   * Sorting is applied via {@link Pageable#getSort()} and delegated to the
   * Mongo adapter using {@code query.with(pageable)}.
   *
   * @param filter   optional filter parameters encapsulated in a record
   * @param pageable pagination and sorting information
   * @return a page of {@link DriftEvent}
   */
  @Cacheable(value = "drift-events", key = "'list:' + #filter.hashCode() + ':' + #pageable")
  public Page<DriftEvent> list(DriftEventRepositoryPort.DriftEventFilter filter, Pageable pageable) {
    return repository.list(filter, pageable);
  }

  /**
   * Finds a drift event by its identifier.
   *
   * @param id event identifier
   * @return optional {@link DriftEvent}
   */
  public Optional<DriftEvent> findById(String id) {
    return repository.findById(id);
  }

  /**
   * Retrieves all drift events that are not resolved.
   *
   * @return list of unresolved drift events
   */
  @Cacheable(value = "drift-events", key = "'unresolved'")
  public List<DriftEvent> findUnresolved() {
    DriftEventRepositoryPort.DriftEventFilter filter = new DriftEventRepositoryPort.DriftEventFilter(
        null, null, null, null, null, null, true);
    Page<DriftEvent> page = repository.list(filter, Pageable.unpaged());
    return page.getContent();
  }

  /**
   * Retrieves unresolved drift events for a specific service.
   *
   * @param serviceName service name
   * @return list of unresolved events
   */
  public List<DriftEvent> findUnresolvedByService(String serviceName) {
    DriftEventRepositoryPort.DriftEventFilter filter = new DriftEventRepositoryPort.DriftEventFilter(
        serviceName, null, null, null, null, null, true);
    return repository.list(filter, Pageable.unpaged()).getContent();
  }

  /**
   * Finds all drift events by service name.
   *
   * @param serviceName service name
   * @return list of drift events
   */
  @Cacheable(value = "drift-events", key = "#serviceName")
  public List<DriftEvent> findByService(String serviceName) {
    DriftEventRepositoryPort.DriftEventFilter filter = new DriftEventRepositoryPort.DriftEventFilter(
        serviceName, null, null, null, null, null, null);
    return repository.list(filter, Pageable.unpaged()).getContent();
  }

  /**
   * Finds all drift events for a given service instance.
   *
   * @param serviceName service name
   * @param instanceId  instance identifier
   * @return list of drift events
   */
  public List<DriftEvent> findByServiceAndInstance(String serviceName, String instanceId) {
    DriftEventRepositoryPort.DriftEventFilter filter = new DriftEventRepositoryPort.DriftEventFilter(
        serviceName, instanceId, null, null, null, null, null);
    return repository.list(filter, Pageable.unpaged()).getContent();
  }

  /**
   * Counts all drift events.
   *
   * @return total drift event count
   */
  public long countAll() {
    return repository.countAll();
  }

  /**
   * Counts drift events by their current status.
   *
   * @param status drift status
   * @return count of events with the given status
   */
  public long countByStatus(DriftEvent.DriftStatus status) {
    return repository.countByStatus(status);
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
  @CacheEvict(value = "drift-events", allEntries = true)
  public void resolveForInstance(String serviceName, String instanceId) {
    repository.resolveForInstance(serviceName, instanceId, "heartbeat-service");
  }
}
