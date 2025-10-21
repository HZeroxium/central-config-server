package com.example.control.domain.port;

import com.example.control.domain.object.IamUser;
import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.domain.id.IamUserId;

import java.util.List;

/**
 * Port (hexagonal architecture) for persisting and querying {@link IamUser}.
 * <p>
 * Optional projection for cached user information from Keycloak. This provides
 * faster access to user data without requiring Keycloak Admin API calls for
 * audit, reporting, and workflow support.
 * </p>
 * <p>
 * <strong>Note:</strong> The source of truth for user information remains Keycloak.
 * This is only a cached projection that should be synchronized periodically.
 * </p>
 */
public interface IamUserRepositoryPort extends RepositoryPort<IamUser, IamUserId, IamUserCriteria> {

    /**
     * Find users belonging to a specific team.
     *
     * @param teamId the team ID
     * @return list of users in the team
     */
    List<IamUser> findByTeam(String teamId);

    /**
     * Find users by manager ID.
     *
     * @param managerId the manager's user ID
     * @return list of users reporting to the manager
     */
    List<IamUser> findByManager(String managerId);

    /**
     * Find users by role.
     *
     * @param role the role name
     * @return list of users with the role
     */
    List<IamUser> findByRole(String role);

    /**
     * Find all user IDs that belong to any of the specified teams.
     *
     * @param teamIds list of team IDs
     * @return list of user IDs
     */
    List<String> findUserIdsByTeams(List<String> teamIds);

    /**
     * Delete all user projections (for full sync).
     */
    void deleteAll();

    /**
     * Count users by team.
     *
     * @param teamId the team ID
     * @return number of users in the team
     */
    long countByTeam(String teamId);

    /**
     * Count users by role.
     *
     * @param role the role name
     * @return number of users with the role
     */
    long countByRole(String role);

    /**
     * Count all users.
     *
     * @return total number of users
     */
    long countAll();
}
