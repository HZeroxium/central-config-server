package com.example.control.infrastructure.mongo.repository;

import com.example.control.infrastructure.mongo.documents.ApprovalRequestDocument;

import java.time.Instant;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for {@link ApprovalRequestDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for approval workflow
 * requests
 * with optimistic locking support.
 * </p>
 */
@Repository
public interface ApprovalRequestMongoRepository extends MongoRepository<ApprovalRequestDocument, String> {

    /**
     * Count approval requests by status.
     *
     * @param status the approval status
     * @return number of requests with the status
     */
    long countByStatus(String status);

    /**
     * Update request status and version for optimistic locking.
     * This method uses MongoDB's atomic update operation to ensure
     * the version field is checked and updated atomically.
     *
     * @param id      the request ID
     * @param status  the new status
     * @param version the expected version
     * @return number of updated documents (1 if successful, 0 if version conflict)
     */
    @Query("{'_id': ?0, 'version': ?2}")
    @Update("{'$set': {'status': ?1, 'updatedAt': ?3, 'version': ?4}}")
    long updateStatusAndVersion(String id, String status, Integer version, Instant updatedAt, Integer newVersion);

    /**
     * Check existence of a pending request for a requester and service.
     */
    boolean existsByRequesterUserIdAndTargetServiceIdAndStatus(String requesterUserId, String targetServiceId,
            String status);

    /**
     * Cascade approve: set status APPROVED where PENDING and matches same team and
     * service.
     */
    @Query(value = "{'targetServiceId': ?0, 'targetTeamId': ?1, 'status': 'PENDING'}")
    @Update("{'$set': {'status': 'APPROVED', 'updatedAt': ?2}, '$inc': {'version': 1}}")
    long cascadeApproveSameTeamPending(String serviceId, String teamId, Instant updatedAt);

    /**
     * Cascade reject: set status REJECTED where PENDING, same service, and
     * different team.
     */
    @Query(value = "{'targetServiceId': ?0, 'targetTeamId': {'$ne': ?1}, 'status': 'PENDING'}")
    @Update("{'$set': {'status': 'REJECTED', 'updatedAt': ?2, 'cancelReason': ?3}, '$inc': {'version': 1}}")
    long cascadeRejectOtherTeamsPending(String serviceId, String approvedTeamId, Instant updatedAt, String reason);
}
