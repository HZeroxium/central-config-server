package com.example.control.application.service;

import com.example.control.config.security.PermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.ApprovalDecision;
import com.example.control.domain.ApprovalRequest;
import com.example.control.domain.ApplicationService;
import com.example.control.domain.port.ApprovalDecisionRepositoryPort;
import com.example.control.domain.port.ApprovalRequestRepositoryPort;
import com.example.control.domain.port.ApplicationServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ApprovalRequestRepositoryPort requestRepository;
    private final ApprovalDecisionRepositoryPort decisionRepository;
    private final ApplicationServiceRepositoryPort serviceRepository;
    private final PermissionEvaluator permissionEvaluator;

    /**
     * Create a new approval request.
     * <p>
     * Creates a PENDING request with default SYS_ADMIN gate requirement.
     *
     * @param serviceId the service ID to request
     * @param targetTeamId the target team ID for assignment
     * @param userContext the current user context
     * @return the created approval request
     */
    @Transactional
    public ApprovalRequest createRequest(String serviceId, String targetTeamId, UserContext userContext) {
        log.info("Creating approval request for service: {} to team: {} by user: {}", 
                serviceId, targetTeamId, userContext.getUserId());

        // Validate service exists
        ApplicationService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        // Check if user can create approval request
        if (!permissionEvaluator.canCreateApprovalRequest(userContext, serviceId)) {
            throw new IllegalStateException("User does not have permission to create approval request");
        }

        // Create approval request with default gates
        ApprovalRequest request = ApprovalRequest.builder()
                .id(UUID.randomUUID().toString())
                .requesterUserId(userContext.getUserId())
                .requestType(ApprovalRequest.RequestType.ASSIGN_SERVICE_TO_TEAM)
                .target(ApprovalRequest.ApprovalTarget.builder()
                        .serviceId(serviceId)
                        .teamId(targetTeamId)
                        .build())
                .required(List.of(
                        ApprovalRequest.ApprovalGate.builder()
                                .gate("SYS_ADMIN")
                                .minApprovals(1)
                                .build()
                ))
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

        ApprovalRequest saved = requestRepository.save(request);
        log.info("Successfully created approval request: {}", saved.getId());
        return saved;
    }

    /**
     * Submit a decision for an approval request.
     * <p>
     * Validates gate eligibility and handles optimistic locking conflicts.
     *
     * @param requestId the request ID
     * @param decision the decision (APPROVE or REJECT)
     * @param gate the gate name
     * @param note optional note
     * @param userContext the current user context
     * @return the approval decision
     */
    @Transactional
    @Retryable(value = {org.springframework.dao.OptimisticLockingFailureException.class}, 
               maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ApprovalDecision submitDecision(String requestId, 
                                         ApprovalDecision.Decision decision, 
                                         String gate, 
                                         String note, 
                                         UserContext userContext) {
        log.info("Submitting decision for request: {} with decision: {} for gate: {} by user: {}", 
                requestId, decision, gate, userContext.getUserId());

        // Get the request
        ApprovalRequest request = requestRepository.findById(requestId)
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
        if (decisionRepository.existsByRequestAndApproverAndGate(requestId, userContext.getUserId(), gate)) {
            throw new IllegalStateException("User has already made a decision for this request and gate");
        }

        // Create decision
        ApprovalDecision approvalDecision = ApprovalDecision.builder()
                .id(UUID.randomUUID().toString())
                .requestId(requestId)
                .approverUserId(userContext.getUserId())
                .gate(gate)
                .decision(decision)
                .decidedAt(Instant.now())
                .note(note)
                .build();

        ApprovalDecision saved = decisionRepository.save(approvalDecision);
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
     * @param filter the filter criteria
     * @param pageable pagination information
     * @param userContext the current user context
     * @return page of approval requests
     */
    public Page<ApprovalRequest> list(ApprovalRequestRepositoryPort.ApprovalRequestFilter filter, 
                                     Pageable pageable, 
                                     UserContext userContext) {
        log.debug("Listing approval requests with filter: {}, pageable: {}", filter, pageable);

        // Non-admin users can only see their own requests
        if (!userContext.isSysAdmin()) {
            ApprovalRequestRepositoryPort.ApprovalRequestFilter restrictedFilter = 
                    new ApprovalRequestRepositoryPort.ApprovalRequestFilter(
                            userContext.getUserId(),
                            filter.status(),
                            filter.requestType(),
                            filter.fromDate(),
                            filter.toDate(),
                            filter.gate()
                    );
            return requestRepository.list(restrictedFilter, pageable);
        }

        return requestRepository.list(filter, pageable);
    }

    /**
     * Find an approval request by ID.
     * <p>
     * Users can only see their own requests unless they're system admins.
     *
     * @param requestId the request ID
     * @param userContext the current user context
     * @return optional approval request
     */
    public Optional<ApprovalRequest> findById(String requestId, UserContext userContext) {
        log.debug("Finding approval request by ID: {} for user: {}", requestId, userContext.getUserId());

        Optional<ApprovalRequest> request = requestRepository.findById(requestId);
        
        if (request.isPresent()) {
            ApprovalRequest req = request.get();
            
            // Non-admin users can only see their own requests
            if (!userContext.isSysAdmin() && !userContext.getUserId().equals(req.getRequesterUserId())) {
                return Optional.empty();
            }
        }

        return request;
    }

    /**
     * Cancel an approval request.
     * <p>
     * Requesters can cancel their own requests, system admins can cancel any request.
     *
     * @param requestId the request ID
     * @param userContext the current user context
     */
    @Transactional
    public void cancelRequest(String requestId, UserContext userContext) {
        log.info("Cancelling approval request: {} by user: {}", requestId, userContext.getUserId());

        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        // Check if user can cancel the request
        if (!permissionEvaluator.canCancelRequest(userContext, request)) {
            throw new IllegalStateException("User does not have permission to cancel this request");
        }

        // Update request status
        boolean updated = requestRepository.updateStatusAndVersion(
                requestId, 
                ApprovalRequest.ApprovalStatus.CANCELLED, 
                request.getVersion());

        if (!updated) {
            throw new IllegalStateException("Failed to cancel request due to concurrent modification");
        }

        log.info("Successfully cancelled approval request: {}", requestId);
    }

    /**
     * Check if all gates are satisfied and update request status if so.
     * <p>
     * This method is called after each decision to automatically approve
     * requests when all required gates are satisfied.
     *
     * @param requestId the request ID
     */
    private void checkAndUpdateRequestStatus(String requestId) {
        log.debug("Checking if request {} can be approved", requestId);

        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        if (request.getStatus() != ApprovalRequest.ApprovalStatus.PENDING) {
            return; // Request is no longer pending
        }

        // Check each required gate
        boolean allGatesSatisfied = true;
        for (ApprovalRequest.ApprovalGate gate : request.getRequired()) {
            long approveCount = decisionRepository.countByRequestIdAndGateAndDecision(
                    requestId, gate.getGate(), ApprovalDecision.Decision.APPROVE);
            
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
     * Approve a request by updating its status and transferring service ownership.
     *
     * @param requestId the request ID
     */
    private void approveRequest(String requestId) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        // Update request status
        boolean updated = requestRepository.updateStatusAndVersion(
                requestId, 
                ApprovalRequest.ApprovalStatus.APPROVED, 
                request.getVersion());

        if (!updated) {
            throw new IllegalStateException("Failed to approve request due to concurrent modification");
        }

        // Transfer service ownership
        ApplicationService service = serviceRepository.findById(request.getTarget().getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + request.getTarget().getServiceId()));

        service.setOwnerTeamId(request.getTarget().getTeamId());
        service.setUpdatedAt(Instant.now());
        serviceRepository.save(service);

        log.info("Successfully approved request: {} and transferred service ownership", requestId);
    }
}
