package com.example.control.api.controller;

import com.example.control.api.dto.domain.ApprovalRequestDtos;
import com.example.control.api.mapper.domain.ApprovalRequestApiMapper;
import com.example.control.application.service.ApprovalService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.object.ApprovalRequest;
import com.example.control.api.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Approval Requests", description = "Multi-gate approval workflow for service ownership requests")
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
    @Operation(
        summary = "Create approval request",
        description = """
            Create a new approval request for service ownership.
            
            **Multi-Gate Approval Flow:**
            - SYS_ADMIN: Can approve/reject any request
            - LINE_MANAGER: Can approve requests from their direct reports
            - Request requires approval from configured gates before service ownership is transferred
            
            **Access Control:**
            - Team members: Can create requests for services not owned by their team
            - SYS_ADMIN: Can create requests for any service
            """,
        security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
        },
        operationId = "createApprovalRequest"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Approval request created successfully",
            content = @Content(schema = @Schema(implementation = ApprovalRequestDtos.Response.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflict - Request already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApprovalRequestDtos.Response> create(
            @Parameter(description = "Service ID to request ownership for", example = "payment-service")
            @PathVariable String serviceId,
            @Parameter(description = "Approval request creation data", 
                      schema = @Schema(implementation = ApprovalRequestDtos.CreateRequest.class))
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
    @Operation(
        summary = "List approval requests",
        description = """
            Retrieve a paginated list of approval requests.
            
            **Access Control:**
            - Team members: Can view their own requests
            - SYS_ADMIN: Can view all requests
            - LINE_MANAGER: Can view requests from their direct reports
            - Results are automatically filtered based on user permissions
            """,
        security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
        },
        operationId = "findAllApprovalRequests"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Approval requests retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApprovalRequestDtos.ApprovalRequestPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApprovalRequestDtos.ApprovalRequestPageResponse> findAll(
            @ParameterObject @Valid ApprovalRequestDtos.QueryFilter filter,
            @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Listing approval requests with filter: {}", filter);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        Page<ApprovalRequest> requests = approvalService.findAll(
                ApprovalRequestApiMapper.toCriteria(filter, userContext), pageable, userContext);
        ApprovalRequestDtos.ApprovalRequestPageResponse response = ApprovalRequestApiMapper.toPageResponse(requests);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get approval request by ID.
     *
     * @param id the request ID
     * @param jwt the JWT token
     * @return the request details
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get approval request by ID",
        description = """
            Retrieve a specific approval request by its ID.
            
            **Access Control:**
            - Team members: Can view their own requests
            - SYS_ADMIN: Can view any request
            - LINE_MANAGER: Can view requests from their direct reports
            """,
        security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
        },
        operationId = "findApprovalRequestById"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Approval request found",
            content = @Content(schema = @Schema(implementation = ApprovalRequestDtos.Response.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Approval request not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApprovalRequestDtos.Response> findById(
            @Parameter(description = "Approval request ID", example = "request-12345")
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
    @Operation(
        summary = "Submit approval decision",
        description = """
            Submit a decision (approve/reject) for an approval request.
            
            **Multi-Gate Approval:**
            - SYS_ADMIN: Can approve/reject any request
            - LINE_MANAGER: Can approve/reject requests from their direct reports
            - Request status is updated based on required approvals from configured gates
            
            **Access Control:**
            - Only authorized approvers can submit decisions
            - Decision is recorded with approver information and timestamp
            """,
        security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
        },
        operationId = "submitApprovalDecision"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Decision submitted successfully",
            content = @Content(schema = @Schema(implementation = ApprovalRequestDtos.Response.class))),
        @ApiResponse(responseCode = "400", description = "Invalid decision data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to approve this request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Approval request not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApprovalRequestDtos.Response> submitDecision(
            @Parameter(description = "Approval request ID", example = "request-12345")
            @PathVariable String id,
            @Parameter(description = "Decision data (approve/reject with optional note)", 
                      schema = @Schema(implementation = ApprovalRequestDtos.DecisionRequest.class))
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
    @Operation(
        summary = "Cancel approval request",
        description = """
            Cancel an approval request.
            
            **Access Control:**
            - Requester: Can cancel their own requests
            - SYS_ADMIN: Can cancel any request
            - Request must be in PENDING status to be cancelled
            
            **Note:** Once cancelled, the request cannot be reactivated
            """,
        security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
        },
        operationId = "cancelApprovalRequest"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Approval request cancelled successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to cancel this request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Approval request not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflict - Request cannot be cancelled in current status",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cancelRequest(
            @Parameter(description = "Approval request ID", example = "request-12345")
            @PathVariable String id, 
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Cancelling approval request: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        approvalService.cancelRequest(id, userContext);
        
        return ResponseEntity.noContent().build();
    }
}