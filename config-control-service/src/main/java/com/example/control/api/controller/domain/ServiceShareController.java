package com.example.control.api.controller.domain;

import com.example.control.api.dto.domain.ServiceShareDtos;
import com.example.control.api.mapper.domain.ServiceShareApiMapper;
import com.example.control.application.service.ServiceShareService;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.ServiceShare;
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
 * REST controller for ServiceShare operations.
 * <p>
 * Provides endpoints for managing service shares with
 * JWT authentication and team-based access control.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/service-shares")
@RequiredArgsConstructor
@Tag(name = "Service Shares", description = "Team-based service sharing with fine-grained permissions")
public class ServiceShareController {

    private final ServiceShareService serviceShareService;

    /**
     * Grant service share (owner team or SYS_ADMIN).
     *
     * @param request the grant request
     * @param jwt     the JWT token
     * @return the created share
     */
    @PostMapping
    @Operation(summary = "Grant service share", description = """
            Grant fine-grained permissions for a service to another team or user.
            
            **Permission Types:**
            - VIEW_INSTANCE: View service instances
            - EDIT_INSTANCE: Edit service instances
            - VIEW_DRIFT: View drift events
            - EDIT_DRIFT: Edit drift events
            
            **Access Control:**
            - Team members: Can share services owned by their team
            - SYS_ADMIN: Can share any service
            - Permissions can be environment-specific (dev, staging, prod)
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "grantServiceShare")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Service share created successfully", content = @Content(schema = @Schema(implementation = ServiceShareDtos.Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Service not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceShareDtos.Response> grantShare(
            @Parameter(description = "Service share creation request", schema = @Schema(implementation = ServiceShareDtos.CreateRequest.class)) @Valid @RequestBody ServiceShareDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Granting service share for service: {} to {}:{}",
                request.serviceId(), request.grantToType(), request.grantToId());

        UserContext userContext = UserContext.fromJwt(jwt);
        ServiceShare share = ServiceShareApiMapper.toDomain(request, userContext);

        ServiceShare saved = serviceShareService.grantShare(
                share.getServiceId(),
                share.getGrantToType(),
                share.getGrantToId(),
                share.getPermissions(),
                share.getEnvironments(),
                share.getExpiresAt(),
                userContext);

        ServiceShareDtos.Response response = ServiceShareApiMapper.toResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List service shares with filtering (owner only).
     *
     * @param filter   optional query filter
     * @param pageable pagination information
     * @param jwt      the JWT token
     * @return page of service shares
     */
    @GetMapping
    @Operation(summary = "List service shares", description = """
            Retrieve a paginated list of service shares.
            
            **Access Control:**
            - Team members: Can view shares for services owned by their team
            - SYS_ADMIN: Can view all shares
            - Results are automatically filtered based on user permissions
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "findAllServiceShares"

    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service shares retrieved successfully", content = @Content(schema = @Schema(implementation = ServiceShareDtos.ServiceSharePageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceShareDtos.ServiceSharePageResponse> findAll(
            @ParameterObject @Valid ServiceShareDtos.QueryFilter filter,
            @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Listing service shares with filter: {}", filter);

        UserContext userContext = UserContext.fromJwt(jwt);
        Page<ServiceShare> shares = serviceShareService.findAll(
                ServiceShareApiMapper.toCriteria(filter, userContext), pageable, userContext);
        ServiceShareDtos.ServiceSharePageResponse response = ServiceShareApiMapper.toPageResponse(shares);

        return ResponseEntity.ok(response);
    }

    /**
     * Get service share by ID.
     *
     * @param id  the share ID
     * @param jwt the JWT token
     * @return the share details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get service share by ID", description = """
            Retrieve a specific service share by its ID.
            
            **Access Control:**
            - Team members: Can view shares for services owned by their team
            - SYS_ADMIN: Can view any share
            - Shared access: Can view shares granted to their team
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "findByIdServiceShare")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service share found", content = @Content(schema = @Schema(implementation = ServiceShareDtos.Response.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Service share not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceShareDtos.Response> findById(
            @Parameter(description = "Service share ID", example = "share-12345") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting service share by ID: {}", id);

        UserContext userContext = UserContext.fromJwt(jwt);
        Optional<ServiceShare> share = serviceShareService.findById(id, userContext);
        if (share.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceShareDtos.Response response = ServiceShareApiMapper.toResponse(share.get());
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke service share (owner or SYS_ADMIN).
     *
     * @param id  the share ID
     * @param jwt the JWT token
     * @return no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke service share", description = """
            Revoke a service share, removing all granted permissions.
            
            **Access Control:**
            - Team members: Can revoke shares for services owned by their team
            - SYS_ADMIN: Can revoke any share
            - Share creator: Can revoke shares they created
            
            **Note:** This action is irreversible
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "revokeServiceShare")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Service share revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Service share not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> revokeShare(
            @Parameter(description = "Service share ID", example = "share-12345") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Revoking service share: {}", id);

        UserContext userContext = UserContext.fromJwt(jwt);
        serviceShareService.revokeShare(id, userContext);

        return ResponseEntity.noContent().build();
    }
}