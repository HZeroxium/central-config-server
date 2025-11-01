package com.example.control.application.query;

import com.example.control.domain.criteria.ApprovalDecisionCriteria;
import com.example.control.domain.valueobject.id.ApprovalDecisionId;
import com.example.control.domain.model.ApprovalDecision;
import com.example.control.domain.port.repository.ApprovalDecisionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query service for ApprovalDecision read operations.
 * <p>
 * Handles all read operations for ApprovalDecision domain objects with caching.
 * Responsible for data retrieval only - no writes, no business logic, no
 * permission checks.
 * All methods are read-only with appropriate caching strategies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalDecisionQueryService {

    private final ApprovalDecisionRepositoryPort repository;

    /**
     * Find approval decision by ID.
     *
     * @param id the approval decision ID
     * @return the approval decision if found
     */
    @Cacheable(value = "approval-decisions", key = "#id")
    public Optional<ApprovalDecision> findById(ApprovalDecisionId id) {
        log.debug("Finding approval decision by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * List approval decisions with filtering and pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of approval decisions
     */
    @Cacheable(value = "approval-decisions", key = "T(com.example.control.infrastructure.cache.CacheKeyGenerator).generateKey('list', #criteria, #pageable)")
    public Page<ApprovalDecision> findAll(ApprovalDecisionCriteria criteria, Pageable pageable) {
        log.debug("Listing approval decisions with criteria: {}", criteria);
        return repository.findAll(criteria, pageable);
    }

    /**
     * Count approval decisions matching the given filter criteria.
     *
     * @param criteria the filter criteria
     * @return count of matching decisions
     */
    @Cacheable(value = "approval-decisions", key = "T(com.example.control.infrastructure.cache.CacheKeyGenerator).generateKeyFromHash('count', #criteria)")
    public long count(ApprovalDecisionCriteria criteria) {
        log.debug("Counting approval decisions with criteria: {}", criteria);
        return repository.count(criteria);
    }

    /**
     * Check if an approval decision exists.
     *
     * @param id the approval decision ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(ApprovalDecisionId id) {
        log.debug("Checking existence of approval decision: {}", id);
        return repository.existsById(id);
    }
}
