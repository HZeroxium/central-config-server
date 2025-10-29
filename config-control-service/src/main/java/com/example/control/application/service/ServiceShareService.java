package com.example.control.application.service;

import com.example.control.application.command.ServiceShareCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.ServiceShareQueryService;
import com.example.control.infrastructure.config.security.DomainPermissionEvaluator;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ServiceShare;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.ServiceShareId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrator service for managing service sharing ACL.
 * <p>
 * This service acts as an orchestrator/aggregator that:
 * <ul>
 * <li>Handles business logic and orchestration</li>
 * <li>Enforces permission checks</li>
 * <li>Validates service existence and duplicates</li>
 * <li>Coordinates between CommandService and QueryService</li>
 * <li>Manages transactions for complex operations</li>
 * </ul>
 * <p>
 * Uses:
 * <ul>
 * <li>{@link ServiceShareCommandService} for write operations</li>
 * <li>{@link ServiceShareQueryService} for read operations</li>
 * <li>{@link ApplicationServiceQueryService} for service validation</li>
 * <li>{@link DomainPermissionEvaluator} for permission checks</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceShareService {

    private final ServiceShareCommandService commandService;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final ServiceShareQueryService serviceShareQueryService;
    private final DomainPermissionEvaluator permissionEvaluator;

    /**
     * Grant share permissions for a service.
     * <p>
     * Validates service existence, permissions, and duplicates before creating
     * share.
     *
     * @param serviceId    the service ID to share
     * @param grantToType  the type of grantee (TEAM or USER)
     * @param grantToId    the grantee ID
     * @param permissions  the permissions to grant
     * @param environments optional environment filter
     * @param expiresAt    optional expiration time
     * @param userContext  the current user context
     * @return the created service share
     * @throws IllegalArgumentException if service not found
     * @throws IllegalStateException    if user lacks permission or share already
     *                                  exists
     */
    @Transactional
    public ServiceShare grantShare(String serviceId,
                                   ServiceShare.GranteeType grantToType,
                                   String grantToId,
                                   List<ServiceShare.SharePermission> permissions,
                                   List<String> environments,
                                   Instant expiresAt,
                                   UserContext userContext) {
        log.info("Granting share for service: {} to {}:{} by user: {}",
                serviceId, grantToType, grantToId, userContext.getUserId());

        // Validate service exists
        ApplicationService service = applicationServiceQueryService
                .findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        // Check if user can manage shares for this service
        if (!permissionEvaluator.canManageShares(userContext, service)) {
            throw new IllegalStateException("User does not have permission to manage shares for this service");
        }

        // Business logic: Check if share already exists
        if (serviceShareQueryService.existsByServiceAndGranteeAndEnvironments(
                serviceId, grantToType, grantToId, environments)) {
            throw new IllegalStateException("Share already exists for the specified criteria");
        }

        // Create share domain object
        ServiceShare share = ServiceShare.builder()
                .resourceLevel(ServiceShare.ResourceLevel.SERVICE)
                .serviceId(serviceId)
                .grantToType(grantToType)
                .grantToId(grantToId)
                .permissions(permissions)
                .environments(environments)
                .grantedBy(userContext.getUserId())
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        // Save via command service
        ServiceShare saved = commandService.save(share);
        log.info("Successfully granted share: {}", saved.getId());
        return saved;
    }

    /**
     * Revoke a service share.
     * <p>
     * Validates share existence and user permission before deletion.
     *
     * @param shareId     the share ID to revoke
     * @param userContext the current user context
     * @throws IllegalArgumentException if share not found
     * @throws IllegalStateException    if user lacks permission
     */
    @Transactional
    public void revokeShare(String shareId, UserContext userContext) {
        log.info("Revoking share: {} by user: {}", shareId, userContext.getUserId());

        // Find share
        ServiceShare share = serviceShareQueryService.findById(ServiceShareId.of(shareId))
                .orElseThrow(() -> new IllegalArgumentException("Share not found: " + shareId));

        // Validate service exists
        ApplicationService service = applicationServiceQueryService
                .findById(ApplicationServiceId.of(share.getServiceId()))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + share.getServiceId()));

        // Check if user can manage shares for this service
        if (!permissionEvaluator.canManageShares(userContext, service)) {
            throw new IllegalStateException("User does not have permission to manage shares for this service");
        }

        // Delete via command service
        commandService.deleteById(ServiceShareId.of(shareId));
        log.info("Successfully revoked share: {}", shareId);
    }

    /**
     * List shares for a specific service.
     * <p>
     * Only service owners can view shares.
     *
     * @param serviceId   the service ID
     * @param userContext the current user context
     * @return list of shares for the service
     */
    public List<ServiceShare> listSharesForService(String serviceId, UserContext userContext) {
        log.debug("Listing shares for service: {} by user: {}", serviceId, userContext.getUserId());

        // Validate service exists and user has permission to view shares
        ApplicationService service = applicationServiceQueryService.findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        if (!permissionEvaluator.canViewShares(userContext, service)) {
            throw new IllegalStateException("User does not have permission to view shares for this service");
        }

        ServiceShareCriteria criteria = ServiceShareCriteria.builder()
                .serviceId(serviceId)
                .userTeamIds(userContext.getTeamIds())
                .build();
        return serviceShareQueryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * List service shares with filtering and pagination.
     *
     * @param filter      the filter criteria
     * @param pageable    pagination information
     * @param userContext the current user context
     * @return page of service shares
     */
    public Page<ServiceShare> findAll(ServiceShareCriteria criteria,
                                      Pageable pageable,
                                      UserContext userContext) {
        log.debug("Listing service shares with criteria: {}, pageable: {}", criteria, pageable);

        // If filtering by service, check permission to view shares for that service
        if (criteria != null && criteria.serviceId() != null) {
            ApplicationService service = applicationServiceQueryService
                    .findById(ApplicationServiceId.of(criteria.serviceId()))
                    .orElseThrow(() -> new IllegalArgumentException("Service not found: " + criteria.serviceId()));

            if (!permissionEvaluator.canViewShares(userContext, service)) {
                throw new IllegalStateException("User does not have permission to view shares for this service");
            }
        }

        return serviceShareQueryService.findAll(criteria, pageable);
    }

    /**
     * Find a service share by ID.
     * <p>
     * Delegates to ServiceShareQueryService for read operations.
     *
     * @param shareId     the share ID
     * @param userContext the current user context
     * @return optional service share
     */
    public Optional<ServiceShare> findById(String shareId, UserContext userContext) {
        log.debug("Finding service share by ID: {} for user: {}", shareId, userContext.getUserId());

        Optional<ServiceShare> share = serviceShareQueryService.findById(ServiceShareId.of(shareId));

        if (share.isPresent()) {
            ServiceShare shareEntity = share.get();

            // Check permission to view this share
            if (!canViewShare(userContext, shareEntity)) {
                log.warn("User {} does not have permission to view share {}", userContext.getUserId(), shareId);
                return Optional.empty();
            }
        }

        return share;
    }

    /**
     * Update a service share.
     *
     * @param share       the updated share
     * @param userContext the current user context
     * @return the saved share
     */
    public ServiceShare update(ServiceShare share, UserContext userContext) {
        log.debug("Updating service share: {} for user: {}", share.getId(), userContext.getUserId());

        // Check permission to update this share
        if (!canUpdateShare(userContext, share)) {
            throw new IllegalStateException("User does not have permission to update this share");
        }

        share.setUpdatedAt(Instant.now());
        return commandService.save(share);
    }

    /**
     * Check if user can view a specific share.
     *
     * @param userContext the user context
     * @param share       the share to check
     * @return true if user can view the share
     */
    private boolean canViewShare(UserContext userContext, ServiceShare share) {
        // System admins can view any share
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Users can view shares they granted
        if (userContext.getUserId().equals(share.getGrantedBy())) {
            return true;
        }

        // Users can view shares granted to their teams
        if (share.getGrantToType() == ServiceShare.GranteeType.TEAM) {
            return userContext.getTeamIds().contains(share.getGrantToId());
        }

        // Users can view shares granted to them directly
        return userContext.getUserId().equals(share.getGrantToId());
    }

    /**
     * Check if user can update a specific share.
     *
     * @param userContext the user context
     * @param share       the share to check
     * @return true if user can update the share
     */
    private boolean canUpdateShare(UserContext userContext, ServiceShare share) {
        // System admins can update any share
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Users can update shares they granted
        return userContext.getUserId().equals(share.getGrantedBy());
    }

    /**
     * Get all service IDs that are shared to specific teams.
     * <p>
     * Delegates to ServiceShareQueryService for read operations.
     *
     * @param teamIds the team IDs to check (user's team membership)
     * @return list of unique service IDs shared to any of the specified teams
     */
    public List<String> getSharedServiceIdsForTeams(List<String> teamIds) {
        log.debug("Getting shared service IDs for teams: {}", teamIds);
        if (teamIds == null || teamIds.isEmpty()) {
            return List.of();
        }
        ServiceShareCriteria criteria = ServiceShareCriteria.forTeams(teamIds);
        List<ServiceShare> shares = serviceShareQueryService.findAll(criteria, Pageable.unpaged()).getContent();
        return shares.stream()
                .map(ServiceShare::getServiceId)
                .distinct()
                .toList();
    }
}
