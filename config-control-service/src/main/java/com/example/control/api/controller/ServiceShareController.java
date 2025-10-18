package com.example.control.api.controller;

import com.example.control.api.dto.ServiceShareDtos;
import com.example.control.api.mapper.ServiceShareApiMapper;
import com.example.control.application.service.ServiceShareService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.ServiceShare;
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
 * REST controller for ServiceShare operations.
 * <p>
 * This controller provides CRUD operations for service shares,
 * which allow teams to share specific permissions with other teams or users.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/service-shares")
@RequiredArgsConstructor
@Tag(name = "Service Shares", description = "Operations for managing service shares")
public class ServiceShareController {

    private final ServiceShareService serviceShareService;

    /**
     * List service shares with filtering and pagination.
     *
     * @param serviceId optional service ID filter
     * @param grantToType optional grantee type filter
     * @param grantToId optional grantee ID filter
     * @param environments optional environments filter
     * @param grantedBy optional granted by filter
     * @param pageable pagination information
     * @param jwt the JWT token
     * @return page of service shares
     */
    @GetMapping
    @Operation(summary = "List service shares", description = "Get a paginated list of service shares with optional filtering")
    public ResponseEntity<ServiceShareDtos.ListResponse> list(
            @Parameter(description = "Filter by service ID") @RequestParam(required = false) String serviceId,
            @Parameter(description = "Filter by grantee type") @RequestParam(required = false) ServiceShare.GranteeType grantToType,
            @Parameter(description = "Filter by grantee ID") @RequestParam(required = false) String grantToId,
            @Parameter(description = "Filter by environments") @RequestParam(required = false) List<String> environments,
            @Parameter(description = "Filter by granted by") @RequestParam(required = false) String grantedBy,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Listing service shares with filters: serviceId={}, grantToType={}, grantToId={}, environments={}, grantedBy={}", 
                 serviceId, grantToType, grantToId, environments, grantedBy);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        ServiceShareDtos.ListRequest listRequest = new ServiceShareDtos.ListRequest(
                serviceId, grantToType, grantToId, environments, grantedBy, 
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().toString());
        
        com.example.control.domain.port.ServiceShareRepositoryPort.ServiceShareFilter filter = 
                ServiceShareApiMapper.toFilter(listRequest, userContext.getTeamIds());
        
        Page<ServiceShare> page = serviceShareService.list(filter, pageable, userContext);
        ServiceShareDtos.ListResponse response = ServiceShareApiMapper.toListResponse(page);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get a service share by ID.
     *
     * @param id the share ID
     * @param jwt the JWT token
     * @return the service share
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get service share", description = "Get a service share by ID")
    public ResponseEntity<ServiceShareDtos.Response> getById(
            @Parameter(description = "Share ID") @PathVariable String id,
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
     * Grant a service share.
     *
     * @param request the grant request
     * @param jwt the JWT token
     * @return the created service share
     */
    @PostMapping
    @Operation(summary = "Grant service share", description = "Grant a new service share")
    public ResponseEntity<ServiceShareDtos.Response> grant(
            @Valid @RequestBody ServiceShareDtos.GrantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Granting service share: serviceId={}, grantToType={}, grantToId={}", 
                 request.serviceId(), request.grantToType(), request.grantToId());
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        ServiceShare share = serviceShareService.grantShare(
                request.serviceId(),
                request.grantToType(),
                request.grantToId(),
                request.permissions(),
                request.environments(),
                request.expiresAt(),
                userContext);
        
        ServiceShareDtos.Response response = ServiceShareApiMapper.toResponse(share);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update a service share.
     *
     * @param id the share ID
     * @param request the update request
     * @param jwt the JWT token
     * @return the updated service share
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update service share", description = "Update an existing service share")
    public ResponseEntity<ServiceShareDtos.Response> update(
            @Parameter(description = "Share ID") @PathVariable String id,
            @Valid @RequestBody ServiceShareDtos.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Updating service share: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        Optional<ServiceShare> existing = serviceShareService.findById(id, userContext);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ServiceShare updated = ServiceShareApiMapper.apply(existing.get(), request, userContext);
        ServiceShare saved = serviceShareService.update(updated, userContext);
        
        ServiceShareDtos.Response response = ServiceShareApiMapper.toResponse(saved);
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke a service share.
     *
     * @param id the share ID
     * @param jwt the JWT token
     * @return no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke service share", description = "Revoke a service share")
    public ResponseEntity<Void> revoke(
            @Parameter(description = "Share ID") @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Revoking service share: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        serviceShareService.revokeShare(id, userContext);
        return ResponseEntity.noContent().build();
    }
}
