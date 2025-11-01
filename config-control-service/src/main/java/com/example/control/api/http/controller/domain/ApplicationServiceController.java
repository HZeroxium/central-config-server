package com.example.control.api.http.controller.domain;

import com.example.control.api.http.dto.domain.ApplicationServiceDtos;
import com.example.control.api.http.exception.ErrorResponse;
import com.example.control.api.http.mapper.domain.ApplicationServiceApiMapper;
import com.example.control.application.service.ApplicationServiceService;
import com.example.control.infrastructure.config.security.DomainPermissionEvaluator;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
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
 * REST controller for ApplicationService operations.
 * <p>
 * Provides CRUD endpoints for managing application services with
 * JWT authentication and team-based access control.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/application-services")
@RequiredArgsConstructor
@Tag(name = "Application Services", description = "Manage application services with team-based access control")
public class ApplicationServiceController {

    private final ApplicationServiceService applicationServiceService;
    private final DomainPermissionEvaluator permissionEvaluator;

    /**
     * List all application services (authenticated endpoint).
     *
     * @param filter   optional query filter
     * @param pageable pagination information
     * @param jwt      the JWT token
     * @return page of application services
     */
    @Operation(summary = "List all application services", description = """
            Retrieve a paginated list of application services visible to the authenticated user.
            
            **Access Control & Visibility Rules:**
            - **Authentication required** - Unauthenticated requests will receive 401
            - **System admins** see all services (no filtering)
            - **Regular users** see:
              - Orphaned services (ownerTeamId=null) - for ownership request workflow
              - Services owned by their teams
              - Services shared to their teams via ServiceShare grants
            - Server-side filtering ensures users only see authorized services
            
            **Use Cases:**
            - Service discovery and catalog browsing
            - Requesting ownership of orphaned services
            - Viewing team-owned and shared services
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "findAllApplicationServices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved application services", content = @Content(schema = @Schema(implementation = ApplicationServiceDtos.ApplicationServicePageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApplicationServiceDtos.ApplicationServicePageResponse> findAll(
            @ParameterObject @Valid ApplicationServiceDtos.QueryFilter filter,
            @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Listing application services with filter: {}", filter);

        UserContext userContext = UserContext.fromJwt(jwt);
        ApplicationServiceCriteria criteria = ApplicationServiceApiMapper.toCriteria(filter, userContext);
        // Application services are public to authenticated users
        Page<ApplicationService> services = applicationServiceService.findAll(criteria, pageable, userContext);
        ApplicationServiceDtos.ApplicationServicePageResponse page = ApplicationServiceApiMapper
                .toPageResponse(services);

        return ResponseEntity.ok(page);
    }

    /**
     * Create a new application service (authenticated).
     *
     * @param request the create request
     * @param jwt     the JWT token
     * @return the created service
     */
    @Operation(summary = "Create a new application service", description = """
            Create a new application service with team ownership.
            
            **Required Permissions:**
            - SYS_ADMIN: Can create services for any team
            - Team members: Can create services for their own team
            - Service ID must be unique across the system
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "createApplicationService")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Application service created successfully", content = @Content(schema = @Schema(implementation = ApplicationServiceDtos.Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Service ID already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApplicationServiceDtos.Response> create(
            @Parameter(description = "Application service creation request", schema = @Schema(implementation = ApplicationServiceDtos.CreateRequest.class)) @Valid @RequestBody ApplicationServiceDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Creating application service: {}", request.id());

        UserContext userContext = UserContext.fromJwt(jwt);
        ApplicationService service = ApplicationServiceApiMapper.toDomain(request);

        ApplicationService saved = applicationServiceService.save(service, userContext);
        ApplicationServiceDtos.Response response = ApplicationServiceApiMapper.toResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get application service by ID (authenticated endpoint).
     *
     * @param id  the service ID
     * @param jwt the JWT token
     * @return the service details
     */
    @Operation(summary = "Get application service by ID", description = """
            Retrieve a specific application service by its ID.
            
            **Access Control:**
            - **Authentication required** - Unauthenticated requests will receive 401
            - **Visibility rules apply** - Users can only access:
              - Orphaned services (ownerTeamId=null)
              - Services owned by their teams
              - Services shared to their teams
            - Returns 404 if service doesn't exist or user lacks permission
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "findApplicationServiceById")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application service found", content = @Content(schema = @Schema(implementation = ApplicationServiceDtos.Response.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Application service not found or access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationServiceDtos.Response> findById(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting application service by ID: {}", id);

        UserContext userContext = UserContext.fromJwt(jwt);
        Optional<ApplicationService> service = applicationServiceService.findById(ApplicationServiceId.of(id));
        if (service.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if user can view this service (permission check)
        // Return 404 instead of 403 to avoid leaking service existence
        if (!permissionEvaluator.canViewService(userContext, service.get())) {
            log.warn("User {} attempted to access service {} without permission",
                    userContext.getUserId(), id);
            return ResponseEntity.notFound().build();
        }

        ApplicationServiceDtos.Response response = ApplicationServiceApiMapper.toResponse(service.get());
        return ResponseEntity.ok(response);
    }

    /**
     * Update application service (owner team or SYS_ADMIN).
     *
     * @param id      the service ID
     * @param request the update request
     * @param jwt     the JWT token
     * @return the updated service
     */
    @Operation(summary = "Update application service", description = """
            Update an existing application service.
            
            **Required Permissions:**
            - SYS_ADMIN: Can update any service
            - Team members: Can update services owned by their team
            - Partial updates are supported (only provided fields are updated)
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "updateApplicationService")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application service updated successfully", content = @Content(schema = @Schema(implementation = ApplicationServiceDtos.Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Application service not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationServiceDtos.Response> update(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String id,
            @Parameter(description = "Application service update request", schema = @Schema(implementation = ApplicationServiceDtos.UpdateRequest.class)) @Valid @RequestBody ApplicationServiceDtos.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Updating application service: {}", id);

        UserContext userContext = UserContext.fromJwt(jwt);
        Optional<ApplicationService> serviceOpt = applicationServiceService.findById(ApplicationServiceId.of(id));
        if (serviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ApplicationService service = serviceOpt.get();
        ApplicationService updated = ApplicationServiceApiMapper.apply(service, request);
        ApplicationService saved = applicationServiceService.save(updated, userContext);
        ApplicationServiceDtos.Response response = ApplicationServiceApiMapper.toResponse(saved);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete application service (SYS_ADMIN only).
     *
     * @param id  the service ID
     * @param jwt the JWT token
     * @return no content
     */
    @Operation(summary = "Delete application service", description = """
            Delete an application service permanently.
            
            **Required Permissions:**
            - SYS_ADMIN: Only system administrators can delete services
            - This action is irreversible and will remove all associated data
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "deleteApplicationService")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Application service deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Application service not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Application service ID", example = "payment-service") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Deleting application service: {}", id);

        UserContext userContext = UserContext.fromJwt(jwt);
        if (!userContext.isSysAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        applicationServiceService.delete(ApplicationServiceId.of(id), userContext);
        return ResponseEntity.noContent().build();
    }
}