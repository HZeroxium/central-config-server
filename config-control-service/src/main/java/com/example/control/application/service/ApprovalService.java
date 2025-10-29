package com.example.control.application.service;

import com.example.control.api.exception.exceptions.ConflictException;
import com.example.control.application.command.ApplicationServiceCommandService;
import com.example.control.application.command.ApprovalDecisionCommandService;
import com.example.control.application.command.ApprovalRequestCommandService;
import com.example.control.application.command.DriftEventCommandService;
import com.example.control.application.command.ServiceInstanceCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.ApprovalDecisionQueryService;
import com.example.control.application.query.ApprovalRequestQueryService;
import com.example.control.infrastructure.config.security.DomainPermissionEvaluator;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.criteria.ApprovalDecisionCriteria;
import com.example.control.domain.criteria.ApprovalRequestCriteria;
import com.example.control.domain.event.ServiceOwnershipTransferred;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.object.ApprovalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrator service for managing approval workflow requests.
 * <p>
 * Provides business logic for multi-gate approval workflows with optimistic
 * locking support, automatic status updates, and ownership transfer
 * orchestration.
 * </p>
 * <p>
 * This orchestrator ONLY calls Command/Query services, NOT other orchestrators.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    // Command Services
    private final ApprovalRequestCommandService approvalRequestCommandService;
    private final ApprovalDecisionCommandService approvalDecisionCommandService;
    private final ApplicationServiceCommandService applicationServiceCommandService;
    private final ServiceInstanceCommandService serviceInstanceCommandService;
    private final DriftEventCommandService driftEventCommandService;

    // Query Services
    private final ApprovalRequestQueryService approvalRequestQueryService;
    private final ApprovalDecisionQueryService approvalDecisionQueryService;
    private final ApplicationServiceQueryService applicationServiceQueryService;

    // Other dependencies
    private final DomainPermissionEvaluator permissionEvaluator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create a new approval request.
     * <p>
     * Creates a PENDING request with default SYS_ADMIN gate requirement.
     *
     * @param serviceId    the service ID to request
     * @param targetTeamId the target team ID for assignment
     * @param userContext  the current user context
     * @return the created approval request
     */
    @Transactional
    public ApprovalRequest createRequest(String serviceId, String targetTeamId, UserContext userContext) {
        log.info("Creating approval request for service: {} to team: {} by user: {}",
                serviceId, targetTeamId, userContext.getUserId());

        // Business logic: Validate service exists and is orphaned (ownerTeamId == null)
        ApplicationService service = applicationServiceQueryService.findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        // Business logic: Only orphaned services can be requested
        if (service.getOwnerTeamId() != null) {
            throw new IllegalStateException("Service is already owned by team: " + service.getOwnerTeamId());
        }

        // Business logic: Check if user can create approval request
        if (!permissionEvaluator.canCreateApprovalRequest(userContext, serviceId)) {
            throw new IllegalStateException("User does not have permission to create approval request");
        }

        // Business logic: Create approval request with dynamic gates based on requester
        List<ApprovalRequest.ApprovalGate> requiredGates = new java.util.ArrayList<>();

        // Always require SYS_ADMIN gate
        requiredGates.add(ApprovalRequest.ApprovalGate.builder()
                .gate("SYS_ADMIN")
                .minApprovals(1)
                .build());

        // Add LINE_MANAGER gate if requester has a manager
        if (userContext.getManagerId() != null && !userContext.getManagerId().trim().isEmpty()) {
            requiredGates.add(ApprovalRequest.ApprovalGate.builder()
                    .gate("LINE_MANAGER")
                    .minApprovals(1)
                    .build());
            log.debug("Added LINE_MANAGER gate for requester with manager: {}", userContext.getManagerId());
        }

        // Business logic: Check for duplicate pending request (same user, same service)
        ApprovalRequestCriteria duplicateCheckCriteria = ApprovalRequestCriteria
                .pendingByRequester(userContext.getUserId());
        Page<ApprovalRequest> existingPending = approvalRequestQueryService.findAll(duplicateCheckCriteria,
                Pageable.unpaged());
        boolean hasPendingForService = existingPending.getContent().stream()
                .anyMatch(req -> req.getTarget().getServiceId().equals(serviceId));

        if (hasPendingForService) {
            throw new ConflictException(
                    "approval.pending.duplicate",
                    "Requester already has a pending request for this service");
        }

        ApprovalRequest request = ApprovalRequest.builder()
                .id(ApprovalRequestId.of(UUID.randomUUID().toString()))
                .requesterUserId(userContext.getUserId())
                .requestType(ApprovalRequest.RequestType.ASSIGN_SERVICE_TO_TEAM)
                .target(ApprovalRequest.ApprovalTarget.builder()
                        .serviceId(serviceId)
                        .teamId(targetTeamId)
                        .build())
                .required(requiredGates)
                .status(ApprovalRequest.ApprovalStatus.PENDING)
                .snapshot(ApprovalRequest.RequesterSnapshot.builder()
                        .teamIds(userContext.getTeamIds())
                        .managerId(userContext.getManagerId())
                        .roles(userContext.getRoles())
                        .build())
                .counts(java.util.Map.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0)
                .build();

        ApprovalRequest saved = approvalRequestCommandService.save(request);
        log.info("Successfully created approval request: {}", saved.getId());
        return saved;
    }

    /**
     * Submit a decision for an approval request.
     * <p>
     * Validates gate eligibility and handles optimistic locking conflicts.
     *
     * @param requestId   the request ID
     * @param decision    the decision (APPROVE or REJECT)
     * @param gate        the gate name
     * @param note        optional note
     * @param userContext the current user context
     * @return the approval decision
     */
    @Transactional
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ApprovalDecision submitDecision(String requestId,
                                           ApprovalDecision.Decision decision,
                                           String gate,
                                           String note,
                                           UserContext userContext) {
        log.info("Submitting decision for request: {} with decision: {} for gate: {} by user: {}",
                requestId, decision, gate, userContext.getUserId());

        // Get the request
        ApprovalRequestId requestIdObj = ApprovalRequestId.of(requestId);
        ApprovalRequest request = approvalRequestQueryService.findById(requestIdObj)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        // Business logic: Validate request is still pending
        if (request.getStatus() != ApprovalRequest.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request is no longer pending: " + requestId);
        }

        // Business logic: Check if user can approve for this gate
        if (!permissionEvaluator.canApprove(userContext, request, gate)) {
            throw new IllegalStateException("User does not have permission to approve for gate: " + gate);
        }

        // Business logic: Check if user already made a decision for this request and
        // gate
        ApprovalDecisionCriteria existingDecisionCriteria = ApprovalDecisionCriteria.forRequestApproverGate(
                requestIdObj.id(), userContext.getUserId(), gate);
        long existingDecisionCount = approvalDecisionQueryService.count(existingDecisionCriteria);

        if (existingDecisionCount > 0) {
            throw new IllegalStateException("User has already made a decision for this request and gate");
        }

        // Create decision
        ApprovalDecision approvalDecision = ApprovalDecision.builder()
                .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
                .requestId(ApprovalRequestId.of(requestId))
                .approverUserId(userContext.getUserId())
                .gate(gate)
                .decision(decision)
                .decidedAt(Instant.now())
                .note(note)
                .build();

        ApprovalDecision saved = approvalDecisionCommandService.save(approvalDecision);
        log.info("Successfully submitted decision: {}", saved.getId());

        // Check if all gates are satisfied
        checkAndUpdateRequestStatus(requestId);

        return saved;
    }

    /**
     * List approval requests with filtering and pagination.
     * <p>
     * Users see their own requests, system admins see all requests.
     *
     * @param filter      the filter criteria
     * @param pageable    pagination information
     * @param userContext the current user context
     * @return page of approval requests
     */
    public Page<ApprovalRequest> findAll(ApprovalRequestCriteria criteria,
                                         Pageable pageable,
                                         UserContext userContext) {
        log.debug("Listing approval requests with criteria: {}, pageable: {}", criteria, pageable);

        // Business logic: Apply user-based filtering
        ApprovalRequestCriteria userCriteria = criteria.toBuilder()
                .requesterUserId(userContext.isSysAdmin() ? null : userContext.getUserId())
                .build();
        return approvalRequestQueryService.findAll(userCriteria, pageable);
    }

    /**
     * Count approval requests by status.
     *
     * @param status the status to count
     * @return the count of requests with the given status
     */
    public long countByStatus(ApprovalRequest.ApprovalStatus status) {
        log.debug("Counting approval requests by status: {}", status);
        ApprovalRequestCriteria statusCriteria = ApprovalRequestCriteria.byStatus(status);
        return approvalRequestQueryService.count(statusCriteria);
    }

    /**
     * Find an approval request by ID.
     * <p>
     * Users can only see their own requests unless they're system admins.
     *
     * @param requestId   the request ID
     * @param userContext the current user context
     * @return optional approval request
     */
    public Optional<ApprovalRequest> findById(String requestId, UserContext userContext) {
        log.debug("Finding approval request by ID: {} for user: {}", requestId, userContext.getUserId());
        // Business logic: Find with permission check
        Optional<ApprovalRequest> request = approvalRequestQueryService.findById(ApprovalRequestId.of(requestId));
        if (request.isEmpty()) {
            return Optional.empty();
        }

        ApprovalRequest approvalRequest = request.get();

        // Business logic: Check permissions - user can view their own requests or be
        // SYS_ADMIN
        if (!approvalRequest.getRequesterUserId().equals(userContext.getUserId()) &&
                !userContext.isSysAdmin()) {
            log.warn("User {} attempted to access request {} without permission",
                    userContext.getUserId(), requestId);
            return Optional.empty();
        }

        return Optional.of(approvalRequest);
    }

    /**
     * Cancel an approval request.
     * <p>
     * Requesters can cancel their own requests, system admins can cancel any
     * request.
     *
     * @param requestId   the request ID
     * @param userContext the current user context
     */
    @Transactional
    public void cancelRequest(String requestId, UserContext userContext) {
        log.info("Cancelling approval request: {} by user: {}", requestId, userContext.getUserId());
        // Business logic: Cancel request with permission check (inline from
        // ApprovalRequestService)
        ApprovalRequestId requestIdObj = ApprovalRequestId.of(requestId);
        Optional<ApprovalRequest> requestOpt = approvalRequestQueryService.findById(requestIdObj);
        if (requestOpt.isEmpty()) {
            throw new IllegalArgumentException("Approval request not found: " + requestId);
        }

        ApprovalRequest request = requestOpt.get();

        // Business logic: Check permissions
        if (!permissionEvaluator.canCancelRequest(userContext, request)) {
            throw new SecurityException("User " + userContext.getUserId() +
                    " is not authorized to cancel request " + requestId);
        }

        // Business logic: Cancel the request
        ApprovalRequest cancelledRequest = request.toBuilder()
                .status(ApprovalRequest.ApprovalStatus.CANCELLED)
                .cancelReason("Cancelled by " + userContext.getUserId())
                .build();

        approvalRequestCommandService.save(cancelledRequest);
        log.info("Successfully cancelled approval request: {}", requestId);
    }

    /**
     * Check if all gates are satisfied and update request status if so.
     * <p>
     * This method is called after each decision to automatically approve or reject
     * requests based on gate decisions. Any REJECT decision immediately rejects the
     * request.
     *
     * @param requestId the request ID
     */
    private void checkAndUpdateRequestStatus(String requestId) {
        log.debug("Checking if request {} can be approved or should be rejected", requestId);

        ApprovalRequestId requestIdObj = ApprovalRequestId.of(requestId);
        ApprovalRequest request = approvalRequestQueryService.findById(requestIdObj)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

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
                rejectRequest(requestId, "Rejected by " + gate.getGate() + " gate");
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
            approveRequest(requestId);
        }
    }

    /**
     * Reject a request by updating its status with reason.
     *
     * @param requestId the request ID
     * @param reason    the rejection reason
     */
    private void rejectRequest(String requestId, String reason) {
        ApprovalRequestId requestIdObj = ApprovalRequestId.of(requestId);
        ApprovalRequest request = approvalRequestQueryService.findById(requestIdObj)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        // Update request status to REJECTED
        boolean updated = approvalRequestCommandService.updateStatusAndVersion(
                requestIdObj,
                ApprovalRequest.ApprovalStatus.REJECTED,
                request.getVersion());

        if (updated) {
            log.info("Successfully rejected request: {} with reason: {}", requestId, reason);
        } else {
            log.warn("Failed to reject request: {} due to version conflict", requestId);
        }
    }

    /**
     * Approve a request by updating its status and transferring service ownership.
     * <p>
     * After ownership transfer, cascade approve/reject other pending requests for
     * the same service
     * and create system decisions for all cascaded requests.
     * </p>
     *
     * @param requestId the request ID
     */
    private void approveRequest(String requestId) {
        ApprovalRequestId requestIdObj = ApprovalRequestId.of(requestId);
        ApprovalRequest request = approvalRequestQueryService.findById(requestIdObj)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

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

        // Business logic: Transfer service ownership (inline from
        // ApplicationServiceService)
        String serviceId = request.getTarget().getServiceId();
        String newTeamId = request.getTarget().getTeamId();

        log.info("Orchestrating ownership transfer of service {} to team {}", serviceId, newTeamId);

        // 1. Update ApplicationService ownership
        ApplicationService service = applicationServiceQueryService.findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Application service not found: " + serviceId));

        String oldTeamId = service.getOwnerTeamId();
        service.setOwnerTeamId(newTeamId);
        service.setUpdatedAt(Instant.now());

        ApplicationService updatedService = applicationServiceCommandService.save(service);

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

        // 4. Find all pending requests for this service (for cascade decision
        // generation)
        ApprovalRequestCriteria pendingServiceCriteria = ApprovalRequestCriteria.allPending();
        List<ApprovalRequest> allPending = approvalRequestQueryService
                .findAll(pendingServiceCriteria, Pageable.unpaged()).getContent();
        List<ApprovalRequest> pendingRequests = allPending.stream()
                .filter(req -> req.getTarget().getServiceId().equals(serviceId))
                .toList();

        // 5. Cascade: Update approval request statuses in bulk
        long approvedAuto = approvalRequestCommandService.cascadeApproveSameTeamPending(serviceId, newTeamId);
        long rejectedAuto = approvalRequestCommandService.cascadeRejectOtherTeamsPending(serviceId, newTeamId,
                "auto-rejected: service ownership assigned to another team");

        log.info("Cascade results for service {}: autoApproved={}, autoRejected={}",
                serviceId, approvedAuto, rejectedAuto);

        // 6. Create system decisions for cascaded requests
        createSystemDecisionsForCascadedRequests(pendingRequests, newTeamId);

        log.info("Successfully approved request: {} and transferred service ownership", requestId);
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
     * @param pendingRequests the list of pending requests before cascade
     * @param approvedTeamId  the team ID that won ownership
     */
    private void createSystemDecisionsForCascadedRequests(List<ApprovalRequest> pendingRequests,
                                                          String approvedTeamId) {
        for (ApprovalRequest pendingRequest : pendingRequests) {
            String targetTeamId = pendingRequest.getTarget().getTeamId();

            if (approvedTeamId.equals(targetTeamId)) {
                // Same team - cascade approve: create APPROVE decision for all gates
                for (ApprovalRequest.ApprovalGate gate : pendingRequest.getRequired()) {
                    // Inline createSystemDecision logic
                    ApprovalDecision systemDecision = ApprovalDecision.builder()
                            .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
                            .requestId(pendingRequest.getId())
                            .approverUserId("SYSTEM")
                            .gate(gate.getGate())
                            .decision(ApprovalDecision.Decision.APPROVE)
                            .decidedAt(Instant.now())
                            .note("Auto-approved: service ownership assigned to team " + approvedTeamId)
                            .build();
                    approvalDecisionCommandService.save(systemDecision);
                }
                log.info("Created system APPROVE decisions for cascaded request: {}",
                        pendingRequest.getId());
            } else {
                // Other team - cascade reject: create REJECT decision for SYS_ADMIN gate
                ApprovalDecision systemDecision = ApprovalDecision.builder()
                        .id(ApprovalDecisionId.of(UUID.randomUUID().toString()))
                        .requestId(pendingRequest.getId())
                        .approverUserId("SYSTEM")
                        .gate("SYS_ADMIN")
                        .decision(ApprovalDecision.Decision.REJECT)
                        .decidedAt(Instant.now())
                        .note("Auto-rejected: service ownership assigned to team " + approvedTeamId)
                        .build();
                approvalDecisionCommandService.save(systemDecision);
                log.info("Created system REJECT decision for cascaded request: {}",
                        pendingRequest.getId());
            }
        }
    }
}
