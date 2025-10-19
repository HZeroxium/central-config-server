package com.example.control.application.service;

import com.example.control.domain.IamTeam;
import com.example.control.domain.id.IamTeamId;
import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.port.IamTeamRepositoryPort;
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
 * Service for managing IAM team projections with caching.
 * <p>
 * Provides CRUD operations for team projections with caching
 * for performance optimization.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamTeamService {

    private final IamTeamRepositoryPort repository;

    /**
     * Save or update a team projection.
     * <p>
     * Evicts cache entries to ensure consistency.
     *
     * @param team the team projection to save
     * @return the saved team
     */
    @Transactional
    @CacheEvict(value = "iam-teams", allEntries = true)
    public IamTeam save(IamTeam team) {
        log.debug("Saving IAM team: {}", team.getTeamId());
        IamTeam saved = repository.save(team);
        log.debug("Saved IAM team: {}", saved.getTeamId());
        return saved;
    }

    /**
     * Find team by ID.
     *
     * @param id the team ID
     * @return the team if found
     */
    @Cacheable(value = "iam-teams", key = "#id")
    public Optional<IamTeam> findById(IamTeamId id) {
        log.debug("Finding IAM team by ID: {}", id);
        Optional<IamTeam> result = repository.findById(id);
        log.debug("Found IAM team: {}", result.isPresent());
        return result;
    }

    /**
     * Find teams that contain a specific user.
     *
     * @param userId the user ID
     * @return list of teams containing the user
     */
    @Cacheable(value = "iam-teams", key = "'by-member:' + #userId")
    public List<IamTeam> findByMember(String userId) {
        log.debug("Finding teams by member: {}", userId);
        List<IamTeam> teams = repository.findByMember(userId);
        log.debug("Found {} teams for member: {}", teams.size(), userId);
        return teams;
    }

    /**
     * List teams with filtering and pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of teams
     */
    @Cacheable(value = "iam-teams", key = "'list:' + #criteria.hashCode() + ':' + #pageable")
    public Page<IamTeam> list(IamTeamCriteria criteria, Pageable pageable) {
        log.debug("Listing IAM teams with criteria: {}", criteria);
        Page<IamTeam> result = repository.findAll(criteria, pageable);
        log.debug("Found {} teams", result.getTotalElements());
        return result;
    }

    /**
     * Delete all team projections (for full sync).
     * <p>
     * This method is used during full synchronization from Keycloak.
     */
    @Transactional
    @CacheEvict(value = "iam-teams", allEntries = true)
    public void deleteAll() {
        log.debug("Deleting all IAM team projections");
        repository.deleteAll();
        log.debug("Deleted all IAM team projections");
    }

    /**
     * Check if a team exists.
     *
     * @param id the team ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(IamTeamId id) {
        log.debug("Checking existence of IAM team: {}", id);
        boolean exists = repository.existsById(id);
        log.debug("IAM team exists: {}", exists);
        return exists;
    }

    /**
     * Delete a team by ID.
     *
     * @param id the team ID
     */
    @Transactional
    @CacheEvict(value = "iam-teams", allEntries = true)
    public void deleteById(IamTeamId id) {
        log.debug("Deleting IAM team: {}", id);
        repository.deleteById(id);
        log.debug("Deleted IAM team: {}", id);
    }
}
