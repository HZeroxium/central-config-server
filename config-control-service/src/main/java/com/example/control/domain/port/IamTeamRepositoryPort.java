package com.example.control.domain.port;

import com.example.control.domain.object.IamTeam;
import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.id.IamTeamId;

import java.util.List;

/**
 * Port (hexagonal architecture) for persisting and querying {@link IamTeam}.
 * <p>
 * Optional projection for cached team information from Keycloak. This provides
 * faster access to team data without requiring Keycloak Admin API calls for
 * audit, reporting, and workflow support.
 * </p>
 * <p>
 * <strong>Note:</strong> The source of truth for team information remains
 * Keycloak groups.
 * This is only a cached projection that should be synchronized periodically.
 * </p>
 */
public interface IamTeamRepositoryPort extends RepositoryPort<IamTeam, IamTeamId, IamTeamCriteria> {

    /**
     * Find teams that contain a specific user.
     *
     * @param userId the user ID
     * @return list of teams containing the user
     */
    List<IamTeam> findByMember(String userId);

    /**
     * Delete all team projections (for full sync).
     */
    // void deleteAll();

    /**
     * Count all teams.
     *
     * @return total number of teams
     */
    long countAll();
}
