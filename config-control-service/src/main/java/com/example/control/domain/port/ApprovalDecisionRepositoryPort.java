package com.example.control.domain.port;

import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.criteria.ApprovalDecisionCriteria;
import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;

/**
 * Port (hexagonal architecture) for persisting and querying
 * {@link ApprovalDecision}.
 * <p>
 * Provides CRUD operations for individual approval/rejection decisions within
 * the multi-gate approval workflow. Each user can only make one decision per
 * request per gate (enforced by unique constraint).
 * </p>
 */
public interface ApprovalDecisionRepositoryPort
        extends RepositoryPort<ApprovalDecision, ApprovalDecisionId, ApprovalDecisionCriteria> {

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
    boolean existsByRequestAndApproverAndGate(ApprovalRequestId requestId, String approverUserId, String gate);

    /**
     * Count decisions for a specific request and gate.
     *
     * @param requestId the approval request ID
     * @param gate      the gate name
     * @return number of decisions for the request and gate
     */
    long countByRequestIdAndGate(ApprovalRequestId requestId, String gate);

    /**
     * Count decisions by decision type for a specific request and gate.
     *
     * @param requestId the approval request ID
     * @param gate      the gate name
     * @param decision  the decision type (APPROVE or REJECT)
     * @return number of decisions of the specified type
     */
    long countByRequestIdAndGateAndDecision(ApprovalRequestId requestId, String gate,
                                            ApprovalDecision.Decision decision);
}
