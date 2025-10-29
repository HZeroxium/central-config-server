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
     * Count drift events with filtering.
     *
     * @param criteria filter criteria
     * @return count of events matching criteria
     */
    @Cacheable(value = "drift-events", key = "'count:' + #criteria.hashCode()")
    public long count(DriftEventCriteria criteria) {
        log.debug("Counting drift events with criteria: {}", criteria);
        return repository.count(criteria);
    }

}
