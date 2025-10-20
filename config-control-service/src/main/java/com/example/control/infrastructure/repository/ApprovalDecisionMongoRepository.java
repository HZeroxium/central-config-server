package com.example.control.infrastructure.repository;

import com.example.control.infrastructure.repository.documents.ApprovalDecisionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link ApprovalDecisionDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for approval decisions
 * with compound unique index enforcement.
 * </p>
 */
@Repository
public interface ApprovalDecisionMongoRepository extends MongoRepository<ApprovalDecisionDocument, String> {

    /**
     * Find approval decisions by request ID.
     *
     * @param requestId the approval request ID
     * @return list of decisions for the request
     */
    List<ApprovalDecisionDocument> findByRequestId(String requestId);

    /**
     * Find approval decisions by approver user ID.
     *
     * @param approverUserId the user ID of the approver
     * @return list of decisions made by the user
     */
    List<ApprovalDecisionDocument> findByApproverUserId(String approverUserId);

    /**
     * Find approval decisions by request ID and gate.
     *
     * @param requestId the approval request ID
     * @param gate      the gate name
     * @return list of decisions for the request and gate
     */
    List<ApprovalDecisionDocument> findByRequestIdAndGate(String requestId, String gate);

    /**
     * Find approval decisions by request ID, approver, and gate.
     * This method is used to check for existing decisions and enforce
     * the unique constraint.
     *
     * @param requestId      the approval request ID
     * @param approverUserId the user ID of the approver
     * @param gate           the gate name
     * @return list of matching decisions (should be empty or single item)
     */
    List<ApprovalDecisionDocument> findByRequestIdAndApproverUserIdAndGate(String requestId, String approverUserId, String gate);

    /**
     * Check if a decision exists for the given request, approver, and gate.
     *
     * @param requestId      the approval request ID
     * @param approverUserId the user ID of the approver
     * @param gate           the gate name
     * @return true if decision exists, false otherwise
     */
    boolean existsByRequestIdAndApproverUserIdAndGate(String requestId, String approverUserId, String gate);

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
     * @param decision  the decision type
     * @return number of decisions of the specified type
     */
    long countByRequestIdAndGateAndDecision(String requestId, String gate, String decision);

    /**
     * Find decisions by decision type.
     *
     * @param decision the decision type (APPROVE or REJECT)
     * @return list of decisions of the specified type
     */
    List<ApprovalDecisionDocument> findByDecision(String decision);

    /**
     * Find decisions by request ID and decision type.
     *
     * @param requestId the approval request ID
     * @param decision  the decision type
     * @return list of decisions for the request of the specified type
     */
    List<ApprovalDecisionDocument> findByRequestIdAndDecision(String requestId, String decision);
}
