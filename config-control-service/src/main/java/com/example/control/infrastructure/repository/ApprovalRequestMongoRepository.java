package com.example.control.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import com.example.control.infrastructure.repository.documents.ApprovalRequestDocument;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data MongoDB repository for {@link ApprovalRequestDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for approval workflow requests
 * with optimistic locking support.
 * </p>
 */
@Repository
public interface ApprovalRequestMongoRepository extends MongoRepository<ApprovalRequestDocument, String> {

    /**
     * Find approval requests by requester user ID.
     *
     * @param requesterUserId the user ID of the requester
     * @return list of requests created by the user
     */
    List<ApprovalRequestDocument> findByRequesterUserId(String requesterUserId);

    /**
     * Find approval requests by status.
     *
     * @param status the approval status
     * @return list of requests with the status
     */
    List<ApprovalRequestDocument> findByStatus(String status);

    /**
     * Find approval requests by request type.
     *
     * @param requestType the request type
     * @return list of requests of the type
     */
    List<ApprovalRequestDocument> findByRequestType(String requestType);

    /**
     * Find pending approval requests that contain a specific gate in their required gates.
     *
     * @param gate the gate name
     * @return list of pending requests requiring approval from the gate
     */
    @Query("{'status': 'PENDING', 'requiredGatesJson': {'$regex': ?0, '$options': 'i'}}")
    List<ApprovalRequestDocument> findPendingByGate(String gate);

    /**
     * Find approval requests created within a date range.
     *
     * @param fromDate the start date (inclusive)
     * @param toDate   the end date (inclusive)
     * @return list of requests created within the range
     */
    List<ApprovalRequestDocument> findByCreatedAtBetween(java.time.Instant fromDate, java.time.Instant toDate);

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
    long updateStatusAndVersion(String id, String status, Integer version, java.time.Instant updatedAt, Integer newVersion);
}
