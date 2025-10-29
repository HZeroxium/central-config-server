package com.example.control.domain.port;

import com.example.control.domain.object.ApprovalRequest;
import com.example.control.domain.criteria.ApprovalRequestCriteria;
import com.example.control.domain.id.ApprovalRequestId;

import java.util.List;

/**
 * Port (hexagonal architecture) for persisting and querying
 * {@link ApprovalRequest}.
 * <p>
 * Provides CRUD operations for multi-gate approval workflow requests with
 * optimistic locking support to prevent race conditions during concurrent
 * approvals.
 * </p>
 */
public interface ApprovalRequestRepositoryPort
        extends RepositoryPort<ApprovalRequest, ApprovalRequestId, ApprovalRequestCriteria> {

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

    /**
     * Check if there is an existing PENDING request by requester for a service.
     *
     * @param requesterUserId requester user ID
     * @param serviceId       target service ID
     * @return true if such a PENDING request exists
     */
    boolean existsPendingByRequesterAndService(String requesterUserId, String serviceId);

    /**
     * Auto-approve all PENDING requests for the same service and target team.
     * Returns the number of updated documents.
     */
    long cascadeApproveSameTeamPending(String serviceId, String teamId);

    /**
     * Auto-reject all other-team PENDING requests for the same service.
     * Returns the number of updated documents.
     */
    long cascadeRejectOtherTeamsPending(String serviceId, String approvedTeamId, String reason);

    /**
     * Find all PENDING approval requests for a specific service.
     * Used for cascade decision generation.
     *
     * @param serviceId target service ID
     * @return list of PENDING requests for the service
     */
    List<ApprovalRequest> findAllPendingByServiceId(String serviceId);

    /**
     * Find all requests for a service by status.
     * Used after cascade operations to find requests that were actually updated.
     *
     * @param serviceId the service ID
     * @param status    the status to filter by
     * @return list of requests matching the criteria
     */
    List<ApprovalRequest> findAllByServiceIdAndStatus(String serviceId, ApprovalRequest.ApprovalStatus status);
}
