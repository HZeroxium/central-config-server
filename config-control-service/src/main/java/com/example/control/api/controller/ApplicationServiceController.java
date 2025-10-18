package com.example.control.api.controller;

import com.example.control.api.dto.ApplicationServiceDtos;
import com.example.control.api.mapper.ApplicationServiceApiMapper;
import com.example.control.application.service.ApplicationServiceService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.ApplicationService;
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
 * REST controller for ApplicationService operations.
 * <p>
 * This controller provides CRUD operations for application services,
 * which are public metadata that can be viewed by all users.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/application-services")
@RequiredArgsConstructor
@Tag(name = "Application Services", description = "Operations for managing application services")
public class ApplicationServiceController {

    private final ApplicationServiceService applicationServiceService;

    /**
     * List application services with filtering and pagination.
     *
     * @param ownerTeamId optional team ID filter
     * @param lifecycle optional lifecycle filter
     * @param tags optional tags filter
     * @param search optional search term
     * @param pageable pagination information
     * @param jwt the JWT token
     * @return page of application services
     */
    @GetMapping
    @Operation(summary = "List application services", description = "Get a paginated list of application services with optional filtering")
    public ResponseEntity<ApplicationServiceDtos.ListResponse> list(
            @Parameter(description = "Filter by owner team ID") @RequestParam(required = false) String ownerTeamId,
            @Parameter(description = "Filter by lifecycle") @RequestParam(required = false) ApplicationService.ServiceLifecycle lifecycle,
            @Parameter(description = "Filter by tags") @RequestParam(required = false) List<String> tags,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "displayName,asc") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Listing application services with filters: ownerTeamId={}, lifecycle={}, tags={}, search={}", 
                 ownerTeamId, lifecycle, tags, search);
        
        UserContext userContext = null;
        try {
            if (jwt != null) {
                userContext = UserContext.fromJwt(jwt);
            }
        } catch (Exception e) {
            log.debug("Could not extract user context from JWT: {}", e.getMessage());
            userContext = null;
        }
        
        ApplicationServiceDtos.ListRequest listRequest = new ApplicationServiceDtos.ListRequest(
                ownerTeamId, lifecycle, tags, search, pageable.getPageNumber(), pageable.getPageSize(), 
                pageable.getSort().toString());
        
        com.example.control.domain.port.ApplicationServiceRepositoryPort.ApplicationServiceFilter filter = 
                ApplicationServiceApiMapper.toFilter(listRequest, userContext != null ? userContext.getTeamIds() : null);
        
        Page<ApplicationService> page = applicationServiceService.list(filter, pageable, userContext);
        ApplicationServiceDtos.ListResponse response = ApplicationServiceApiMapper.toListResponse(page);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get an application service by ID.
     *
     * @param id the service ID
     * @param jwt the JWT token
     * @return the application service
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get application service", description = "Get an application service by ID")
    public ResponseEntity<ApplicationServiceDtos.Response> getById(
            @Parameter(description = "Service ID") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Getting application service by ID: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        Optional<ApplicationService> service = applicationServiceService.findById(id);
        if (service.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApplicationServiceDtos.Response response = ApplicationServiceApiMapper.toResponse(service.get());
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new application service.
     *
     * @param request the create request
     * @param jwt the JWT token
     * @return the created application service
     */
    @PostMapping
    @Operation(summary = "Create application service", description = "Create a new application service")
    public ResponseEntity<ApplicationServiceDtos.Response> create(
            @Valid @RequestBody ApplicationServiceDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Creating application service: {}", request.id());
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        ApplicationService service = ApplicationServiceApiMapper.toDomain(request, userContext);
        ApplicationService saved = applicationServiceService.save(service, userContext);
        
        ApplicationServiceDtos.Response response = ApplicationServiceApiMapper.toResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an application service.
     *
     * @param id the service ID
     * @param request the update request
     * @param jwt the JWT token
     * @return the updated application service
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update application service", description = "Update an existing application service")
    public ResponseEntity<ApplicationServiceDtos.Response> update(
            @Parameter(description = "Service ID") @PathVariable String id,
            @Valid @RequestBody ApplicationServiceDtos.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Updating application service: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        Optional<ApplicationService> existing = applicationServiceService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApplicationService updated = ApplicationServiceApiMapper.apply(existing.get(), request, userContext);
        ApplicationService saved = applicationServiceService.save(updated, userContext);
        
        ApplicationServiceDtos.Response response = ApplicationServiceApiMapper.toResponse(saved);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an application service.
     *
     * @param id the service ID
     * @param jwt the JWT token
     * @return no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete application service", description = "Delete an application service")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Service ID") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Deleting application service: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        applicationServiceService.delete(id, userContext);
        return ResponseEntity.noContent().build();
    }
}
