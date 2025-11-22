package com.example.control.api.http.controller.infra;

import com.example.control.api.http.dto.infra.FailedHeartbeatDtos;
import com.example.control.api.http.exception.ErrorResponse;
import com.example.control.api.http.mapper.infra.FailedHeartbeatApiMapper;
import com.example.control.application.service.FailedHeartbeatService;
import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.criteria.FailedHeartbeatCriteria;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import com.example.control.infrastructure.config.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing failed heartbeats from DLQ.
 * <p>
 * Provides endpoints for viewing, updating status, and re-driving failed heartbeats
 * with team-based access control.
 * </p>
 */
@RestController
@RequestMapping("/api/heartbeat/dlq")
@RequiredArgsConstructor
@Tag(name = "Failed Heartbeats", description = "Manage failed heartbeats from Dead Letter Queue")
public class FailedHeartbeatController {

    private final FailedHeartbeatService service;

    @GetMapping
    @Operation(summary = "List failed heartbeats with filters and pagination", description = """
            Retrieve a paginated list of failed heartbeats with optional filtering.

            **Access Control:**
            - Team members: Can view failed heartbeats for services owned by their team
            - SYS_ADMIN: Can view all failed heartbeats
            - Results are automatically filtered based on user permissions
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "findAllFailedHeartbeats")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Failed heartbeats retrieved successfully", content = @Content(schema = @Schema(implementation = FailedHeartbeatDtos.FailedHeartbeatPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FailedHeartbeatDtos.FailedHeartbeatPageResponse> findAll(
            @ParameterObject @Valid FailedHeartbeatDtos.QueryFilter filter,
            @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        FailedHeartbeatCriteria criteria = FailedHeartbeatApiMapper.toCriteria(filter, userContext);
        Page<FailedHeartbeat> page = service.findAll(criteria, pageable, userContext);
        FailedHeartbeatDtos.FailedHeartbeatPageResponse response = FailedHeartbeatApiMapper.toPageResponse(page);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get failed heartbeat by ID", description = """
            Retrieve a specific failed heartbeat by its ID.

            **Access Control:**
            - Team members: Can view failed heartbeats for services owned by their team
            - SYS_ADMIN: Can view any failed heartbeat
            - Orphaned services: Visible to all authenticated users
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "findFailedHeartbeatById")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Failed heartbeat found", content = @Content(schema = @Schema(implementation = FailedHeartbeatDtos.Response.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Failed heartbeat not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FailedHeartbeatDtos.Response> findById(
            @Parameter(description = "Failed heartbeat ID", example = "failed-heartbeat-12345") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        UserContext userContext = UserContext.fromJwt(jwt);
        Optional<FailedHeartbeat> opt = service.findById(FailedHeartbeatId.of(id), userContext);
        return opt.map(fh -> ResponseEntity.ok(FailedHeartbeatApiMapper.toResponse(fh)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update failed heartbeat status", description = """
            Update the status of a failed heartbeat (INVESTIGATING, RESOLVED, IGNORED).

            **Access Control:**
            - Team members: Can update failed heartbeats for services owned by their team
            - SYS_ADMIN: Can update any failed heartbeat
            - When status is RESOLVED or IGNORED, resolvedAt and resolvedBy are automatically set
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "updateFailedHeartbeatStatus")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Failed heartbeat status updated successfully", content = @Content(schema = @Schema(implementation = FailedHeartbeatDtos.Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Failed heartbeat not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FailedHeartbeatDtos.Response> updateStatus(
            @Parameter(description = "Failed heartbeat ID", example = "failed-heartbeat-12345") @PathVariable String id,
            @Parameter(description = "Status update request", schema = @Schema(implementation = FailedHeartbeatDtos.UpdateStatusRequest.class)) @Valid @RequestBody FailedHeartbeatDtos.UpdateStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserContext userContext = UserContext.fromJwt(jwt);
        FailedHeartbeat updated = service.updateStatus(
                FailedHeartbeatId.of(id),
                request.getStatus(),
                request.getNotes(),
                userContext);
        return ResponseEntity.ok(FailedHeartbeatApiMapper.toResponse(updated));
    }

    @PostMapping("/{id}/redrive")
    @Operation(summary = "Re-drive failed heartbeat to main topic", description = """
            Re-drive a failed heartbeat back to the main topic for reprocessing.

            **Access Control:**
            - Team members: Can re-drive failed heartbeats for services owned by their team (if <5 minutes old)
            - SYS_ADMIN: Can re-drive any failed heartbeat regardless of age
            - Non-admin users can only re-drive recent failures (<5 minutes old)
            - After successful re-drive, status is automatically set to RESOLVED
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "redriveFailedHeartbeat")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Failed heartbeat re-driven successfully", content = @Content(schema = @Schema(implementation = FailedHeartbeatDtos.Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or failed heartbeat too old", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Failed heartbeat not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FailedHeartbeatDtos.Response> redrive(
            @Parameter(description = "Failed heartbeat ID", example = "failed-heartbeat-12345") @PathVariable String id,
            @Parameter(description = "Re-drive request (force flag is ignored for now)", schema = @Schema(implementation = FailedHeartbeatDtos.RedriveRequest.class)) @RequestBody(required = false) FailedHeartbeatDtos.RedriveRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserContext userContext = UserContext.fromJwt(jwt);
        service.redrive(FailedHeartbeatId.of(id), userContext);
        
        // Fetch updated failed heartbeat (status should be RESOLVED after re-drive)
        Optional<FailedHeartbeat> updated = service.findById(FailedHeartbeatId.of(id), userContext);
        return updated.map(fh -> ResponseEntity.ok(FailedHeartbeatApiMapper.toResponse(fh)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk-status")
    @Operation(summary = "Bulk update failed heartbeat status", description = """
            Update the status of multiple failed heartbeats in a single operation.

            **Access Control:**
            - Team members: Can bulk update failed heartbeats for services owned by their team
            - SYS_ADMIN: Can bulk update any failed heartbeats
            - All failed heartbeats must be accessible by the user (permission check for each)
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "bulkUpdateFailedHeartbeatStatus")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk update completed successfully", content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Long> bulkUpdateStatus(
            @Parameter(description = "Bulk update request", schema = @Schema(implementation = FailedHeartbeatDtos.BulkUpdateStatusRequest.class)) @Valid @RequestBody FailedHeartbeatDtos.BulkUpdateStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserContext userContext = UserContext.fromJwt(jwt);
        
        List<FailedHeartbeatId> ids = request.getIds().stream()
                .map(FailedHeartbeatId::of)
                .collect(Collectors.toList());
        
        long updated = service.bulkUpdateStatus(ids, request.getStatus(), userContext);
        return ResponseEntity.ok(updated);
    }
}

