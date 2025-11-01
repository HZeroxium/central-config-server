package com.example.control.infrastructure.adapter.persistence.mongo.repository;

import com.example.control.infrastructure.adapter.persistence.mongo.documents.IamTeamDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

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
     * Count total number of teams.
     *
     * @return number of teams
     */
    long count();

}
