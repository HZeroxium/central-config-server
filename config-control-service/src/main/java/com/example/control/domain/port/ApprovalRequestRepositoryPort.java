package com.example.control.domain.port;

import com.example.control.domain.ApprovalRequest;

import java.time.Instant;
import java.util.List;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApprovalRequest}.
 * <p>
 * Provides CRUD operations for multi-gate approval workflow requests with
 * optimistic locking support to prevent race conditions during concurrent approvals.
 * </p>
 */
public interface ApprovalRequestRepositoryPort extends RepositoryPort<ApprovalRequest, String> {

    /**
     * Count approval requests by status.
     *
     * @param status the approval status
     * @return number of requests with the given status
     */
    long countByStatus(ApprovalRequest.ApprovalStatus status);

    /**
     * Update request status and version for optimistic locking.
     *
     * @param id      the request ID
     * @param status  the new status
     * @param version the expected version for optimistic locking
     * @return true if update successful, false if version conflict
     */
    boolean updateStatusAndVersion(String id, ApprovalRequest.ApprovalStatus status, Integer version);

    /**
     * Filter object for querying approval requests.
     */
    record ApprovalRequestFilter(
            String requesterUserId,
            ApprovalRequest.ApprovalStatus status,
            ApprovalRequest.RequestType requestType,
            Instant fromDate,
            Instant toDate,
            String gate,
            List<String> userTeamIds
    ) {}
}
