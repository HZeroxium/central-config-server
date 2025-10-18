package com.example.control.domain.port;

import com.example.control.domain.ApprovalDecision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApprovalDecision}.
 * <p>
 * Provides CRUD operations for individual approval/rejection decisions within
 * the multi-gate approval workflow. Each user can only make one decision per
 * request per gate (enforced by unique constraint).
 * </p>
 */
public interface ApprovalDecisionRepositoryPort {

    /**
     * Persist or update an approval decision.
     *
     * @param decision the approval decision to save
     * @return the persisted approval decision
     */
    ApprovalDecision save(ApprovalDecision decision);

    /**
     * Find an approval decision by its unique identifier.
     *
     * @param id the decision ID
     * @return optional approval decision
     */
    Optional<ApprovalDecision> findById(String id);

    /**
     * Find all decisions for a specific approval request.
     *
     * @param requestId the approval request ID
     * @return list of decisions for the request
     */
    List<ApprovalDecision> findByRequestId(String requestId);

    /**
     * Find all decisions made by a specific approver.
     *
     * @param approverUserId the user ID of the approver
     * @return list of decisions made by the user
     */
    List<ApprovalDecision> findByApprover(String approverUserId);

    /**
     * Find decisions for a specific request and gate.
     *
     * @param requestId the approval request ID
     * @param gate      the gate name
     * @return list of decisions for the request and gate
     */
    List<ApprovalDecision> findByRequestIdAndGate(String requestId, String gate);

    /**
     * Check if a decision exists for a specific request, approver, and gate.
     * <p>
     * This enforces the constraint that each user can only make one decision
     * per request per gate.
     *
     * @param requestId      the approval request ID
     * @param approverUserId the user ID of the approver
     * @param gate           the gate name
     * @return true if decision exists, false otherwise
     */
    boolean existsByRequestAndApproverAndGate(String requestId, String approverUserId, String gate);

    /**
     * Count decisions for a specific request and gate.
     *
     * @param requestId the approval request ID
     * @param gate      the gate name
     * @return number of decisions for the request and gate
     */
    long countByRequestIdAndGate(String requestId, String gate);

    /**
     * Count decisions by decision type for a specific request and gate.
     *
     * @param requestId the approval request ID
     * @param gate      the gate name
     * @param decision  the decision type (APPROVE or REJECT)
     * @return number of decisions of the specified type
     */
    long countByRequestIdAndGateAndDecision(String requestId, String gate, ApprovalDecision.Decision decision);

    /**
     * List approval decisions with filtering and pagination.
     *
     * @param filter   optional filter parameters
     * @param pageable pagination and sorting information
     * @return a page of approval decisions
     */
    Page<ApprovalDecision> list(ApprovalDecisionFilter filter, Pageable pageable);

    /**
     * Filter object for querying approval decisions.
     */
    record ApprovalDecisionFilter(
            String requestId,
            String approverUserId,
            String gate,
            ApprovalDecision.Decision decision
    ) {}
}
