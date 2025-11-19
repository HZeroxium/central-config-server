package com.example.control.application.service.helpers;

import com.example.control.application.command.ApprovalRequestCommandService;
import com.example.control.application.query.ApprovalDecisionQueryService;
import com.example.control.application.query.ApprovalRequestQueryService;
import com.example.control.domain.criteria.ApprovalDecisionCriteria;
import com.example.control.domain.event.ApprovalRequestRejectedEvent;
import com.example.control.domain.valueobject.id.ApprovalRequestId;
import com.example.control.domain.model.ApprovalDecision;
import com.example.control.domain.model.ApprovalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for evaluating approval request status and updating requests
 * accordingly.
 * <p>
 * Handles automatic status transitions based on gate decisions:
 * - Any REJECT decision immediately rejects the request
 * - All gates satisfied triggers approval
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalStatusEvaluator {

  private final ApprovalRequestQueryService approvalRequestQueryService;
  private final ApprovalDecisionQueryService approvalDecisionQueryService;
  private final ApprovalRequestCommandService approvalRequestCommandService;
  private final ApprovalCascadeService approvalCascadeService;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Check if all gates are satisfied and update request status if so.
   * <p>
   * This method is called after each decision to automatically approve or reject
   * requests based on gate decisions. Any REJECT decision immediately rejects the
   * request.
   *
   * @param requestId the request ID
   */
  @Transactional
  public void checkAndUpdateRequestStatus(String requestId) {
    log.debug("Checking if request {} can be approved or should be rejected", requestId);

    ApprovalRequestId requestIdObj = ApprovalRequestId.of(requestId);
    ApprovalRequest request = approvalRequestQueryService.findById(requestIdObj)
        .orElseThrow(() -> new IllegalArgumentException(
            "Approval request not found: " + requestId));

    if (request.getStatus() != ApprovalRequest.ApprovalStatus.PENDING) {
      return; // Request is no longer pending
    }

    // Business logic: Check for ANY REJECT decision - immediate rejection
    for (ApprovalRequest.ApprovalGate gate : request.getRequired()) {
      ApprovalDecisionCriteria rejectCriteria = ApprovalDecisionCriteria.forRequestGateDecision(
          requestIdObj.id(), gate.getGate(), ApprovalDecision.Decision.REJECT);
      long rejectCount = approvalDecisionQueryService.count(rejectCriteria);

      if (rejectCount > 0) {
        log.info("Found rejection for request: {} at gate: {}, rejecting entire request",
            requestId, gate.getGate());
        // Find the first rejection decision to get rejector user ID
        String rejectorUserId = "SYSTEM";
        var rejectDecisions = approvalDecisionQueryService.findAll(rejectCriteria, Pageable.ofSize(1));
        if (!rejectDecisions.isEmpty()) {
          ApprovalDecision rejectDecision = rejectDecisions.getContent().get(0);
          rejectorUserId = rejectDecision.getApproverUserId() != null ? rejectDecision.getApproverUserId() : "SYSTEM";
        }
        rejectRequest(requestId, "Rejected by " + gate.getGate() + " gate", rejectorUserId);
        return;
      }
    }

    // Business logic: No rejections - check if all gates are satisfied for approval
    boolean allGatesSatisfied = true;
    for (ApprovalRequest.ApprovalGate gate : request.getRequired()) {
      ApprovalDecisionCriteria approveCriteria = ApprovalDecisionCriteria.forRequestGateDecision(
          requestIdObj.id(), gate.getGate(), ApprovalDecision.Decision.APPROVE);
      long approveCount = approvalDecisionQueryService.count(approveCriteria);

      if (approveCount < gate.getMinApprovals()) {
        allGatesSatisfied = false;
        break;
      }
    }

    if (allGatesSatisfied) {
      log.info("All gates satisfied for request: {}, approving", requestId);
      approvalCascadeService.handleApproval(requestId);
    }
  }

  /**
   * Reject a request by updating its status with reason.
   *
   * @param requestId     the request ID
   * @param reason        the rejection reason
   * @param rejectorUserId the user ID who rejected the request (can be SYSTEM)
   */
  @Transactional
  public void rejectRequest(String requestId, String reason, String rejectorUserId) {
    ApprovalRequestId requestIdObj = ApprovalRequestId.of(requestId);
    ApprovalRequest request = approvalRequestQueryService.findById(requestIdObj)
        .orElseThrow(() -> new IllegalArgumentException(
            "Approval request not found: " + requestId));

    // Update request status to REJECTED
    boolean updated = approvalRequestCommandService.updateStatusAndVersion(
        requestIdObj,
        ApprovalRequest.ApprovalStatus.REJECTED,
        request.getVersion());

    if (updated) {
      log.info("Successfully rejected request: {} with reason: {}", requestId, reason);
      
      // Publish rejection event for email notification
      eventPublisher.publishEvent(ApprovalRequestRejectedEvent.builder()
          .requestId(requestId)
          .requesterUserId(request.getRequesterUserId())
          .serviceId(request.getTarget().getServiceId())
          .targetTeamId(request.getTarget().getTeamId())
          .rejectorUserId(rejectorUserId)
          .rejectedAt(Instant.now())
          .reason(reason)
          .build());
    } else {
      log.warn("Failed to reject request: {} due to version conflict", requestId);
    }
  }
}
