package com.example.control.application.service;

import com.example.control.config.security.DomainPermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ServiceShare;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.ServiceShareId;
import com.example.control.domain.port.ServiceShareRepositoryPort;
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
 * Application service for managing service sharing ACL.
 * <p>
 * Provides business logic for granting and revoking service shares with
 * environment-based filtering and permission management.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceShareService {

    private final ServiceShareRepositoryPort shareRepository;
    private final ApplicationServiceService applicationServiceService;
    private final DomainPermissionEvaluator permissionEvaluator;

    /**
     * Grant share permissions for a service.
     * <p>
     * Only service owners or system admins can grant shares.
     *
     * @param serviceId the service ID to share
     * @param grantToType the type of grantee (TEAM or USER)
     * @param grantToId the grantee ID
     * @param permissions the permissions to grant
     * @param environments optional environment filter
     * @param expiresAt optional expiration time
     * @param userContext the current user context
     * @return the created service share
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

        // Validate service exists and user has permission to share
        ApplicationService service = applicationServiceService.findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        if (!permissionEvaluator.canManageShares(userContext, service)) {
            throw new IllegalStateException("User does not have permission to manage shares for this service");
        }

        // Check if share already exists
        if (shareRepository.existsByServiceAndGranteeAndEnvironments(serviceId, grantToType, grantToId, environments)) {
            throw new IllegalStateException("Share already exists for the specified criteria");
        }

        // Create share
        ServiceShare share = ServiceShare.builder()
                .id(ServiceShareId.of(generateShareId()))
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

        ServiceShare saved = shareRepository.save(share);
        log.info("Successfully granted share: {}", saved.getId());
        return saved;
    }

    /**
     * Revoke a service share.
     * <p>
     * Only the user who granted the share or system admins can revoke it.
     *
     * @param shareId the share ID to revoke
     * @param userContext the current user context
     */
    @Transactional
    public void revokeShare(String shareId, UserContext userContext) {
        log.info("Revoking share: {} by user: {}", shareId, userContext.getUserId());

        ServiceShare share = shareRepository.findById(ServiceShareId.of(shareId))
                .orElseThrow(() -> new IllegalArgumentException("Share not found: " + shareId));

        // Check permission to revoke
        if (!canRevokeShare(userContext, share)) {
            throw new IllegalStateException("User does not have permission to revoke this share");
        }

        shareRepository.deleteById(ServiceShareId.of(shareId));
        log.info("Successfully revoked share: {}", shareId);
    }

    /**
     * List shares for a specific service.
     * <p>
     * Only service owners can view shares.
     *
     * @param serviceId the service ID
     * @param userContext the current user context
     * @return list of shares for the service
     */
    public List<ServiceShare> listSharesForService(String serviceId, UserContext userContext) {
        log.debug("Listing shares for service: {} by user: {}", serviceId, userContext.getUserId());

        // Validate service exists and user has permission to view shares
        ApplicationService service = applicationServiceService.findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        if (!permissionEvaluator.canViewShares(userContext, service)) {
            throw new IllegalStateException("User does not have permission to view shares for this service");
        }

        ServiceShareCriteria criteria = ServiceShareCriteria.builder()
            .serviceId(serviceId)
            .userTeamIds(userContext.getTeamIds())
            .build();
        return shareRepository.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * List service shares with filtering and pagination.
     *
     * @param filter the filter criteria
     * @param pageable pagination information
     * @param userContext the current user context
     * @return page of service shares
     */
    public Page<ServiceShare> findAll(ServiceShareCriteria criteria,
                                      Pageable pageable,
                                      UserContext userContext) {
        log.debug("Listing service shares with criteria: {}, pageable: {}", criteria, pageable);

        // If filtering by service, check permission to view shares for that service
        if (criteria != null && criteria.serviceId() != null) {
            ApplicationService service = applicationServiceService.findById(ApplicationServiceId.of(criteria.serviceId()))
                    .orElseThrow(() -> new IllegalArgumentException("Service not found: " + criteria.serviceId()));

            if (!permissionEvaluator.canViewShares(userContext, service)) {
                throw new IllegalStateException("User does not have permission to view shares for this service");
            }
        }

        return shareRepository.findAll(criteria, pageable);
    }

    /**
     * Find a service share by ID.
     *
     * @param shareId the share ID
     * @param userContext the current user context
     * @return optional service share
     */
    public Optional<ServiceShare> findById(String shareId, UserContext userContext) {
        log.debug("Finding service share by ID: {} for user: {}", shareId, userContext.getUserId());

        Optional<ServiceShare> share = shareRepository.findById(ServiceShareId.of(shareId));
        
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
     * @param share the updated share
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
        return shareRepository.save(share);
    }



    /**
     * Check if user can revoke a specific share.
     * <p>
     * Users can revoke shares they granted, system admins can revoke any share.
     *
     * @param userContext the user context
     * @param share the service share
     * @return true if user can revoke the share
     */
    private boolean canRevokeShare(UserContext userContext, ServiceShare share) {
        // System admins can revoke any share
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Users can revoke shares they granted
        return userContext.getUserId().equals(share.getGrantedBy());
    }

    /**
     * Check if user can view a specific share.
     *
     * @param userContext the user context
     * @param share the share to check
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
     * @param share the share to check
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
     * Generate a unique share ID.
     *
     * @return unique share ID
     */
    private String generateShareId() {
        return "share_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }
}
