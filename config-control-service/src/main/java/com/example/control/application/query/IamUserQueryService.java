package com.example.control.application.query;

import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.domain.id.IamUserId;
import com.example.control.domain.object.IamUser;
import com.example.control.domain.port.IamUserRepositoryPort;
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
 * Query service for IamUser read operations.
 * <p>
 * Handles all read operations for IamUser domain objects with caching.
 * Responsible for data retrieval only - no writes, no business logic, no
 * permission checks.
 * All methods are read-only with appropriate caching strategies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamUserQueryService {

    private final IamUserRepositoryPort repository;

    /**
     * Find IAM user by ID.
     *
     * @param id the user ID
     * @return the IAM user if found
     */
    @Cacheable(value = "iam-users", key = "#id")
    public Optional<IamUser> findById(IamUserId id) {
        log.debug("Finding IAM user by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Find all user IDs that belong to any of the specified teams.
     *
     * @param teamIds list of team IDs
     * @return list of user IDs
     */
    @Cacheable(value = "iam-users", key = "'userIds:' + #teamIds.hashCode()")
    public List<String> findUserIdsByTeams(List<String> teamIds) {
        log.debug("Finding user IDs by teams: {}", teamIds);
        return repository.findUserIdsByTeams(teamIds);
    }

    /**
     * List IAM users with filtering and pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of IAM users
     */
    @Cacheable(value = "iam-users", key = "'list:' + #criteria.hashCode() + ':' + #pageable")
    public Page<IamUser> findAll(IamUserCriteria criteria, Pageable pageable) {
        log.debug("Listing IAM users with criteria: {}", criteria);
        return repository.findAll(criteria, pageable);
    }

    /**
     * Count IAM users matching the given filter criteria.
     *
     * @param criteria the filter criteria
     * @return count of matching users
     */
    @Cacheable(value = "iam-users", key = "'count:' + #criteria.hashCode()")
    public long count(IamUserCriteria criteria) {
        log.debug("Counting IAM users with criteria: {}", criteria);
        return repository.count(criteria);
    }

    /**
     * Count all IAM users.
     *
     * @return total count of all users
     */
    @Cacheable(value = "iam-users", key = "'countAll'")
    public long countAll() {
        log.debug("Counting all IAM users");
        return repository.countAll();
    }

    /**
     * Check if an IAM user exists.
     *
     * @param id the user ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(IamUserId id) {
        log.debug("Checking existence of IAM user: {}", id);
        return repository.existsById(id);
    }
}
