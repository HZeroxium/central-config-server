package com.example.control.application.query;

import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.id.IamTeamId;
import com.example.control.domain.object.IamTeam;
import com.example.control.domain.port.IamTeamRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query service for IamTeam read operations.
 * <p>
 * Handles all read operations for IamTeam domain objects with caching.
 * Responsible for data retrieval only - no writes, no business logic, no
 * permission checks.
 * All methods are read-only with appropriate caching strategies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamTeamQueryService {

    private final IamTeamRepositoryPort repository;

    /**
     * Find IAM team by ID.
     *
     * @param id the team ID
     * @return the IAM team if found
     */
    @Cacheable(value = "iam-teams", key = "#id")
    public Optional<IamTeam> findById(IamTeamId id) {
        log.debug("Finding IAM team by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * List IAM teams with filtering and pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of IAM teams
     */
    @Cacheable(value = "iam-teams", key = "'list:' + #criteria.hashCode() + ':' + #pageable")
    public Page<IamTeam> findAll(IamTeamCriteria criteria, Pageable pageable) {
        log.debug("Listing IAM teams with criteria: {}", criteria);
        return repository.findAll(criteria, pageable);
    }

    /**
     * Count IAM teams matching the given filter criteria.
     *
     * @param criteria the filter criteria
     * @return count of matching teams
     */
    @Cacheable(value = "iam-teams", key = "'count:' + #criteria.hashCode()")
    public long count(IamTeamCriteria criteria) {
        log.debug("Counting IAM teams with criteria: {}", criteria);
        return repository.count(criteria);
    }

    /**
     * Count all IAM teams.
     *
     * @return total count of all teams
     */
    @Cacheable(value = "iam-teams", key = "'countAll'")
    public long countAll() {
        log.debug("Counting all IAM teams");
        return repository.countAll();
    }

    /**
     * Check if an IAM team exists.
     *
     * @param id the team ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(IamTeamId id) {
        log.debug("Checking existence of IAM team: {}", id);
        return repository.existsById(id);
    }
}
