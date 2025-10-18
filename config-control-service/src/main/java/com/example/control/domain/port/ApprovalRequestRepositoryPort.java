package com.example.control.domain.port;

import com.example.control.domain.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApprovalRequest}.
 * <p>
 * Provides CRUD operations for multi-gate approval workflow requests with
 * optimistic locking support to prevent race conditions during concurrent approvals.
 * </p>
 */
public interface ApprovalRequestRepositoryPort {

    /**
     * Persist or update an approval request.
     * <p>
     * Uses optimistic locking to prevent concurrent modification conflicts.
     *
     * @param request the approval request to save
     * @return the persisted approval request
     */
    ApprovalRequest save(ApprovalRequest request);

    /**
     * Find an approval request by its unique identifier.
     *
     * @param id the request ID
     * @return optional approval request
     */
    Optional<ApprovalRequest> findById(String id);

    /**
     * List approval requests with filtering and pagination.
     *
     * @param filter   optional filter parameters
     * @param pageable pagination and sorting information
     * @return a page of approval requests
     */
    Page<ApprovalRequest> list(ApprovalRequestFilter filter, Pageable pageable);

    /**
     * Count approval requests by status.
     *
     * @param status the approval status
     * @return number of requests with the given status
     */
    long countByStatus(ApprovalRequest.ApprovalStatus status);

    /**
     * Find pending approval requests for a specific gate.
     *
     * @param gate the gate name (e.g., SYS_ADMIN, LINE_MANAGER)
     * @return list of pending requests requiring approval from the gate
     */
    List<ApprovalRequest> findPendingByGate(String gate);

    /**
     * Find approval requests by requester.
     *
     * @param requesterUserId the user ID of the requester
     * @return list of requests created by the user
     */
    List<ApprovalRequest> findByRequester(String requesterUserId);

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
            String gate
    ) {}
}
