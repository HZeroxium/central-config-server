package com.example.control.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.control.infrastructure.repository.documents.IamTeamDocument;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link IamTeamDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for cached team projections
 * from Keycloak.
 * </p>
 */
@Repository
public interface IamTeamMongoRepository extends MongoRepository<IamTeamDocument, String> {

    /**
     * Find teams that contain a specific user.
     *
     * @param userId the user ID
     * @return list of teams containing the user
     */
    @Query("{'members': ?0}")
    List<IamTeamDocument> findByMember(String userId);

    /**
     * Find teams by display name containing the search term (case-insensitive).
     *
     * @param searchTerm the search term
     * @return list of teams with matching display names
     */
    @Query("{'displayName': {'$regex': ?0, '$options': 'i'}}")
    List<IamTeamDocument> findByDisplayNameContainingIgnoreCase(String searchTerm);

    /**
     * Find teams by exact display name.
     *
     * @param displayName the display name
     * @return list of teams with the exact display name
     */
    List<IamTeamDocument> findByDisplayName(String displayName);

    /**
     * Count total number of teams.
     *
     * @return number of teams
     */
    long count();

    /**
     * Count teams that contain a specific user.
     *
     * @param userId the user ID
     * @return number of teams containing the user
     */
    @Query(value = "{'members': ?0}", count = true)
    long countByMember(String userId);

    /**
     * Check if a team exists by display name.
     *
     * @param displayName the display name
     * @return true if team exists, false otherwise
     */
    boolean existsByDisplayName(String displayName);

    /**
     * Find teams with a minimum number of members.
     *
     * @param minMembers the minimum number of members
     * @return list of teams with at least the specified number of members
     */
    @Query("{'members': {'$exists': true}, '$expr': {'$gte': [{'$size': '$members'}, ?0]}}")
    List<IamTeamDocument> findTeamsWithMinMembers(int minMembers);

    /**
     * Find teams with a maximum number of members.
     *
     * @param maxMembers the maximum number of members
     * @return list of teams with at most the specified number of members
     */
    @Query("{'members': {'$exists': true}, '$expr': {'$lte': [{'$size': '$members'}, ?0]}}")
    List<IamTeamDocument> findTeamsWithMaxMembers(int maxMembers);
}
