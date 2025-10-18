package com.example.control.domain.port;

import com.example.control.domain.IamUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

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
public interface IamUserRepositoryPort extends RepositoryPort<IamUser, String> {

    /**
     * Persist or update a user projection.
     *
     * @param user the IAM user to save
     * @return the persisted IAM user
     */
    IamUser save(IamUser user);

    /**
     * Find a user projection by Keycloak user ID.
     *
     * @param userId the Keycloak user ID (sub claim)
     * @return optional IAM user
     */
    Optional<IamUser> findById(String userId);

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
     * List all user projections with pagination.
     *
     * @param pageable pagination information
     * @return a page of IAM users
     */
    Page<IamUser> list(Pageable pageable);

    /**
     * Find all user IDs that belong to any of the specified teams.
     *
     * @param teamIds list of team IDs
     * @return list of user IDs
     */
    List<String> findUserIdsByTeams(List<String> teamIds);

    /**
     * Delete a user projection.
     *
     * @param userId the user ID to delete
     */
    void delete(String userId);

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
}
