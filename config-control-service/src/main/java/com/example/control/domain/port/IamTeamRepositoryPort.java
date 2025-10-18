package com.example.control.domain.port;

import com.example.control.domain.IamTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link IamTeam}.
 * <p>
 * Optional projection for cached team information from Keycloak. This provides
 * faster access to team data without requiring Keycloak Admin API calls for
 * audit, reporting, and workflow support.
 * </p>
 * <p>
 * <strong>Note:</strong> The source of truth for team information remains Keycloak groups.
 * This is only a cached projection that should be synchronized periodically.
 * </p>
 */
public interface IamTeamRepositoryPort {

    /**
     * Persist or update a team projection.
     *
     * @param team the IAM team to save
     * @return the persisted IAM team
     */
    IamTeam save(IamTeam team);

    /**
     * Find a team projection by Keycloak group name.
     *
     * @param teamId the team ID (Keycloak group name)
     * @return optional IAM team
     */
    Optional<IamTeam> findById(String teamId);

    /**
     * Find all team projections.
     *
     * @return list of all IAM teams
     */
    List<IamTeam> findAll();

    /**
     * List team projections with pagination.
     *
     * @param pageable pagination information
     * @return a page of IAM teams
     */
    Page<IamTeam> list(Pageable pageable);

    /**
     * Find teams that contain a specific user.
     *
     * @param userId the user ID
     * @return list of teams containing the user
     */
    List<IamTeam> findByMember(String userId);

    /**
     * Check if a team exists.
     *
     * @param teamId the team ID
     * @return true if team exists, false otherwise
     */
    boolean existsById(String teamId);

    /**
     * Delete a team projection.
     *
     * @param teamId the team ID to delete
     */
    void delete(String teamId);

    /**
     * Delete all team projections (for full sync).
     */
    void deleteAll();

    /**
     * Count total number of teams.
     *
     * @return number of teams
     */
    long count();
}
