package com.example.control.application.service;

import com.example.control.application.command.IamTeamCommandService;
import com.example.control.application.query.IamTeamQueryService;
import com.example.control.domain.model.IamTeam;
import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.valueobject.id.IamTeamId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrator service for managing IAM teams.
 * <p>
 * Coordinates between CommandService and QueryService for IAM team operations.
 * Handles business logic and orchestration but delegates persistence to
 * Command/Query services.
 * This service currently has minimal business logic as IAM teams are simple
 * read-only projections.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamTeamService {

    private final IamTeamCommandService commandService;
    private final IamTeamQueryService queryService;

    /**
     * Save or update an IAM team.
     * <p>
     * Delegates to CommandService for persistence.
     * Currently minimal business logic as IAM teams are simple projections.
     *
     * @param team the IAM team to save
     * @return the saved team
     */
    @Transactional
    public IamTeam save(IamTeam team) {
        log.debug("Orchestrating save for IAM team: {}", team.getTeamId());
        return commandService.save(team);
    }

    /**
     * Find IAM team by ID.
     *
     * @param id the team ID
     * @return the IAM team if found
     */
    public Optional<IamTeam> findById(IamTeamId id) {
        return queryService.findById(id);
    }

    /**
     * Find teams that contain a specific user.
     *
     * @param userId the user ID
     * @return list of teams containing the user
     */
    public List<IamTeam> findByMember(String userId) {
        IamTeamCriteria criteria = IamTeamCriteria.forMember(userId);
        return queryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * List IAM teams with filtering and pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of IAM teams
     */
    public Page<IamTeam> findAll(IamTeamCriteria criteria, Pageable pageable) {
        return queryService.findAll(criteria, pageable);
    }

    /**
     * Count IAM teams matching the given filter criteria.
     *
     * @param criteria the filter criteria
     * @return count of matching teams
     */
    public long count(IamTeamCriteria criteria) {
        return queryService.count(criteria);
    }

    /**
     * Count all IAM teams.
     *
     * @return total count of all teams
     */
    public long countAll() {
        return queryService.countAll();
    }

    /**
     * Delete all team projections (for full sync).
     * Delegates to CommandService.
     */
    @Transactional
    public void deleteAll() {
        log.debug("Orchestrating delete all IAM teams");
        commandService.deleteAll();
    }

    /**
     * Check if an IAM team exists.
     *
     * @param id the team ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(IamTeamId id) {
        return queryService.existsById(id);
    }
}