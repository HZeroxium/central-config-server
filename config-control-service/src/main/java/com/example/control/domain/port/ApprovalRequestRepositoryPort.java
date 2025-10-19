package com.example.control.domain.port;

import com.example.control.domain.ApprovalRequest;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.criteria.ApprovalRequestCriteria;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApprovalRequest}.
 * <p>
 * Provides CRUD operations for multi-gate approval workflow requests with
 * optimistic locking support to prevent race conditions during concurrent approvals.
 * </p>
 */
public interface ApprovalRequestRepositoryPort extends RepositoryPort<ApprovalRequest, ApprovalRequestId, ApprovalRequestCriteria> {

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
    boolean updateStatusAndVersion(ApprovalRequestId id, ApprovalRequest.ApprovalStatus status, Integer version);
}
