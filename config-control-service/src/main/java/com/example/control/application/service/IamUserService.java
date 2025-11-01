package com.example.control.application.service;

import com.example.control.application.command.IamUserCommandService;
import com.example.control.application.query.IamUserQueryService;
import com.example.control.domain.model.IamUser;
import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.domain.valueobject.id.IamUserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrator service for managing IAM users.
 * <p>
 * Coordinates between CommandService and QueryService for IAM user operations.
 * Handles business logic and orchestration but delegates persistence to
 * Command/Query services.
 * This service currently has minimal business logic as IAM users are simple
 * read-only projections.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamUserService {

    private final IamUserCommandService commandService;
    private final IamUserQueryService queryService;

    /**
     * Save or update an IAM user.
     * <p>
     * Delegates to CommandService for persistence.
     * Currently minimal business logic as IAM users are simple projections.
     *
     * @param user the IAM user to save
     * @return the saved user
     */
    @Transactional
    public IamUser save(IamUser user) {
        log.debug("Orchestrating save for IAM user: {}", user.getUserId());
        return commandService.save(user);
    }

    /**
     * Find IAM user by ID.
     *
     * @param id the user ID
     * @return the IAM user if found
     */
    public Optional<IamUser> findById(IamUserId id) {
        return queryService.findById(id);
    }

    /**
     * Find users belonging to a specific team.
     *
     * @param teamId the team ID
     * @return list of users in the team
     */
    public List<IamUser> findByTeam(String teamId) {
        IamUserCriteria criteria = IamUserCriteria.forTeam(teamId);
        return queryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Find users by manager ID.
     *
     * @param managerId the manager's user ID
     * @return list of users reporting to the manager
     */
    public List<IamUser> findByManager(String managerId) {
        IamUserCriteria criteria = IamUserCriteria.forManager(managerId);
        return queryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Find users by role.
     *
     * @param role the role name
     * @return list of users with the role
     */
    public List<IamUser> findByRole(String role) {
        IamUserCriteria criteria = IamUserCriteria.forRole(role);
        return queryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Find all user IDs that belong to any of the specified teams.
     *
     * @param teamIds list of team IDs
     * @return list of user IDs
     */
    public List<String> findUserIdsByTeams(List<String> teamIds) {
        return queryService.findUserIdsByTeams(teamIds);
    }

    /**
     * Count users by team.
     *
     * @param teamId the team ID
     * @return number of users in the team
     */
    public long countByTeam(String teamId) {
        IamUserCriteria criteria = IamUserCriteria.forTeam(teamId);
        return queryService.count(criteria);
    }

    /**
     * Count users by role.
     *
     * @param role the role name
     * @return number of users with the role
     */
    public long countByRole(String role) {
        IamUserCriteria criteria = IamUserCriteria.forRole(role);
        return queryService.count(criteria);
    }

    /**
     * Delete all user projections (for full sync).
     * Delegates to CommandService.
     */
    @Transactional
    public void deleteAll() {
        log.debug("Orchestrating delete all IAM users");
        commandService.deleteAll();
    }

    /**
     * List IAM users with filtering and pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of IAM users
     */
    public Page<IamUser> findAll(IamUserCriteria criteria, Pageable pageable) {
        return queryService.findAll(criteria, pageable);
    }

    /**
     * Count IAM users matching the given filter criteria.
     *
     * @param criteria the filter criteria
     * @return count of matching users
     */
    public long count(IamUserCriteria criteria) {
        return queryService.count(criteria);
    }

    /**
     * Count all IAM users.
     *
     * @return total count of all users
     */
    public long countAll() {
        return queryService.countAll();
    }

    /**
     * Check if an IAM user exists.
     *
     * @param id the user ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(IamUserId id) {
        return queryService.existsById(id);
    }
}