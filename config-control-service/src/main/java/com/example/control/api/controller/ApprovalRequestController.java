package com.example.control.api.controller;

import com.example.control.api.dto.domain.ApprovalRequestDtos;
import com.example.control.api.mapper.domain.ApprovalRequestApiMapper;
import com.example.control.application.service.ApprovalService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.object.ApprovalRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for ApprovalRequest operations.
 * <p>
 * Provides endpoints for managing approval requests with
 * JWT authentication and team-based access control.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalService approvalService;

    /**
     * Create approval request for a service.
     *
     * @param serviceId the service ID
     * @param request the create request
     * @param jwt the JWT token
     * @return the created request
     */
    @PostMapping("/application-services/{serviceId}/approval-requests")
    public ResponseEntity<ApprovalRequestDtos.Response> create(
            @PathVariable String serviceId,
            @Valid @RequestBody ApprovalRequestDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Creating approval request for service: {} to team: {}", serviceId, request.targetTeamId());
        
        UserContext userContext = UserContext.fromJwt(jwt);
        ApprovalRequest approvalRequest = approvalService.createRequest(serviceId, request.targetTeamId(), userContext);
        ApprovalRequestDtos.Response response = ApprovalRequestApiMapper.toResponse(approvalRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List approval requests (requester's own or all for SYS_ADMIN).
     *
     * @param filter optional query filter
     * @param pageable pagination information
     * @param jwt the JWT token
     * @return page of approval requests
     */
    @GetMapping
    public ResponseEntity<Page<ApprovalRequestDtos.Response>> findAll(
            @RequestParam(required = false) ApprovalRequestDtos.QueryFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Listing approval requests with filter: {}", filter);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        Page<ApprovalRequest> requests = approvalService.findAll(
                ApprovalRequestApiMapper.toCriteria(filter, userContext), pageable, userContext);
        Page<ApprovalRequestDtos.Response> responses = requests.map(ApprovalRequestApiMapper::toResponse);
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get approval request by ID.
     *
     * @param id the request ID
     * @param jwt the JWT token
     * @return the request details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequestDtos.Response> findById(
            @PathVariable String id,
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
     * Submit decision for approval request.
     *
     * @param id the request ID
     * @param request the decision request
     * @param jwt the JWT token
     * @return the decision response
     */
    @PostMapping("/{id}/decisions")
    public ResponseEntity<ApprovalRequestDtos.Response> submitDecision(
            @PathVariable String id,
            @Valid @RequestBody ApprovalRequestDtos.DecisionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Submitting decision for request: {} with decision: {}", id, request.decision());
        
        UserContext userContext = UserContext.fromJwt(jwt);
        approvalService.submitDecision(id, 
                ApprovalDecision.Decision.valueOf(request.decision()), 
                "SYS_ADMIN", // Default gate
                request.note(), 
                userContext);
        
        // Return updated request
        Optional<ApprovalRequest> updatedRequest = approvalService.findById(id, userContext);
        if (updatedRequest.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApprovalRequestDtos.Response response = ApprovalRequestApiMapper.toResponse(updatedRequest.get());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel approval request (requester or SYS_ADMIN).
     *
     * @param id the request ID
     * @param jwt the JWT token
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelRequest(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.info("Cancelling approval request: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        approvalService.cancelRequest(id, userContext);
        
        return ResponseEntity.noContent().build();
    }
}