package com.example.control.seeding.factory;

import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.object.ApprovalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Factory for generating realistic {@link ApprovalDecision} mock data.
 * <p>
 * Generates approval decisions corresponding to approval requests with
 * realistic timestamps, notes, and approver information.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Approver: Admin user ID from configuration</li>
 * <li>Gate: Matches request's required gates</li>
 * <li>Decision: APPROVE or REJECT based on request status</li>
 * <li>Timestamps: After request creation, before request update</li>
 * <li>Notes: Realistic approval/rejection explanations</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalDecisionFactory {

  private final Faker faker;

  /**
   * Approval notes templates.
   */
  private static final List<String> APPROVAL_NOTES = List.of(
      "Request approved. Team has demonstrated capability to manage this service.",
      "Approved based on team's track record and operational readiness.",
      "Ownership transfer approved. Team is well-positioned for this responsibility.",
      "Request granted. Please ensure proper documentation and handover.",
      "Approved. Team has met all requirements for service ownership.",
      "Request approved after reviewing team's capacity and expertise.");

  /**
   * Rejection notes templates.
   */
  private static final List<String> REJECTION_NOTES = List.of(
      "Request rejected. Team needs to demonstrate more experience first.",
      "Rejected due to insufficient operational capacity in the target team.",
      "Request denied. Service requires specialized knowledge not present in requesting team.",
      "Rejected. Please resubmit after addressing the noted concerns.",
      "Request declined. Current team structure doesn't align with service requirements.",
      "Rejected pending clarification on support and maintenance plans.");

  /**
   * Generates an {@link ApprovalDecision} for an approval request.
   * <p>
   * <strong>Note on Optimistic Locking:</strong> This method accepts individual
   * parameters
   * rather than the full ApprovalRequest object to avoid holding references to
   * entities
   * that have been saved and had their version field modified. This prevents
   * OptimisticLockingFailureException when the same request object is reused
   * elsewhere.
   * </p>
   *
   * @param requestId     approval request ID (as string, not the whole object)
   * @param requestStatus approval request status (to determine APPROVE/REJECT)
   * @param createdAt     request creation timestamp
   * @param updatedAt     request update timestamp
   * @param adminUserId   admin user ID making the decision
   * @param gate          gate name for this decision
   * @return generated approval decision
   */
  public ApprovalDecision generate(
      String requestId,
      ApprovalRequest.ApprovalStatus requestStatus,
      Instant createdAt,
      Instant updatedAt,
      String adminUserId,
      String gate) {

    ApprovalDecision.Decision decision = determineDecision(requestStatus);

    Instant decidedAt = generateDecidedAt(createdAt, updatedAt);

    String note = generateNote(decision);

    log.debug("Generated approval decision: request={} gate={} decision={}",
        requestId, gate, decision);

    return ApprovalDecision.builder()
        .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
        .requestId(ApprovalRequestId.of(requestId))
        .approverUserId(adminUserId)
        .gate(gate)
        .decision(decision)
        .decidedAt(decidedAt)
        .note(note)
        .build();
  }

  /**
   * Determines decision type based on request status.
   *
   * @param status approval request status
   * @return approval decision
   */
  private ApprovalDecision.Decision determineDecision(ApprovalRequest.ApprovalStatus status) {
    return switch (status) {
      case APPROVED -> ApprovalDecision.Decision.APPROVE;
      case REJECTED -> ApprovalDecision.Decision.REJECT;
      default -> ApprovalDecision.Decision.APPROVE; // Default to approve for pending (shouldn't happen)
    };
  }

  /**
   * Generates decision timestamp between request creation and update.
   *
   * @param createdAt request creation timestamp
   * @param updatedAt request update timestamp
   * @return decision instant
   */
  private Instant generateDecidedAt(Instant createdAt, Instant updatedAt) {
    // Decision timestamp should be between creation and update
    long secondsBetween = ChronoUnit.SECONDS.between(createdAt, updatedAt);

    if (secondsBetween <= 0) {
      // If same time, add a small offset
      return createdAt.plus(faker.number().numberBetween(1, 60), ChronoUnit.MINUTES);
    }

    // Random time between creation and update
    long randomSeconds = faker.number().numberBetween(0, secondsBetween);
    return createdAt.plus(randomSeconds, ChronoUnit.SECONDS);
  }

  /**
   * Generates decision note based on decision type.
   *
   * @param decision decision type
   * @return decision note
   */
  private String generateNote(ApprovalDecision.Decision decision) {
    List<String> templates = (decision == ApprovalDecision.Decision.APPROVE)
        ? APPROVAL_NOTES
        : REJECTION_NOTES;

    return templates.get(faker.random().nextInt(templates.size()));
  }

  /**
   * Generates a system-generated approval decision.
   * <p>
   * Used for cascade operations where the system automatically approves or rejects
   * requests based on service ownership assignment.
   * </p>
   *
   * @param requestId approval request ID (as string)
   * @param gate gate name for this decision
   * @param decision decision type (APPROVE or REJECT)
   * @param note the reason/note for the decision
   * @return generated system decision
   */
  public ApprovalDecision generateSystemDecision(
      String requestId,
      String gate,
      ApprovalDecision.Decision decision,
      String note) {

    log.debug("Generated system decision for request: {}, gate: {}, decision: {}",
        requestId, gate, decision);

    return ApprovalDecision.builder()
        .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
        .requestId(ApprovalRequestId.of(requestId))
        .approverUserId("SYSTEM")
        .gate(gate)
        .decision(decision)
        .decidedAt(Instant.now())
        .note(note)
        .build();
  }
}
