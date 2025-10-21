package com.example.control.config.security;

import com.example.control.application.service.ServiceShareService;
import com.example.control.domain.object.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * Service for evaluating user permissions on domain objects.
 * <p>
 * Provides authorization logic for team-based access control, service sharing,
 * and approval workflow permissions.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainPermissionEvaluator {

    private static final String LINE_MANAGER = "LINE_MANAGER";
    private static final String SYS_ADMIN = "SYS_ADMIN";
    private final ServiceShareService serviceShareService;

    /**
     * Check if user can view an application service.
     * <p>
     * Application services are public - anyone can view them.
     *
     * @param userContext the user context
     * @param service the application service
     * @return true if user can view the service
     */
    public boolean canViewService(UserContext userContext, ApplicationService service) {
        log.debug("Checking if user {} can view service {}", userContext.getUserId(), service.getId());
        
        // Application services are public - everyone can view them
        return true;
    }

    /**
     * Check if user can edit an application service.
     * <p>
     * Only team members or system admins can edit services.
     *
     * @param userContext the user context
     * @param service the application service
     * @return true if user can edit the service
     */
    public boolean canEditService(UserContext userContext, ApplicationService service) {
        log.debug("Checking if user {} can edit service {}", userContext.getUserId(), service.getId());
        
        // System admins can edit any service
        if (userContext.isSysAdmin()) {
            return true;
        }
        
        // Team members can edit services owned by their team
        return userContext.isMemberOfTeam(service.getOwnerTeamId());
    }

    /**
     * Check if user can view a service instance.
     * <p>
     * Users can view instances if they belong to the owning team or have explicit share permissions.
     *
     * @param userContext the user context
     * @param instance the service instance
     * @return true if user can view the instance
     */
    public boolean canViewInstance(UserContext userContext, ServiceInstance instance) {
        log.debug("Checking if user {} can view instance {} of service {}", 
                userContext.getUserId(), instance.getInstanceId(), instance.getServiceName());
        
        // System admins can view all instances
        if (userContext.isSysAdmin()) {
            return true;
        }
        
        // Team members can view instances of services owned by their team
        if (instance.getTeamId() != null && userContext.isMemberOfTeam(instance.getTeamId())) {
            return true;
        }
        
        // Check service shares for explicit permissions
        if (instance.getServiceId() != null) {
            List<ServiceShare.SharePermission> permissions = serviceShareService.findEffectivePermissions(
                userContext, instance.getServiceId(), List.of(instance.getEnvironment())
            );
            if (permissions.contains(ServiceShare.SharePermission.VIEW_INSTANCE)) {
                log.debug("User {} granted VIEW_INSTANCE permission via service share for service {}", 
                    userContext.getUserId(), instance.getServiceId());
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if user can edit a service instance.
     * <p>
     * Similar to view permissions but requires EDIT_INSTANCE share permission.
     *
     * @param userContext the user context
     * @param instance the service instance
     * @return true if user can edit the instance
     */
    public boolean canEditInstance(UserContext userContext, ServiceInstance instance) {
        log.debug("Checking if user {} can edit instance {} of service {}", 
                userContext.getUserId(), instance.getInstanceId(), instance.getServiceName());
        
        // System admins can edit all instances
        if (userContext.isSysAdmin()) {
            return true;
        }
        
        // Team members can edit instances of services owned by their team
        if (instance.getTeamId() != null && userContext.isMemberOfTeam(instance.getTeamId())) {
            return true;
        }
        
        // Check service shares for EDIT_INSTANCE permission
        if (instance.getServiceId() != null) {
            List<ServiceShare.SharePermission> permissions = serviceShareService.findEffectivePermissions(
                userContext, instance.getServiceId(), List.of(instance.getEnvironment())
            );
            if (permissions.contains(ServiceShare.SharePermission.EDIT_INSTANCE)) {
                log.debug("User {} granted EDIT_INSTANCE permission via service share for service {}", 
                    userContext.getUserId(), instance.getServiceId());
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if user can approve requests for a specific gate.
     * <p>
     * SYS_ADMIN gate: requires SYS_ADMIN role
     * LINE_MANAGER gate: requires user to be the manager of the requester
     *
     * @param userContext the user context
     * @param request the approval request
     * @param gate the gate name
     * @return true if user can approve for the gate
     */
    public boolean canApprove(UserContext userContext, ApprovalRequest request, String gate) {
        log.debug("Checking if user {} can approve request {} for gate {}", 
                userContext.getUserId(), request.getId(), gate);
        
        switch (gate.toUpperCase()) {
            case SYS_ADMIN:
                return userContext.hasRole(SYS_ADMIN);
                
            case LINE_MANAGER:
                // Check if current user is the manager of the requester
                String requesterManagerId = request.getSnapshot() != null 
                    ? request.getSnapshot().getManagerId() 
                    : null;
                return userContext.getUserId().equals(requesterManagerId);
                
            default:
                log.warn("Unknown approval gate: {}", gate);
                return false;
        }
    }

    /**
     * Check if user can create approval requests for a service.
     * <p>
     * Any authenticated user can request service ownership transfer.
     *
     * @param userContext the user context
     * @param serviceId the service ID
     * @return true if user can create approval request
     */
    public boolean canCreateApprovalRequest(UserContext userContext, String serviceId) {
        log.debug("Checking if user {} can create approval request for service {}", 
                userContext.getUserId(), serviceId);
        
        // Any authenticated user can request service ownership transfer
        return userContext.getUserId() != null;
    }

    /**
     * Check if user can manage service shares.
     * <p>
     * Only service owners or system admins can manage shares.
     *
     * @param userContext the user context
     * @param service the application service
     * @return true if user can manage shares
     */
    public boolean canManageShares(UserContext userContext, ApplicationService service) {
        log.debug("Checking if user {} can manage shares for service {}", 
                userContext.getUserId(), service.getId());
        
        // System admins can manage shares for any service
        if (userContext.isSysAdmin()) {
            return true;
        }
        
        // Team members can manage shares for services owned by their team
        return userContext.isMemberOfTeam(service.getOwnerTeamId());
    }

    /**
     * Check if user can view service shares.
     * <p>
     * Only service owners or system admins can view shares.
     *
     * @param userContext the user context
     * @param service the application service
     * @return true if user can view shares
     */
    public boolean canViewShares(UserContext userContext, ApplicationService service) {
        log.debug("Checking if user {} can view shares for service {}", 
                userContext.getUserId(), service.getId());
        
        // System admins can view shares for any service
        if (userContext.isSysAdmin()) {
            return true;
        }
        
        // Team members can view shares for services owned by their team
        return userContext.isMemberOfTeam(service.getOwnerTeamId());
    }

    /**
     * Check if user can cancel an approval request.
     * <p>
     * Requesters can cancel their own requests, system admins can cancel any request.
     *
     * @param userContext the user context
     * @param request the approval request
     * @return true if user can cancel the request
     */
    public boolean canCancelRequest(UserContext userContext, ApprovalRequest request) {
        log.debug("Checking if user {} can cancel request {}", 
                userContext.getUserId(), request.getId());
        
        // System admins can cancel any request
        if (userContext.isSysAdmin()) {
            return true;
        }
        
        // Requesters can cancel their own requests
        return userContext.getUserId().equals(request.getRequesterUserId());
    }

    /**
     * Check if user can view a drift event.
     * <p>
     * Users can view drift events if they belong to the owning team or have explicit share permissions.
     * VIEW_INSTANCE permission on service also grants view on related drift_events.
     *
     * @param userContext the user context
     * @param driftEvent the drift event
     * @return true if user can view the drift event
     */
    public boolean canViewDriftEvent(UserContext userContext, DriftEvent driftEvent) {
        log.debug("Checking if user {} can view drift event {} for service {}", 
                userContext.getUserId(), driftEvent.getId(), driftEvent.getServiceName());
        
        // System admins can view all drift events
        if (userContext.isSysAdmin()) {
            return true;
        }
        
        // Team members can view drift events of services owned by their team
        if (driftEvent.getTeamId() != null && userContext.isMemberOfTeam(driftEvent.getTeamId())) {
            return true;
        }
        
        // Check service shares for explicit permissions
        if (driftEvent.getServiceId() != null) {
            List<ServiceShare.SharePermission> permissions = serviceShareService.findEffectivePermissions(
                userContext, driftEvent.getServiceId(), List.of(driftEvent.getEnvironment())
            );
            if (permissions.contains(ServiceShare.SharePermission.VIEW_INSTANCE)) {
                log.debug("User {} granted VIEW_INSTANCE permission via service share for drift event service {}", 
                    userContext.getUserId(), driftEvent.getServiceId());
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if user can edit a drift event.
     * <p>
     * Similar to view permissions but requires EDIT_INSTANCE share permission.
     *
     * @param userContext the user context
     * @param driftEvent the drift event
     * @return true if user can edit the drift event
     */
    public boolean canEditDriftEvent(UserContext userContext, DriftEvent driftEvent) {
        log.debug("Checking if user {} can edit drift event {} for service {}", 
                userContext.getUserId(), driftEvent.getId(), driftEvent.getServiceName());
        
        // System admins can edit all drift events
        if (userContext.isSysAdmin()) {
            return true;
        }
        
        // Team members can edit drift events of services owned by their team
        if (driftEvent.getTeamId() != null && userContext.isMemberOfTeam(driftEvent.getTeamId())) {
            return true;
        }
        
        // Check service shares for EDIT_INSTANCE permission
        if (driftEvent.getServiceId() != null) {
            List<ServiceShare.SharePermission> permissions = serviceShareService.findEffectivePermissions(
                userContext, driftEvent.getServiceId(), List.of(driftEvent.getEnvironment())
            );
            if (permissions.contains(ServiceShare.SharePermission.EDIT_INSTANCE)) {
                log.debug("User {} granted EDIT_INSTANCE permission via service share for drift event service {}", 
                    userContext.getUserId(), driftEvent.getServiceId());
                return true;
            }
        }
        
        return false;
    }
}
