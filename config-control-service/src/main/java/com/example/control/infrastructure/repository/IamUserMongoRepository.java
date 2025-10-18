package com.example.control.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.control.infrastructure.repository.documents.IamUserDocument;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link IamUserDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for cached user projections
 * from Keycloak.
 * </p>
 */
@Repository
public interface IamUserMongoRepository extends MongoRepository<IamUserDocument, String> {

    /**
     * Find users belonging to a specific team.
     *
     * @param teamId the team ID
     * @return list of users in the team
     */
    @Query("{'teamIds': ?0}")
    List<IamUserDocument> findByTeamId(String teamId);

    /**
     * Find users by manager ID.
     *
     * @param managerId the manager's user ID
     * @return list of users reporting to the manager
     */
    List<IamUserDocument> findByManagerId(String managerId);

    /**
     * Find users by role.
     *
     * @param role the role name
     * @return list of users with the role
     */
    @Query("{'roles': ?0}")
    List<IamUserDocument> findByRole(String role);

    /**
     * Find users by email address.
     *
     * @param email the email address
     * @return list of users with the email (should be unique)
     */
    List<IamUserDocument> findByEmail(String email);

    /**
     * Find users by username.
     *
     * @param username the username
     * @return list of users with the username
     */
    List<IamUserDocument> findByUsername(String username);

    /**
     * Find all user IDs that belong to any of the specified teams.
     *
     * @param teamIds list of team IDs
     * @return list of user IDs
     */
    @Query(value = "{'teamIds': {'$in': ?0}}", fields = "{'userId': 1}")
    List<IamUserDocument> findUserIdsByTeamIds(List<String> teamIds);

    /**
     * Count users by team.
     *
     * @param teamId the team ID
     * @return number of users in the team
     */
    @Query(value = "{'teamIds': ?0}", count = true)
    long countByTeamId(String teamId);

    /**
     * Count users by role.
     *
     * @param role the role name
     * @return number of users with the role
     */
    @Query(value = "{'roles': ?0}", count = true)
    long countByRole(String role);

    /**
     * Check if a user exists by email.
     *
     * @param email the email address
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user exists by username.
     *
     * @param username the username
     * @return true if user exists, false otherwise
     */
    boolean existsByUsername(String username);
}
