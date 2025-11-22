package com.example.control.application.query;

import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.criteria.FailedHeartbeatCriteria;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import com.example.control.domain.port.repository.FailedHeartbeatRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query service for FailedHeartbeat read operations.
 * <p>
 * Provides read-only access to FailedHeartbeat data.
 * This service depends only on Repository Ports to avoid circular dependencies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FailedHeartbeatQueryService {

    private final FailedHeartbeatRepositoryPort repository;

    /**
     * Find failed heartbeat by ID.
     *
     * @param id the failed heartbeat ID
     * @return optional failed heartbeat
     */
    public Optional<FailedHeartbeat> findById(FailedHeartbeatId id) {
        log.debug("Finding failed heartbeat by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Find all failed heartbeats with filtering and pagination.
     * <p>
     * This method does NOT apply user-based filtering - it returns raw data.
     * Use this for admin operations or when building permission-aware queries.
     *
     * @param criteria optional filter parameters
     * @param pageable pagination information
     * @return page of failed heartbeats
     */
    public Page<FailedHeartbeat> findAll(FailedHeartbeatCriteria criteria, Pageable pageable) {
        log.debug("Finding all failed heartbeats with criteria: {}", criteria);
        return repository.findAll(criteria, pageable);
    }

    /**
     * Count failed heartbeats by status.
     *
     * @param status the status to count
     * @return the count of failed heartbeats with the given status
     */
    public long countByStatus(FailedHeartbeat.FailedHeartbeatStatus status) {
        log.debug("Counting failed heartbeats by status: {}", status);
        return repository.countByStatus(status);
    }
}

