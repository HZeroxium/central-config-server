package com.example.control.api.controller;

import com.example.control.api.dto.domain.ApplicationServiceDtos;
import com.example.control.api.mapper.domain.ApplicationServiceApiMapper;
import com.example.control.application.service.ApplicationServiceService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.id.ApplicationServiceId;
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
    @GetMapping
    public ResponseEntity<Page<ApplicationServiceDtos.Response>> findAll(
            @RequestParam(required = false) ApplicationServiceDtos.QueryFilter filter,
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
    @PostMapping
    public ResponseEntity<ApplicationServiceDtos.Response> create(
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
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationServiceDtos.Response> findById(@PathVariable String id) {
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
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationServiceDtos.Response> update(
            @PathVariable String id,
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.info("Deleting application service: {}", id);
        
        UserContext userContext = UserContext.fromJwt(jwt);
        if (!userContext.isSysAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        applicationServiceService.delete(ApplicationServiceId.of(id), userContext);
        return ResponseEntity.noContent().build();
    }
}