package com.example.control.application.service;

import com.example.control.domain.object.IamUser;
import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.domain.id.IamUserId;
import com.example.control.domain.port.IamUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing IAM users with team-based access control.
 * <p>
 * Provides CRUD operations for IAM users with caching
 * for performance optimization.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamUserService {

    private final IamUserRepositoryPort repository;

    /**
     * Save or update an IAM user.
     * <p>
     * Evicts cache entries to ensure consistency.
     *
     * @param user the IAM user to save
     * @return the saved user
     */
    @Transactional
    @CacheEvict(value = "iam-users", allEntries = true)
    public IamUser save(IamUser user) {
        log.debug("Saving IAM user: {}", user.getUserId());
        IamUser saved = repository.save(user);
        log.debug("Saved IAM user: {}", saved.getUserId());
        return saved;
    }

    /**
     * Find IAM user by ID.
     *
     * @param id the user ID
     * @return the IAM user if found
     */
    @Cacheable(value = "iam-users", key = "#id")
    public Optional<IamUser> findById(IamUserId id) {
        log.debug("Finding IAM user by ID: {}", id);
        Optional<IamUser> result = repository.findById(id);
        log.debug("Found IAM user: {}", result.isPresent());
        return result;
    }

    /**
     * Find users belonging to a specific team.
     *
     * @param teamId the team ID
     * @return list of users in the team
     */
    @Cacheable(value = "iam-users", key = "'team:' + #teamId")
    public List<IamUser> findByTeam(String teamId) {
        log.debug("Finding users by team: {}", teamId);
        List<IamUser> result = repository.findByTeam(teamId);
        log.debug("Found {} users in team: {}", result.size(), teamId);
        return result;
    }

    /**
     * Find users by manager ID.
     *
     * @param managerId the manager's user ID
     * @return list of users reporting to the manager
     */
    @Cacheable(value = "iam-users", key = "'manager:' + #managerId")
    public List<IamUser> findByManager(String managerId) {
        log.debug("Finding users by manager: {}", managerId);
        List<IamUser> result = repository.findByManager(managerId);
        log.debug("Found {} users reporting to manager: {}", result.size(), managerId);
        return result;
    }

    /**
     * Find users by role.
     *
     * @param role the role name
     * @return list of users with the role
     */
    @Cacheable(value = "iam-users", key = "'role:' + #role")
    public List<IamUser> findByRole(String role) {
        log.debug("Finding users by role: {}", role);
        List<IamUser> result = repository.findByRole(role);
        log.debug("Found {} users with role: {}", result.size(), role);
        return result;
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
        List<String> result = repository.findUserIdsByTeams(teamIds);
        log.debug("Found {} user IDs for teams: {}", result.size(), teamIds);
        return result;
    }

    /**
     * Count users by team.
     *
     * @param teamId the team ID
     * @return number of users in the team
     */
    @Cacheable(value = "iam-users", key = "'count-team:' + #teamId")
    public long countByTeam(String teamId) {
        log.debug("Counting users by team: {}", teamId);
        long count = repository.countByTeam(teamId);
        log.debug("Found {} users in team: {}", count, teamId);
        return count;
    }

    /**
     * Count users by role.
     *
     * @param role the role name
     * @return number of users with the role
     */
    @Cacheable(value = "iam-users", key = "'count-role:' + #role")
    public long countByRole(String role) {
        log.debug("Counting users by role: {}", role);
        long count = repository.countByRole(role);
        log.debug("Found {} users with role: {}", count, role);
        return count;
    }

    /**
     * Delete all user projections (for full sync).
     */
    @Transactional
    @CacheEvict(value = "iam-users", allEntries = true)
    public void deleteAll() {
        log.debug("Deleting all IAM users");
        repository.deleteAll();
        log.debug("Deleted all IAM users");
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
        Page<IamUser> result = repository.findAll(criteria, pageable);
        log.debug("Found {} IAM users", result.getTotalElements());
        return result;
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
        long count = repository.count(criteria);
        log.debug("Found {} IAM users matching criteria", count);
        return count;
    }

    /**
     * Count all IAM users.
     *
     * @return total count of all users
     */
    @Cacheable(value = "iam-users", key = "'countAll'")
    public long countAll() {
        log.debug("Counting all IAM users");
        long count = repository.countAll();
        log.debug("Found {} total IAM users", count);
        return count;
    }

    /**
     * Check if an IAM user exists.
     *
     * @param id the user ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(IamUserId id) {
        log.debug("Checking existence of IAM user: {}", id);
        boolean exists = repository.existsById(id);
        log.debug("IAM user exists: {}", exists);
        return exists;
    }
}