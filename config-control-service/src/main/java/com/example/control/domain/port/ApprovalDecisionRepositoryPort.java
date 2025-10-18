package com.example.control.domain.port;

import com.example.control.domain.ApprovalDecision;

import java.util.List;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApprovalDecision}.
 * <p>
 * Provides CRUD operations for individual approval/rejection decisions within
 * the multi-gate approval workflow. Each user can only make one decision per
 * request per gate (enforced by unique constraint).
 * </p>
 */
public interface ApprovalDecisionRepositoryPort extends RepositoryPort<ApprovalDecision, String> {


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
     * Filter object for querying approval decisions.
     */
    record ApprovalDecisionFilter(
            String requestId,
            String approverUserId,
            String gate,
            ApprovalDecision.Decision decision,
            List<String> userTeamIds
    ) {}
}
