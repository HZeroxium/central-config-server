package com.example.control.application.query;

import com.example.control.domain.object.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.id.DriftEventId;
import com.example.control.domain.port.DriftEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Query service for DriftEvent read operations.
 * <p>
 * Provides read-only access to DriftEvent data with caching support.
 * This service depends only on Repository Ports to avoid circular dependencies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriftEventQueryService {

    private final DriftEventRepositoryPort repository;

    /**
     * Find drift event by ID.
     *
     * @param id the event ID
     * @return optional drift event
     */
    @Cacheable(value = "drift-events", key = "#id")
    public Optional<DriftEvent> findById(DriftEventId id) {
        log.debug("Finding drift event by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Find all drift events with filtering and pagination.
     * <p>
     * This method does NOT apply user-based filtering - it returns raw data.
     * Use this for admin operations or when building permission-aware queries.
     *
     * @param criteria optional filter parameters
     * @param pageable pagination information
     * @return page of drift events
     */
    @Cacheable(value = "drift-events", key = "'all:' + #criteria.hashCode() + ':' + #pageable")
    public Page<DriftEvent> findAll(DriftEventCriteria criteria, Pageable pageable) {
        log.debug("Finding all drift events with criteria: {}", criteria);
        return repository.findAll(criteria, pageable);
    }

    /**
     * Find all drift events that are not resolved.
     *
     * @return list of unresolved drift events
     */
    @Cacheable(value = "drift-events", key = "'unresolved'")
    public List<DriftEvent> findUnresolved() {
        log.debug("Finding unresolved drift events");
        DriftEventCriteria criteria = DriftEventCriteria.builder()
            .status(DriftEvent.DriftStatus.DETECTED)
            .build();
        return repository.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Find unresolved drift events for a specific service.
     *
     * @param serviceName service name
     * @return list of unresolved events
     */
    @Cacheable(value = "drift-events", key = "'unresolved:' + #serviceName")
    public List<DriftEvent> findUnresolvedByService(String serviceName) {
        log.debug("Finding unresolved drift events for service: {}", serviceName);
        DriftEventCriteria criteria = DriftEventCriteria.builder()
            .serviceName(serviceName)
            .status(DriftEvent.DriftStatus.DETECTED)
            .build();
        return repository.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Find all drift events by service name.
     *
     * @param serviceName service name
     * @return list of drift events
     */
    @Cacheable(value = "drift-events", key = "'service:' + #serviceName")
    public List<DriftEvent> findByService(String serviceName) {
        log.debug("Finding drift events for service: {}", serviceName);
        DriftEventCriteria criteria = DriftEventCriteria.builder()
            .serviceName(serviceName)
            .build();
        return repository.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Count all drift events.
     *
     * @return total drift event count
     */
    @Cacheable(value = "drift-events", key = "'count'")
    public long countAll() {
        log.debug("Counting all drift events");
        return repository.countAll();
    }

    /**
     * Count drift events by their current status.
     *
     * @param status drift status
     * @return count of events with the given status
     */
    @Cacheable(value = "drift-events", key = "'countByStatus:' + #status")
    public long countByStatus(DriftEvent.DriftStatus status) {
        log.debug("Counting drift events by status: {}", status);
        return repository.countByStatus(status);
    }
}
