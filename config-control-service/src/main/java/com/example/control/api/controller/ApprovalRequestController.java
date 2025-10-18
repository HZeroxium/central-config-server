package com.example.control.api.controller;

import com.example.control.api.dto.ApprovalRequestDtos;
import com.example.control.api.mapper.ApprovalRequestApiMapper;
import com.example.control.application.service.ApprovalService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.ApprovalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for ApprovalRequest operations.
 * <p>
 * This controller provides operations for managing approval requests,
 * which are used for multi-gate approval workflows.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
@Tag(name = "Approval Requests", description = "Operations for managing approval requests")
public class ApprovalRequestController {

    private final ApprovalService approvalService;

    /**
     * List approval requests with filtering and pagination.
     *
     * @param requesterUserId optional requester user ID filter
     * @param status optional status filter
     * @param requestType optional request type filter
     * @param fromDate optional from date filter
     * @param toDate optional to date filter
     * @param gate optional gate filter
     * @param pageable pagination information
     * @param jwt the JWT token
     * @return page of approval requests
     */
    @GetMapping
    @Operation(summary = "List approval requests", description = "Get a paginated list of approval requests with optional filtering")
    public ResponseEntity<ApprovalRequestDtos.ListResponse> list(
            @Parameter(description = "Filter by requester user ID") @RequestParam(required = false) String requesterUserId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) ApprovalRequest.ApprovalStatus status,
            @Parameter(description = "Filter by request type") @RequestParam(required = false) ApprovalRequest.RequestType requestType,
            @Parameter(description = "Filter by from date") @RequestParam(required = false) java.time.Instant fromDate,
            @Parameter(description = "Filter by to date") @RequestParam(required = false) java.time.Instant toDate,
            @Parameter(description = "Filter by gate") @RequestParam(required = false) String gate,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Listing approval requests with filters: requesterUserId={}, status={}, requestType={}, fromDate={}, toDate={}, gate={}", 
                 requesterUserId, status, requestType, fromDate, toDate, gate);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        ApprovalRequestDtos.ListRequest listRequest = new ApprovalRequestDtos.ListRequest(
                requesterUserId, status, requestType, fromDate, toDate, gate,
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().toString());
        
        com.example.control.domain.port.ApprovalRequestRepositoryPort.ApprovalRequestFilter filter = 
                ApprovalRequestApiMapper.toFilter(listRequest, userContext.getTeamIds());
        
        Page<ApprovalRequest> page = approvalService.list(filter, pageable, userContext);
        ApprovalRequestDtos.ListResponse response = ApprovalRequestApiMapper.toListResponse(page);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get an approval request by ID.
     *
     * @param id the request ID
     * @param jwt the JWT token
     * @return the approval request
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get approval request", description = "Get an approval request by ID")
    public ResponseEntity<ApprovalRequestDtos.Response> getById(
            @Parameter(description = "Request ID") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Getting approval request by ID: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        Optional<ApprovalRequest> request = approvalService.findById(id, userContext);
        if (request.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApprovalRequestDtos.Response response = ApprovalRequestApiMapper.toResponse(request.get());
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new approval request.
     *
     * @param request the create request
     * @param jwt the JWT token
     * @return the created approval request
     */
    @PostMapping
    @Operation(summary = "Create approval request", description = "Create a new approval request")
    public ResponseEntity<ApprovalRequestDtos.Response> create(
            @Valid @RequestBody ApprovalRequestDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Creating approval request: serviceId={}, targetTeamId={}", 
                 request.serviceId(), request.targetTeamId());
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        ApprovalRequest approvalRequest = ApprovalRequestApiMapper.toDomain(request, userContext);
        ApprovalRequest saved = approvalService.createRequest(request.serviceId(), request.targetTeamId(), userContext);
        
        ApprovalRequestDtos.Response response = ApprovalRequestApiMapper.toResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submit an approval decision.
     *
     * @param id the request ID
     * @param request the decision request
     * @param jwt the JWT token
     * @return the approval decision
     */
    @PostMapping("/{id}/decisions")
    @Operation(summary = "Submit approval decision", description = "Submit an approval decision for a request")
    public ResponseEntity<ApprovalRequestDtos.DecisionResponse> submitDecision(
            @Parameter(description = "Request ID") @PathVariable String id,
            @Valid @RequestBody ApprovalRequestDtos.DecisionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Submitting approval decision: requestId={}, decision={}, gate={}", 
                 id, request.decision(), request.gate());
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        com.example.control.domain.ApprovalDecision decision = approvalService.submitDecision(
                id, request.decision(), request.gate(), request.note(), userContext);
        
        ApprovalRequestDtos.DecisionResponse response = new ApprovalRequestDtos.DecisionResponse(
                decision.getId(),
                decision.getRequestId(),
                decision.getApproverUserId(),
                decision.getDecision(),
                decision.getGate(),
                decision.getNote(),
                decision.getCreatedAt()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancel an approval request.
     *
     * @param id the request ID
     * @param request the cancel request
     * @param jwt the JWT token
     * @return no content
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel approval request", description = "Cancel an approval request")
    public ResponseEntity<Void> cancel(
            @Parameter(description = "Request ID") @PathVariable String id,
            @Valid @RequestBody ApprovalRequestDtos.CancelRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Canceling approval request: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        approvalService.cancelRequest(id, userContext);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get approval request statistics.
     *
     * @param jwt the JWT token
     * @return the approval request statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get approval request statistics", description = "Get approval request statistics")
    public ResponseEntity<ApprovalRequestDtos.StatsResponse> getStats(
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Getting approval request statistics");
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        long pendingCount = approvalService.countByStatus(ApprovalRequest.ApprovalStatus.PENDING);
        long approvedCount = approvalService.countByStatus(ApprovalRequest.ApprovalStatus.APPROVED);
        long rejectedCount = approvalService.countByStatus(ApprovalRequest.ApprovalStatus.REJECTED);
        long cancelledCount = approvalService.countByStatus(ApprovalRequest.ApprovalStatus.CANCELLED);
        
        ApprovalRequestDtos.StatsResponse response = new ApprovalRequestDtos.StatsResponse(
                pendingCount, approvedCount, rejectedCount, cancelledCount);
        
        return ResponseEntity.ok(response);
    }
}
