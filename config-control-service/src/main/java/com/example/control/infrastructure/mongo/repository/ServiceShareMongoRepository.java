package com.example.control.infrastructure.mongo.repository;

import com.example.control.infrastructure.mongo.documents.ServiceShareDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link ServiceShareDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for service sharing ACL.
 * </p>
 */
@Repository
public interface ServiceShareMongoRepository extends MongoRepository<ServiceShareDocument, String> {

    /**
     * Find service shares by service ID.
     *
     * @param serviceId the service ID
     * @return list of shares for the service
     */
    List<ServiceShareDocument> findByServiceId(String serviceId);

    /**
     * Find service shares by grantee type and ID.
     *
     * @param grantToType the grantee type
     * @param grantToId   the grantee ID
     * @return list of shares for the grantee
     */
    List<ServiceShareDocument> findByGrantToTypeAndGrantToId(String grantToType, String grantToId);

    /**
     * Find service shares by service ID and grantee type and ID.
     *
     * @param serviceId   the service ID
     * @param grantToType the grantee type
     * @param grantToId   the grantee ID
     * @return list of matching shares
     */
    List<ServiceShareDocument> findByServiceIdAndGrantToTypeAndGrantToId(String serviceId, String grantToType, String grantToId);

    /**
     * Find service shares by grantee type and ID with optional environment filter.
     *
     * @param grantToType   the grantee type
     * @param grantToId     the grantee ID
     * @param environments  the environments to filter by (null means no filter)
     * @return list of shares for the grantee
     */
    @Query("{'grantToType': ?0, 'grantToId': ?1, '$or': [{'environments': null}, {'environments': {'$in': ?2}}]}")
    List<ServiceShareDocument> findByGranteeWithEnvironmentFilter(String grantToType, String grantToId, List<String> environments);

    /**
     * Check if a service share exists for the given criteria.
     *
     * @param serviceId   the service ID
     * @param grantToType the grantee type
     * @param grantToId   the grantee ID
     * @param environments the environments (null means any environment)
     * @return true if share exists, false otherwise
     */
    @Query("{'serviceId': ?0, 'grantToType': ?1, 'grantToId': ?2, '$or': [{'environments': null}, {'environments': {'$in': ?3}}]}")
    boolean existsByServiceAndGranteeAndEnvironments(String serviceId, String grantToType, String grantToId, List<String> environments);

    /**
     * Find service shares by granted by user ID.
     *
     * @param grantedBy the user ID who created the share
     * @return list of shares created by the user
     */
    List<ServiceShareDocument> findByGrantedBy(String grantedBy);

    /**
     * Find effective permissions for a user on a service.
     * This query finds all shares where the user is either directly granted access
     * or is a member of a team that has been granted access, with strict environment filtering.
     *
     * @param userId      the user ID
     * @param userTeamIds the team IDs the user belongs to
     * @param serviceId   the service ID
     * @param environments the environments to check
     * @return list of shares with effective permissions
     */
    @Query("{'serviceId': ?2, '$and': [{'$or': [{'grantToType': 'USER', 'grantToId': ?0}, {'grantToType': 'TEAM', 'grantToId': {'$in': ?1}}]}, {'$or': [{'environments': null}, {'environments': {'$in': ?3}}]}]}")
    List<ServiceShareDocument> findEffectivePermissions(String userId, List<String> userTeamIds, String serviceId, List<String> environments);
}
