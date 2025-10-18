package com.example.control.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.control.infrastructure.repository.documents.ApplicationServiceDocument;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link ApplicationServiceDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for application services.
 * </p>
 */
@Repository
public interface ApplicationServiceMongoRepository extends MongoRepository<ApplicationServiceDocument, String> {

    /**
     * Find application services by owner team ID.
     *
     * @param ownerTeamId the team ID
     * @return list of services owned by the team
     */
    List<ApplicationServiceDocument> findByOwnerTeamId(String ownerTeamId);

    /**
     * Find application services by lifecycle status.
     *
     * @param lifecycle the lifecycle status
     * @return list of services with the lifecycle status
     */
    List<ApplicationServiceDocument> findByLifecycle(String lifecycle);

    /**
     * Find application services by tags containing any of the specified values.
     *
     * @param tags list of tag values to search for
     * @return list of services with matching tags
     */
    @Query("{'tags': {'$in': ?0}}")
    List<ApplicationServiceDocument> findByTagsContainingAny(List<String> tags);

    /**
     * Find application services by display name containing the search term (case-insensitive).
     *
     * @param searchTerm the search term
     * @return list of services with matching display names
     */
    @Query("{'displayName': {'$regex': ?0, '$options': 'i'}}")
    List<ApplicationServiceDocument> findByDisplayNameContainingIgnoreCase(String searchTerm);

    /**
     * Count application services by owner team ID.
     *
     * @param ownerTeamId the team ID
     * @return number of services owned by the team
     */
    long countByOwnerTeamId(String ownerTeamId);

    /**
     * Check if an application service exists by owner team ID and display name.
     *
     * @param ownerTeamId the team ID
     * @param displayName the display name
     * @return true if service exists, false otherwise
     */
    boolean existsByOwnerTeamIdAndDisplayName(String ownerTeamId, String displayName);
}
