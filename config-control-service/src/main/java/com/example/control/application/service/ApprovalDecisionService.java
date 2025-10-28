package com.example.control.application.service;

import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.port.ApprovalDecisionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing approval decisions with caching.
 * <p>
 * Provides CRUD operations for approval decisions with caching
 * for performance optimization.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalDecisionService {

    private final ApprovalDecisionRepositoryPort repository;

    /**
     * Save an approval decision.
     * <p>
     * Evicts cache entries to ensure consistency.
     *
     * @param decision the approval decision to save
     * @return the saved decision
     */
    @Transactional
    @CacheEvict(value = "approval-decisions", allEntries = true)
    public ApprovalDecision save(ApprovalDecision decision) {
        log.debug("Saving approval decision: {}", decision.getId());
        ApprovalDecision saved = repository.save(decision);
        log.debug("Saved approval decision: {}", saved.getId());
        return saved;
    }

    /**
     * Find approval decision by ID.
     *
     * @param id the decision ID
     * @return the approval decision if found
     */
    @Cacheable(value = "approval-decisions", key = "#id")
    public Optional<ApprovalDecision> findById(ApprovalDecisionId id) {
        log.debug("Finding approval decision by ID: {}", id);
        Optional<ApprovalDecision> result = repository.findById(id);
        log.debug("Found approval decision: {}", result.isPresent());
        return result;
    }

    /**
     * Find all decisions for a specific approval request.
     *
     * @param requestId the approval request ID
     * @return list of decisions for the request
     */

    /**
     * Check if a decision exists for a specific request, approver, and gate.
     * <p>
     * This enforces the constraint that each user can only make one decision
     * per request per gate.
     *
     * @param requestId the approval request ID
     * @param approverUserId the user ID of the approver
     * @param gate the gate name
     * @return true if decision exists, false otherwise
     */
    @Cacheable(value = "approval-decisions", key = "'exists:' + #requestId + ':' + #approverUserId + ':' + #gate")
    public boolean existsByRequestAndApproverAndGate(ApprovalRequestId requestId, 
                                                     String approverUserId, 
                                                     String gate) {
        log.debug("Checking if decision exists for request: {}, approver: {}, gate: {}", 
                requestId, approverUserId, gate);
        boolean exists = repository.existsByRequestAndApproverAndGate(requestId, approverUserId, gate);
        log.debug("Decision exists: {}", exists);
        return exists;
    }

    /**
     * Count decisions by decision type for a specific request and gate.
     *
     * @param requestId the approval request ID
     * @param gate the gate name
     * @param decision the decision type (APPROVE or REJECT)
     * @return number of decisions of the specified type
     */
    @Cacheable(value = "approval-decisions", key = "'count-by-request-gate-decision:' + #requestId + ':' + #gate + ':' + #decision")
    public long countByRequestIdAndGateAndDecision(ApprovalRequestId requestId, 
                                                   String gate, 
                                                   ApprovalDecision.Decision decision) {
        log.debug("Counting decisions for request: {}, gate: {}, decision: {}", 
                requestId, gate, decision);
        long count = repository.countByRequestIdAndGateAndDecision(requestId, gate, decision);
        log.debug("Found {} decisions for request: {}, gate: {}, decision: {}", 
                count, requestId, gate, decision);
        return count;
    }

    /**
     * Check if an approval decision exists.
     *
     * @param id the decision ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(ApprovalDecisionId id) {
        log.debug("Checking existence of approval decision: {}", id);
        boolean exists = repository.existsById(id);
        log.debug("Approval decision exists: {}", exists);
        return exists;
    }

    /**
     * Delete an approval decision by ID.
     *
     * @param id the decision ID
     */
    @Transactional
    @CacheEvict(value = "approval-decisions", allEntries = true)
    public void deleteById(ApprovalDecisionId id) {
        log.debug("Deleting approval decision: {}", id);
        repository.deleteById(id);
        log.debug("Deleted approval decision: {}", id);
    }

    /**
     * Create a system-generated approval decision.
     * <p>
     * Used for cascade operations where the system automatically approves or rejects
     * requests based on service ownership assignment.
     * </p>
     *
     * @param requestId the approval request ID
     * @param gate the gate name
     * @param decision the decision type (APPROVE or REJECT)
     * @param note the reason/note for the decision
     * @return the created system decision
     */
    @Transactional
    @CacheEvict(value = "approval-decisions", allEntries = true)
    public ApprovalDecision createSystemDecision(ApprovalRequestId requestId,
                                                 String gate,
                                                 ApprovalDecision.Decision decision,
                                                 String note) {
        log.debug("Creating system decision for request: {}, gate: {}, decision: {}",
                requestId, gate, decision);

        ApprovalDecision systemDecision = ApprovalDecision.builder()
                .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
                .requestId(requestId)
                .approverUserId("SYSTEM")
                .gate(gate)
                .decision(decision)
                .decidedAt(Instant.now())
                .note(note)
                .build();

        ApprovalDecision saved = repository.save(systemDecision);
        log.info("Created system decision: {} for request: {}", saved.getId(), requestId);
        return saved;
    }
}
