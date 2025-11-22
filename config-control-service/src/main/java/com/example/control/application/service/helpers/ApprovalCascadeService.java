package com.example.control.application.service.helpers;

import com.example.control.application.command.ApplicationServiceCommandService;
import com.example.control.application.command.ApprovalDecisionCommandService;
import com.example.control.application.command.ApprovalRequestCommandService;
import com.example.control.application.command.DriftEventCommandService;
import com.example.control.application.command.ServiceInstanceCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.ApprovalDecisionQueryService;
import com.example.control.application.query.ApprovalRequestQueryService;
import com.example.control.application.service.ConfigGitService;
import com.example.control.application.service.ServiceCredentialService;
import com.example.control.domain.criteria.ApprovalRequestCriteria;
import com.example.control.domain.event.ServiceOwnershipTransferred;
import com.example.control.domain.event.ApprovalRequestApprovedEvent;
import com.example.control.domain.event.ApprovalRequestRejectedEvent;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.ApprovalDecisionId;
import com.example.control.domain.valueobject.id.ApprovalRequestId;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.ApprovalDecision;
import com.example.control.domain.model.ApprovalRequest;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling cascade operations in approval workflow.
 * <p>
 * Handles cascade approval/rejection and system decision generation when
 * a service ownership is transferred.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalCascadeService {

  private final ApprovalRequestQueryService approvalRequestQueryService;
  private final ApprovalRequestCommandService approvalRequestCommandService;
  private final ApprovalDecisionCommandService approvalDecisionCommandService;
  private final ApprovalDecisionQueryService approvalDecisionQueryService;
  private final ApplicationServiceCommandService applicationServiceCommandService;
  private final ApplicationServiceQueryService applicationServiceQueryService;
  private final ServiceInstanceCommandService serviceInstanceCommandService;
  private final DriftEventCommandService driftEventCommandService;
  private final ApplicationEventPublisher eventPublisher;
  private final ServiceCredentialService serviceCredentialService;
  private final ConfigGitService configGitService;

  // Thread-local storage for credentials created during approval (for response)
  private static final ThreadLocal<ServiceCredentialService.ServiceCredentialResponse> credentialsHolder = new ThreadLocal<>();

  /**
   * Handle approval of a request: transfer ownership and cascade operations.
   *
   * @param requestId the request ID to approve
   */
  @Transactional
  public void handleApproval(String requestId) {
    ApprovalRequestId requestIdObj = ApprovalRequestId.of(requestId);
    ApprovalRequest request = approvalRequestQueryService.findById(requestIdObj)
        .orElseThrow(() -> new IllegalArgumentException(
            "Approval request not found: " + requestId));

    // Update request status with optimistic locking
    boolean updated = approvalRequestCommandService.updateStatusAndVersion(
        requestIdObj,
        ApprovalRequest.ApprovalStatus.APPROVED,
        request.getVersion());

    if (!updated) {
      // Loser of race: mark rejected with conflict note
      ApprovalRequest loser = request.toBuilder()
          .status(ApprovalRequest.ApprovalStatus.REJECTED)
          .cancelReason("conflict – already assigned")
          .updatedAt(Instant.now())
          .build();
      approvalRequestCommandService.save(loser);
      log.info("Request {} rejected due to ownership conflict", requestId);
      return;
    }

    // Business logic: Transfer service ownership
    String serviceId = request.getTarget().getServiceId();
    String newTeamId = request.getTarget().getTeamId();

    log.info("Orchestrating ownership transfer of service {} to team {}", serviceId, newTeamId);

    // 1. Update ApplicationService ownership
    ApplicationService service = applicationServiceQueryService.findById(ApplicationServiceId.of(serviceId))
        .orElseThrow(() -> new IllegalArgumentException(
            "Application service not found: " + serviceId));

    String oldTeamId = service.getOwnerTeamId();
    service.setOwnerTeamId(newTeamId);
    service.setUpdatedAt(Instant.now());

    applicationServiceCommandService.save(service);

    // 2. Cascade updates to related entities
    log.debug("Cascading teamId update to service instances and drift events for service: {}", serviceId);
    serviceInstanceCommandService.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
    driftEventCommandService.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);

    // 3. Publish ownership transfer event
    eventPublisher.publishEvent(ServiceOwnershipTransferred.builder()
        .serviceId(serviceId)
        .oldTeamId(oldTeamId)
        .newTeamId(newTeamId)
        .transferredAt(Instant.now())
        .build());

    log.info("Successfully transferred ownership of service {} to team {}", serviceId, newTeamId);

    // 3.5. Create service credentials after ownership transfer
    // This must happen within the same transaction - if Keycloak client creation fails,
    // the entire approval transaction will rollback
    ServiceCredentialService.ServiceCredentialResponse credentialsResponse = null;
    try {
      UserContext systemContext = UserContext.builder()
          .userId("SYSTEM")
          .username("system")
          .roles(List.of("SYS_ADMIN"))
          .teamIds(List.of())
          .build();
      
      credentialsResponse = serviceCredentialService.createCredentialsForService(
          ApplicationServiceId.of(serviceId), systemContext);
      // Store credentials in thread-local for retrieval by ApprovalService
      credentialsHolder.set(credentialsResponse);
      log.info("Created service credentials for service: {} (status: PENDING)", serviceId);
    } catch (Exception e) {
      log.error("Failed to create service credentials for service: {}", serviceId, e);
      // Re-throw to trigger transaction rollback
      throw new RuntimeException("Failed to create service credentials: " + e.getMessage(), e);
    } finally {
      // Clean up thread-local after approval completes
      // Note: This will be cleared after ApprovalService retrieves credentials
    }

    // 3.6. Initialize Git repository directory and template config file
    // This is non-blocking - if Git operations fail, approval still succeeds
    // User can manually create config files later if needed
    try {
      String displayName = service.getDisplayName();
      List<String> environments = service.getEnvironments() != null 
          ? service.getEnvironments() 
          : List.of("dev", "staging", "prod");
      
      configGitService.initializeServiceDirectory(serviceId, displayName, environments);
      log.info("Initialized Git directory and template config file for service: {}", serviceId);
    } catch (Exception e) {
      // Log warning but don't fail approval - Git operations are non-critical
      log.warn("Failed to initialize Git directory for service: {}: {}. " +
              "Approval succeeded, but user will need to create config files manually.",
              serviceId, e.getMessage(), e);
      // Don't re-throw - allow approval to complete successfully
    }

    // 4. Cascade: Update approval request statuses in bulk
    long approvedAuto = approvalRequestCommandService.cascadeApproveSameTeamPending(serviceId, newTeamId);
    long rejectedAuto = approvalRequestCommandService.cascadeRejectOtherTeamsPending(serviceId, newTeamId,
        "auto-rejected: service ownership assigned to another team");

    log.info("Cascade results for service {}: autoApproved={}, autoRejected={}",
        serviceId, approvedAuto, rejectedAuto);

    // 5. Optimized: Fetch cascaded requests using direct query by serviceId and
    // status
    List<ApprovalRequest> cascadedApprovedRequests = new ArrayList<>();
    List<ApprovalRequest> cascadedRejectedRequests = new ArrayList<>();

    if (approvedAuto > 0) {
      // Use optimized query: directly fetch APPROVED requests for this service and
      // team
      cascadedApprovedRequests = approvalRequestQueryService.findAll(
          ApprovalRequestCriteria.forServiceIdAndStatusAndTeamId(serviceId,
              ApprovalRequest.ApprovalStatus.APPROVED, newTeamId),
          org.springframework.data.domain.Pageable.unpaged())
          .getContent();
      log.debug("Found {} APPROVED requests for service {} and team {} after cascade",
          cascadedApprovedRequests.size(), serviceId, newTeamId);
    }

    if (rejectedAuto > 0) {
      // Use optimized query: directly fetch REJECTED requests for this service
      // excluding the approved team
      cascadedRejectedRequests = approvalRequestQueryService.findAll(
          ApprovalRequestCriteria.forServiceIdAndStatusExcludingTeamId(serviceId,
              ApprovalRequest.ApprovalStatus.REJECTED, newTeamId),
          org.springframework.data.domain.Pageable.unpaged())
          .getContent();
      log.debug("Found {} REJECTED requests for service {} (other teams) after cascade",
          cascadedRejectedRequests.size(), serviceId);
    }

    // 6. Create system decisions for cascaded requests
    createSystemDecisionsForCascadedRequests(cascadedApprovedRequests, cascadedRejectedRequests, newTeamId);

    log.info("Successfully approved request: {} and transferred service ownership", requestId);

    // 7. Publish approval event for email notification
    eventPublisher.publishEvent(ApprovalRequestApprovedEvent.builder()
        .requestId(requestId)
        .requesterUserId(request.getRequesterUserId())
        .serviceId(serviceId)
        .targetTeamId(newTeamId)
        .approverUserId(getApproverUserId(request))
        .approvedAt(Instant.now())
        .build());

    // 8. Publish rejection events for cascade-rejected requests
    String rejectionReason = "auto-rejected: service ownership assigned to another team";
    for (ApprovalRequest rejectedRequest : cascadedRejectedRequests) {
      eventPublisher.publishEvent(ApprovalRequestRejectedEvent.builder()
          .requestId(rejectedRequest.getId().id())
          .requesterUserId(rejectedRequest.getRequesterUserId())
          .serviceId(serviceId)
          .targetTeamId(rejectedRequest.getTarget().getTeamId())
          .rejectorUserId("SYSTEM")
          .rejectedAt(Instant.now())
          .reason(rejectionReason)
          .build());
    }
  }

  /**
   * Gets the approver user ID from the approval decisions.
   * Returns the first human approver (not SYSTEM), or "SYSTEM" if no human
   * approver found.
   *
   * @param request the approval request
   * @return approver user ID
   */
  private String getApproverUserId(ApprovalRequest request) {
    // Fetch approval decisions for this request
    var decisions = approvalDecisionQueryService.findAll(
        com.example.control.domain.criteria.ApprovalDecisionCriteria.forRequest(request.getId().id()),
        org.springframework.data.domain.Pageable.unpaged()).getContent();

    // Find the first human approver (not SYSTEM)
    return decisions.stream()
        .filter(decision -> !"SYSTEM".equals(decision.getApproverUserId()))
        .map(ApprovalDecision::getApproverUserId)
        .findFirst()
        .orElse("SYSTEM");
  }

  /**
   * Create system-generated approval decisions for all cascaded requests.
   * <p>
   * For requests that were cascade-approved (same team), create APPROVE decisions
   * for all gates.
   * For requests that were cascade-rejected (other teams), create REJECT decision
   * for SYS_ADMIN gate.
   * </p>
   *
   * @param cascadedApprovedRequests list of requests that were cascade-approved
   * @param cascadedRejectedRequests list of requests that were cascade-rejected
   * @param approvedTeamId           the team ID that won ownership
   */
  @Transactional
  public void createSystemDecisionsForCascadedRequests(List<ApprovalRequest> cascadedApprovedRequests,
      List<ApprovalRequest> cascadedRejectedRequests,
      String approvedTeamId) {
    // Process cascade-approved requests (same team)
    for (ApprovalRequest request : cascadedApprovedRequests) {
      // Refresh request from database to ensure we have latest status
      ApprovalRequest refreshedRequest = approvalRequestQueryService.findById(request.getId())
          .orElse(null);

      if (refreshedRequest == null) {
        log.warn("Request {} not found when creating cascade decisions, skipping",
            request.getId());
        continue;
      }

      // Verify request status matches expected cascade result
      if (refreshedRequest.getStatus() != ApprovalRequest.ApprovalStatus.APPROVED) {
        log.warn("Request {} has unexpected status {} (expected APPROVED), skipping decision creation",
            refreshedRequest.getId(), refreshedRequest.getStatus());
        continue;
      }

      // Verify request was for the same team
      if (!approvedTeamId.equals(refreshedRequest.getTarget().getTeamId())) {
        log.warn("Request {} teamId {} doesn't match approved teamId {}, skipping decision creation",
            refreshedRequest.getId(), refreshedRequest.getTarget().getTeamId(),
            approvedTeamId);
        continue;
      }

      // Create APPROVE decision for all gates
      for (ApprovalRequest.ApprovalGate gate : refreshedRequest.getRequired()) {
        ApprovalDecision systemDecision = ApprovalDecision.builder()
            .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
            .requestId(refreshedRequest.getId())
            .approverUserId("SYSTEM")
            .gate(gate.getGate())
            .decision(ApprovalDecision.Decision.APPROVE)
            .decidedAt(Instant.now())
            .note("Auto-approved: service ownership assigned to team "
                + approvedTeamId)
            .build();
        approvalDecisionCommandService.save(systemDecision);
      }
      log.info("Created system APPROVE decisions for cascaded request: {}",
          refreshedRequest.getId());
    }

    // Process cascade-rejected requests (other teams)
    for (ApprovalRequest request : cascadedRejectedRequests) {
      // Refresh request from database to ensure we have latest status
      ApprovalRequest refreshedRequest = approvalRequestQueryService.findById(request.getId())
          .orElse(null);

      if (refreshedRequest == null) {
        log.warn("Request {} not found when creating cascade decisions, skipping",
            request.getId());
        continue;
      }

      // Verify request status matches expected cascade result
      if (refreshedRequest.getStatus() != ApprovalRequest.ApprovalStatus.REJECTED) {
        log.warn("Request {} has unexpected status {} (expected REJECTED), skipping decision creation",
            refreshedRequest.getId(), refreshedRequest.getStatus());
        continue;
      }

      // Verify request was for a different team
      if (approvedTeamId.equals(refreshedRequest.getTarget().getTeamId())) {
        log.warn("Request {} teamId {} matches approved teamId {}, skipping decision creation",
            refreshedRequest.getId(), refreshedRequest.getTarget().getTeamId(),
            approvedTeamId);
        continue;
      }

      // Create REJECT decision for SYS_ADMIN gate
      ApprovalDecision systemDecision = ApprovalDecision.builder()
          .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
          .requestId(refreshedRequest.getId())
          .approverUserId("SYSTEM")
          .gate("SYS_ADMIN")
          .decision(ApprovalDecision.Decision.REJECT)
          .decidedAt(Instant.now())
          .note("Auto-rejected: service ownership assigned to team " + approvedTeamId)
          .build();
      approvalDecisionCommandService.save(systemDecision);
      log.info("Created system REJECT decision for cascaded request: {}",
          refreshedRequest.getId());
    }
  }

  /**
   * Retrieves credentials created during approval workflow.
   * <p>
   * This method retrieves credentials from thread-local storage that were
   * created during the approval cascade. Credentials are only available
   * immediately after handleApproval() completes successfully.
   * </p>
   *
   * @return ServiceCredentialResponse if credentials were created, null otherwise
   */
  public ServiceCredentialService.ServiceCredentialResponse getCredentialsFromApproval() {
    ServiceCredentialService.ServiceCredentialResponse credentials = credentialsHolder.get();
    if (credentials != null) {
      // Clear thread-local after retrieval (one-time access)
      credentialsHolder.remove();
    }
    return credentials;
  }
}
