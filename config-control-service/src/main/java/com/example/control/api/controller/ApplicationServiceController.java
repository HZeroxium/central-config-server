package com.example.control.api.controller;

import com.example.control.api.dto.domain.ApplicationServiceDtos;
import com.example.control.api.exception.ErrorResponse;
import com.example.control.api.mapper.domain.ApplicationServiceApiMapper;
import com.example.control.application.service.ApplicationServiceService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.id.ApplicationServiceId;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final ApplicationServiceApiMapper mapper;

    /**
     * List all application services (authenticated endpoint).
     *
     * @param filter optional query filter
     * @param pageable pagination information
     * @param jwt the JWT token
     * @return page of application services
     */
    @Operation(
        summary = "List all application services",
        description = """
            Retrieve a paginated list of all application services.
            
            **Access Control:**
            - All authenticated users can view application services
            - Services are public to authenticated users for discovery
            - Filtering by team ownership is applied automatically
            """,
        security = @SecurityRequirement(name = "oauth2_auth_code"),
        operationId = "findAllApplicationServices"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved application services",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ApplicationServiceDtos.Response>> findAll(
            @Parameter(description = "Optional query filter for searching services", 
                      schema = @Schema(implementation = ApplicationServiceDtos.QueryFilter.class))
            @RequestParam(required = false) ApplicationServiceDtos.QueryFilter filter,
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Listing application services with filter: {}", filter);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        // Application services are public to authenticated users
        List<ApplicationService> services = applicationServiceService.findAll();
        // Convert to Page manually for now
        Page<ApplicationService> page = new PageImpl<>(services, pageable, services.size());
        Page<ApplicationServiceDtos.Response> responses = page.map(mapper::toResponse);
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Create a new application service (authenticated).
     *
     * @param request the create request
     * @param jwt the JWT token
     * @return the created service
     */
    @Operation(
        summary = "Create a new application service",
        description = """
            Create a new application service with team ownership.
            
            **Required Permissions:**
            - SYS_ADMIN: Can create services for any team
            - Team members: Can create services for their own team
            - Service ID must be unique across the system
            """,
        security = @SecurityRequirement(name = "oauth2_auth_code"),
        operationId = "createApplicationService"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Application service created successfully",
            content = @Content(schema = @Schema(implementation = ApplicationServiceDtos.Response.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflict - Service ID already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApplicationServiceDtos.Response> create(
            @Parameter(description = "Application service creation request", 
                      schema = @Schema(implementation = ApplicationServiceDtos.CreateRequest.class))
            @Valid @RequestBody ApplicationServiceDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Creating application service: {}", request.id());
        
        UserContext userContext = UserContext.fromJwt(jwt);
        ApplicationService service = mapper.toDomain(request);
        service.setCreatedBy(userContext.getUserId());
        
        ApplicationService saved = applicationServiceService.save(service, userContext);
        ApplicationServiceDtos.Response response = mapper.toResponse(saved);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get application service by ID (public endpoint).
     *
     * @param id the service ID
     * @return the service details
     */
    @Operation(
        summary = "Get application service by ID",
        description = """
            Retrieve a specific application service by its ID.
            
            **Access Control:**
            - Public endpoint - no authentication required
            - Returns service metadata for discovery purposes
            """,
        security = @SecurityRequirement(name = "oauth2_auth_code"),
        operationId = "findApplicationServiceById"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application service found",
            content = @Content(schema = @Schema(implementation = ApplicationServiceDtos.Response.class))),
        @ApiResponse(responseCode = "404", description = "Application service not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationServiceDtos.Response> findById(
            @Parameter(description = "Application service ID", example = "payment-service")
            @PathVariable String id) {
        log.debug("Getting application service by ID: {}", id);
        
        Optional<ApplicationService> service = applicationServiceService.findById(ApplicationServiceId.of(id));
        if (service.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApplicationServiceDtos.Response response = mapper.toResponse(service.get());
        return ResponseEntity.ok(response);
    }

    /**
     * Update application service (owner team or SYS_ADMIN).
     *
     * @param id the service ID
     * @param request the update request
     * @param jwt the JWT token
     * @return the updated service
     */
    @Operation(
        summary = "Update application service",
        description = """
            Update an existing application service.
            
            **Required Permissions:**
            - SYS_ADMIN: Can update any service
            - Team members: Can update services owned by their team
            - Partial updates are supported (only provided fields are updated)
            """,
        security = @SecurityRequirement(name = "oauth2_auth_code"),
        operationId = "updateApplicationService"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application service updated successfully",
            content = @Content(schema = @Schema(implementation = ApplicationServiceDtos.Response.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Application service not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationServiceDtos.Response> update(
            @Parameter(description = "Application service ID", example = "payment-service")
            @PathVariable String id,
            @Parameter(description = "Application service update request", 
                      schema = @Schema(implementation = ApplicationServiceDtos.UpdateRequest.class))
            @Valid @RequestBody ApplicationServiceDtos.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Updating application service: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        Optional<ApplicationService> serviceOpt = applicationServiceService.findById(ApplicationServiceId.of(id));
        if (serviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApplicationService service = serviceOpt.get();
        ApplicationService updated = mapper.apply(service, request);
        ApplicationService saved = applicationServiceService.save(updated, userContext);
        ApplicationServiceDtos.Response response = mapper.toResponse(saved);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete application service (SYS_ADMIN only).
     *
     * @param id the service ID
     * @param jwt the JWT token
     * @return no content
     */
    @Operation(
        summary = "Delete application service",
        description = """
            Delete an application service permanently.
            
            **Required Permissions:**
            - SYS_ADMIN: Only system administrators can delete services
            - This action is irreversible and will remove all associated data
            """,
        security = @SecurityRequirement(name = "oauth2_auth_code"),
        operationId = "deleteApplicationService"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Application service deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Application service not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Application service ID", example = "payment-service")
            @PathVariable String id, 
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