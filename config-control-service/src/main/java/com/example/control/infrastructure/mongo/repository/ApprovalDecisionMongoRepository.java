package com.example.control.infrastructure.mongo.repository;

import com.example.control.infrastructure.mongo.documents.ApprovalDecisionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

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
}
