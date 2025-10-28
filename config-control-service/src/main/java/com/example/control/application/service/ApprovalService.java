package com.example.control.application.service;

import com.example.control.api.exception.exceptions.ConflictException;
import com.example.control.application.command.applicationservice.TransferOwnershipCommand;
import com.example.control.application.command.applicationservice.TransferOwnershipHandler;
import com.example.control.config.security.DomainPermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.object.ApprovalRequest;
import com.example.control.domain.criteria.ApprovalRequestCriteria;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Application service for managing approval workflow requests.
 * <p>
 * Provides business logic for multi-gate approval workflows with optimistic
 * locking support and automatic status updates.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalRequestService approvalRequestService;
    private final ApprovalDecisionService approvalDecisionService;
    private final ApplicationServiceService applicationServiceService;
    private final DomainPermissionEvaluator permissionEvaluator;
    private final TransferOwnershipHandler transferOwnershipHandler;

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

        // Validate service exists and is orphaned (ownerTeamId == null)
        ApplicationService service = applicationServiceService.findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        // Only orphaned services can be requested
        if (service.getOwnerTeamId() != null) {
            throw new IllegalStateException("Service is already owned by team: " + service.getOwnerTeamId());
        }

        // Check if user can create approval request
        if (!permissionEvaluator.canCreateApprovalRequest(userContext, serviceId)) {
            throw new IllegalStateException("User does not have permission to create approval request");
        }

        // Create approval request with dynamic gates based on requester
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

        // Duplicate pending by same user for same service → 409 policy (throw IllegalState for now, mapped later)
        if (approvalRequestService.getRepository().existsPendingByRequesterAndService(userContext.getUserId(), serviceId)) {
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

        ApprovalRequest saved = approvalRequestService.save(request);
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
    @Retryable(value = { OptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ApprovalDecision submitDecision(String requestId,
            ApprovalDecision.Decision decision,
            String gate,
            String note,
            UserContext userContext) {
        log.info("Submitting decision for request: {} with decision: {} for gate: {} by user: {}",
                requestId, decision, gate, userContext.getUserId());

        // Get the request
        ApprovalRequest request = approvalRequestService.findById(ApprovalRequestId.of(requestId))
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        // Validate request is still pending
        if (request.getStatus() != ApprovalRequest.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request is no longer pending: " + requestId);
        }

        // Check if user can approve for this gate
        if (!permissionEvaluator.canApprove(userContext, request, gate)) {
            throw new IllegalStateException("User does not have permission to approve for gate: " + gate);
        }

        // Check if user already made a decision for this request and gate
        if (approvalDecisionService.existsByRequestAndApproverAndGate(ApprovalRequestId.of(requestId),
                userContext.getUserId(), gate)) {
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

        ApprovalDecision saved = approvalDecisionService.save(approvalDecision);
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

        return approvalRequestService.findAll(criteria, pageable, userContext);
    }

    /**
     * Count approval requests by status.
     *
     * @param status the status to count
     * @return the count of requests with the given status
     */
    public long countByStatus(ApprovalRequest.ApprovalStatus status) {
        log.debug("Counting approval requests by status: {}", status);
        return approvalRequestService.countByStatus(status);
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
        return approvalRequestService.findById(ApprovalRequestId.of(requestId), userContext);
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
        approvalRequestService.cancelRequest(ApprovalRequestId.of(requestId), userContext);
        log.info("Successfully cancelled approval request: {}", requestId);
    }

    /**
     * Check if all gates are satisfied and update request status if so.
     * <p>
     * This method is called after each decision to automatically approve or reject
     * requests based on gate decisions. Any REJECT decision immediately rejects the request.
     *
     * @param requestId the request ID
     */
    private void checkAndUpdateRequestStatus(String requestId) {
        log.debug("Checking if request {} can be approved or should be rejected", requestId);

        ApprovalRequest request = approvalRequestService.findById(ApprovalRequestId.of(requestId))
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        if (request.getStatus() != ApprovalRequest.ApprovalStatus.PENDING) {
            return; // Request is no longer pending
        }

        // Check for ANY REJECT decision - immediate rejection
        for (ApprovalRequest.ApprovalGate gate : request.getRequired()) {
            long rejectCount = approvalDecisionService.countByRequestIdAndGateAndDecision(
                    ApprovalRequestId.of(requestId), gate.getGate(), ApprovalDecision.Decision.REJECT);

            if (rejectCount > 0) {
                log.info("Found rejection for request: {} at gate: {}, rejecting entire request",
                        requestId, gate.getGate());
                rejectRequest(requestId, "Rejected by " + gate.getGate() + " gate");
                return;
            }
        }

        // No rejections - check if all gates are satisfied for approval
        boolean allGatesSatisfied = true;
        for (ApprovalRequest.ApprovalGate gate : request.getRequired()) {
            long approveCount = approvalDecisionService.countByRequestIdAndGateAndDecision(
                    ApprovalRequestId.of(requestId), gate.getGate(), ApprovalDecision.Decision.APPROVE);

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
     * @param reason the rejection reason
     */
    private void rejectRequest(String requestId, String reason) {
        ApprovalRequest request = approvalRequestService.findById(ApprovalRequestId.of(requestId))
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        // Update request status to REJECTED
        boolean updated = approvalRequestService.updateStatus(
                ApprovalRequestId.of(requestId),
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
     * After ownership transfer, cascade approve/reject other pending requests for the same service
     * and create system decisions for all cascaded requests.
     * </p>
     *
     * @param requestId the request ID
     */
    private void approveRequest(String requestId) {
        ApprovalRequest request = approvalRequestService.findById(ApprovalRequestId.of(requestId))
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        // Update request status
        boolean updated = approvalRequestService.updateStatus(
                ApprovalRequestId.of(requestId),
                ApprovalRequest.ApprovalStatus.APPROVED,
                request.getVersion());

        if (!updated) {
            // Loser of race: mark rejected with conflict note
            ApprovalRequest loser = request.toBuilder()
                    .status(ApprovalRequest.ApprovalStatus.REJECTED)
                    .cancelReason("conflict – already assigned")
                    .updatedAt(Instant.now())
                    .build();
            approvalRequestService.save(loser);
            log.info("Request {} rejected due to ownership conflict", requestId);
            return;
        }

        // Transfer service ownership using command handler
        TransferOwnershipCommand command = TransferOwnershipCommand.builder()
                .serviceId(request.getTarget().getServiceId())
                .newTeamId(request.getTarget().getTeamId())
                .transferredBy("system")
                .build();

        transferOwnershipHandler.handle(command);

        // Find all pending requests for this service before cascade
        List<ApprovalRequest> pendingRequests = findPendingRequestsForService(
                request.getTarget().getServiceId());

        // Cascade: Update statuses in bulk
        long approvedAuto = approvalRequestService.getRepository().cascadeApproveSameTeamPending(
                request.getTarget().getServiceId(), request.getTarget().getTeamId());
        long rejectedAuto = approvalRequestService.getRepository().cascadeRejectOtherTeamsPending(
                request.getTarget().getServiceId(), request.getTarget().getTeamId(),
                "auto-rejected: service ownership assigned to another team");
        
        log.info("Cascade results for service {}: autoApproved={}, autoRejected={}",
                request.getTarget().getServiceId(), approvedAuto, rejectedAuto);

        // Create system decisions for cascaded requests
        createSystemDecisionsForCascadedRequests(pendingRequests, request.getTarget().getTeamId());

        log.info("Successfully approved request: {} and transferred service ownership", requestId);
    }

    /**
     * Find all pending requests for a specific service.
     *
     * @param serviceId the service ID
     * @return list of pending requests
     */
    private List<ApprovalRequest> findPendingRequestsForService(String serviceId) {
        return approvalRequestService.getRepository().findAllPendingByServiceId(serviceId);
    }

    /**
     * Create system-generated approval decisions for all cascaded requests.
     * <p>
     * For requests that were cascade-approved (same team), create APPROVE decisions for all gates.
     * For requests that were cascade-rejected (other teams), create REJECT decision for SYS_ADMIN gate.
     * </p>
     *
     * @param pendingRequests the list of pending requests before cascade
     * @param approvedTeamId the team ID that won ownership
     */
    private void createSystemDecisionsForCascadedRequests(List<ApprovalRequest> pendingRequests,
                                                          String approvedTeamId) {
        for (ApprovalRequest pendingRequest : pendingRequests) {
            String targetTeamId = pendingRequest.getTarget().getTeamId();
            
            if (approvedTeamId.equals(targetTeamId)) {
                // Same team - cascade approve: create APPROVE decision for all gates
                for (ApprovalRequest.ApprovalGate gate : pendingRequest.getRequired()) {
                    approvalDecisionService.createSystemDecision(
                            pendingRequest.getId(),
                            gate.getGate(),
                            ApprovalDecision.Decision.APPROVE,
                            "Auto-approved: service ownership assigned to team " + approvedTeamId);
                }
                log.info("Created system APPROVE decisions for cascaded request: {}", 
                        pendingRequest.getId());
            } else {
                // Other team - cascade reject: create REJECT decision for SYS_ADMIN gate
                approvalDecisionService.createSystemDecision(
                        pendingRequest.getId(),
                        "SYS_ADMIN",
                        ApprovalDecision.Decision.REJECT,
                        "Auto-rejected: service ownership assigned to team " + approvedTeamId);
                log.info("Created system REJECT decision for cascaded request: {}", 
                        pendingRequest.getId());
            }
        }
    }
}
