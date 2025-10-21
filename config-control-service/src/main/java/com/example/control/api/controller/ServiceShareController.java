package com.example.control.api.controller;

import com.example.control.api.dto.domain.ServiceShareDtos;
import com.example.control.api.mapper.domain.ServiceShareApiMapper;
import com.example.control.application.service.ServiceShareService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ServiceShare;
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

import java.util.List;
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
public class ServiceShareController {

    private final ServiceShareService serviceShareService;

    /**
     * Grant service share (owner team or SYS_ADMIN).
     *
     * @param request the grant request
     * @param jwt the JWT token
     * @return the created share
     */
    @PostMapping
    public ResponseEntity<ServiceShareDtos.Response> grantShare(
            @Valid @RequestBody ServiceShareDtos.CreateRequest request,
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
                userContext
        );
        
        ServiceShareDtos.Response response = ServiceShareApiMapper.toResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List service shares with filtering (owner only).
     *
     * @param filter optional query filter
     * @param pageable pagination information
     * @param jwt the JWT token
     * @return page of service shares
     */
    @GetMapping
    public ResponseEntity<Page<ServiceShareDtos.Response>> list(
            @RequestParam(required = false) ServiceShareDtos.QueryFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Listing service shares with filter: {}", filter);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        Page<ServiceShare> shares = serviceShareService.list(
                ServiceShareApiMapper.toCriteria(filter, userContext), pageable, userContext);
        Page<ServiceShareDtos.Response> responses = shares.map(ServiceShareApiMapper::toResponse);
        
        return ResponseEntity.ok(responses);
    }

    /**
     * List shares for a specific service (owner only).
     *
     * @param serviceId the service ID
     * @param jwt the JWT token
     * @return list of shares for the service
     */
    @GetMapping(params = "serviceId")
    public ResponseEntity<List<ServiceShareDtos.Response>> listSharesForService(
            @RequestParam String serviceId,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Listing shares for service: {}", serviceId);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        List<ServiceShare> shares = serviceShareService.listSharesForService(serviceId, userContext);
        List<ServiceShareDtos.Response> responses = shares.stream()
                .map(ServiceShareApiMapper::toResponse)
                .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get service share by ID.
     *
     * @param id the share ID
     * @param jwt the JWT token
     * @return the share details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceShareDtos.Response> getById(
            @PathVariable String id,
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
     * @param id the share ID
     * @param jwt the JWT token
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeShare(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.info("Revoking service share: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        serviceShareService.revokeShare(id, userContext);
        
        return ResponseEntity.noContent().build();
    }
}