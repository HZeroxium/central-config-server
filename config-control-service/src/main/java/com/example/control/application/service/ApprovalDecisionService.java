package com.example.control.application.service;

import com.example.control.application.command.ApprovalDecisionCommandService;
import com.example.control.application.query.ApprovalDecisionQueryService;
import com.example.control.domain.criteria.ApprovalDecisionCriteria;
import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.object.ApprovalDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrator service for managing approval decisions.
 * <p>
 * Coordinates between CommandService and QueryService for approval decision
 * operations.
 * Handles business logic and orchestration but delegates persistence to
 * Command/Query services.
 * Provides special logic for system-generated decisions used in cascade
 * approval scenarios.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalDecisionService {

    private final ApprovalDecisionCommandService commandService;
    private final ApprovalDecisionQueryService queryService;

    /**
     * Save an approval decision.
     * <p>
     * Delegates to CommandService for persistence.
     *
     * @param decision the approval decision to save
     * @return the saved decision
     */
    @Transactional
    public ApprovalDecision save(ApprovalDecision decision) {
        log.debug("Orchestrating save for approval decision: {}", decision.getId());
        return commandService.save(decision);
    }

    /**
     * Find approval decision by ID.
     *
     * @param id the decision ID
     * @return the approval decision if found
     */
    public Optional<ApprovalDecision> findById(ApprovalDecisionId id) {
        return queryService.findById(id);
    }

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
    public boolean existsByRequestAndApproverAndGate(ApprovalRequestId requestId,
                                                     String approverUserId,
                                                     String gate) {
        ApprovalDecisionCriteria criteria = ApprovalDecisionCriteria.forRequestApproverGate(
                requestId.id(), approverUserId, gate);
        return queryService.count(criteria) > 0;
    }

    /**
     * Count decisions by decision type for a specific request and gate.
     *
     * @param requestId the approval request ID
     * @param gate      the gate name
     * @param decision  the decision type (APPROVE or REJECT)
     * @return number of decisions of the specified type
     */
    public long countByRequestIdAndGateAndDecision(ApprovalRequestId requestId,
                                                   String gate,
                                                   ApprovalDecision.Decision decision) {
        ApprovalDecisionCriteria criteria = ApprovalDecisionCriteria.forRequestGateDecision(
                requestId.id(), gate, decision);
        return queryService.count(criteria);
    }

    /**
     * Check if an approval decision exists.
     *
     * @param id the decision ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(ApprovalDecisionId id) {
        return queryService.existsById(id);
    }

    /**
     * Delete an approval decision by ID.
     * Delegates to CommandService.
     *
     * @param id the decision ID
     */
    @Transactional
    public void deleteById(ApprovalDecisionId id) {
        log.debug("Orchestrating delete for approval decision: {}", id);
        commandService.deleteById(id);
    }

    /**
     * Create a system-generated approval decision.
     * <p>
     * Business logic for cascade operations where the system automatically approves
     * or rejects
     * requests based on service ownership assignment.
     * Builds the domain object with system defaults and delegates to
     * CommandService.
     * </p>
     *
     * @param requestId the approval request ID
     * @param gate      the gate name
     * @param decision  the decision type (APPROVE or REJECT)
     * @param note      the reason/note for the decision
     * @return the created system decision
     */
    @Transactional
    public ApprovalDecision createSystemDecision(ApprovalRequestId requestId,
                                                 String gate,
                                                 ApprovalDecision.Decision decision,
                                                 String note) {
        log.debug("Creating system decision for request: {}, gate: {}, decision: {}",
                requestId, gate, decision);

        // Build system decision domain object
        ApprovalDecision systemDecision = ApprovalDecision.builder()
                .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
                .requestId(requestId)
                .approverUserId("SYSTEM")
                .gate(gate)
                .decision(decision)
                .decidedAt(Instant.now())
                .note(note)
                .build();

        ApprovalDecision saved = commandService.save(systemDecision);
        log.info("Created system decision: {} for request: {}", saved.getId(), requestId);
        return saved;
    }
}
