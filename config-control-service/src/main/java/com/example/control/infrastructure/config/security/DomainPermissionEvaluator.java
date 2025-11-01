package com.example.control.infrastructure.config.security;

import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.*;
import com.example.control.domain.port.repository.ServiceShareRepositoryPort;
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
    private final ServiceShareRepositoryPort serviceShareRepository;

    /**
     * Check if user can view an application service.
     * <p>
     * <strong>Visibility Rules:</strong>
     * <ul>
     * <li>System admins can view all services</li>
     * <li>Orphaned services (ownerTeamId=null) are visible to all authenticated
     * users</li>
     * <li>Team members can view services owned by their teams</li>
     * <li>Users can view services shared to their teams via ServiceShare</li>
     * </ul>
     *
     * @param userContext the user context
     * @param service     the application service
     * @return true if user can view the service
     */
    public boolean canViewService(UserContext userContext, ApplicationService service) {
        log.debug("Checking if user {} can view service {}", userContext.getUserId(), service.getId());

        // System admins can view all services
        if (userContext.isSysAdmin()) {
            log.debug("User {} is SYS_ADMIN, can view service {}", userContext.getUserId(), service.getId());
            return true;
        }

        // Orphaned services (ownerTeamId=null) are visible to all authenticated users
        // This enables users to request ownership of orphaned services
        if (service.getOwnerTeamId() == null) {
            log.debug("Service {} is orphaned, visible to user {}", service.getId(), userContext.getUserId());
            return true;
        }

        // Team members can view services owned by their teams
        if (userContext.isMemberOfTeam(service.getOwnerTeamId())) {
            log.debug("User {} is member of owning team {}, can view service {}",
                    userContext.getUserId(), service.getOwnerTeamId(), service.getId());
            return true;
        }

        // Check if service is shared to user's teams
        List<String> sharedServiceIds = serviceShareRepository.findServiceIdsByGranteeTeams(userContext.getTeamIds());
        if (sharedServiceIds.contains(service.getId().id())) {
            log.debug("Service {} is shared to user {} teams, can view",
                    service.getId(), userContext.getUserId());
            return true;
        }

        log.debug("User {} cannot view service {} - not owned, not shared, not orphaned",
                userContext.getUserId(), service.getId());
        return false;
    }

    /**
     * Check if user can edit an application service.
     * <p>
     * Only team members or system admins can edit services.
     *
     * @param userContext the user context
     * @param service     the application service
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
     * Users can view instances if they belong to the owning team or have explicit
     * share permissions.
     *
     * @param userContext the user context
     * @param instance    the service instance
     * @return true if user can view the instance
     */
    public boolean canViewInstance(UserContext userContext, ServiceInstance instance) {
        log.debug("Checking if user {} can view instance {} of service {}",
                userContext.getUserId(), instance.getInstanceId(), instance.getServiceId());

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
            List<ServiceShare.SharePermission> permissions = findEffectivePermissions(
                    userContext, instance.getServiceId(), List.of(instance.getEnvironment()));
            if (permissions.contains(ServiceShare.SharePermission.VIEW_INSTANCE)) {
                log.debug("User {} granted VIEW_INSTANCE permission via service share for service {}",
                        userContext.getUserId(), instance.getServiceId());
                return true;
            }
        }

        return false;
    }

    /**
     * Check if user can create a service instance for an application service.
     * <p>
     * Checks permission based on ApplicationService before instance is created.
     * SYS_ADMIN can always create. Team members can create if service is owned by their team.
     * Users can create if service is shared with EDIT_INSTANCE permission.
     *
     * @param userContext the user context
     * @param service     the application service
     * @param environment the environment for the instance (used for share permission checks)
     * @return true if user can create instance for the service
     */
    public boolean canCreateInstance(UserContext userContext, ApplicationService service, String environment) {
        log.debug("Checking if user {} can create instance for service {} in environment {}",
                userContext.getUserId(), service.getId(), environment);

        // System admins can create instances for any service
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Team members can create instances for services owned by their team
        if (service.getOwnerTeamId() != null && userContext.isMemberOfTeam(service.getOwnerTeamId())) {
            log.debug("User {} is member of owning team {}, can create instance",
                    userContext.getUserId(), service.getOwnerTeamId());
            return true;
        }

        // Check service shares for EDIT_INSTANCE permission
        if (environment != null) {
            List<ServiceShare.SharePermission> permissions = findEffectivePermissions(
                    userContext, service.getId().id(), List.of(environment));
            if (permissions.contains(ServiceShare.SharePermission.EDIT_INSTANCE)) {
                log.debug("User {} granted EDIT_INSTANCE permission via service share for service {}",
                        userContext.getUserId(), service.getId());
                return true;
            }
        }

        log.debug("User {} cannot create instance for service {} - not owned, not shared with EDIT_INSTANCE",
                userContext.getUserId(), service.getId());
        return false;
    }

    /**
     * Check if user can edit a service instance.
     * <p>
     * Similar to view permissions but requires EDIT_INSTANCE share permission.
     *
     * @param userContext the user context
     * @param instance    the service instance
     * @return true if user can edit the instance
     */
    public boolean canEditInstance(UserContext userContext, ServiceInstance instance) {
        log.debug("Checking if user {} can edit instance {} of service {}",
                userContext.getUserId(), instance.getInstanceId(), instance.getServiceId());

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
            List<ServiceShare.SharePermission> permissions = findEffectivePermissions(
                    userContext, instance.getServiceId(), List.of(instance.getEnvironment()));
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
     * @param request     the approval request
     * @param gate        the gate name
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
     * @param serviceId   the service ID
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
     * @param service     the application service
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
     * @param service     the application service
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
     * Requesters can cancel their own requests, system admins can cancel any
     * request.
     *
     * @param userContext the user context
     * @param request     the approval request
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
     * Users can view drift events if they belong to the owning team or have
     * explicit share permissions.
     * VIEW_INSTANCE permission on service also grants view on related drift_events.
     *
     * @param userContext the user context
     * @param driftEvent  the drift event
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
            List<ServiceShare.SharePermission> permissions = findEffectivePermissions(
                    userContext, driftEvent.getServiceId(), List.of(driftEvent.getEnvironment()));
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
     * @param driftEvent  the drift event
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
            List<ServiceShare.SharePermission> permissions = findEffectivePermissions(
                    userContext, driftEvent.getServiceId(), List.of(driftEvent.getEnvironment()));
            if (permissions.contains(ServiceShare.SharePermission.EDIT_INSTANCE)) {
                log.debug("User {} granted EDIT_INSTANCE permission via service share for drift event service {}",
                        userContext.getUserId(), driftEvent.getServiceId());
                return true;
            }
        }

        return false;
    }

    /**
     * Check if user can view drift events for a service.
     * <p>
     * Users can view drift events if they have team ownership or VIEW_DRIFT share
     * permission.
     *
     * @param userContext the user context
     * @param serviceId   the service ID
     * @param environment the environment
     * @return true if user can view drift events
     */
    public boolean canViewDrift(UserContext userContext, String serviceId, String environment) {
        log.debug("Checking if user {} can view drift for service {} in environment {}",
                userContext.getUserId(), serviceId, environment);

        // System admins can view all drift events
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Check service shares for VIEW_DRIFT permission
        List<ServiceShare.SharePermission> permissions = findEffectivePermissions(
                userContext, serviceId, List.of(environment));
        if (permissions.contains(ServiceShare.SharePermission.VIEW_DRIFT)) {
            log.debug("User {} granted VIEW_DRIFT permission via service share for service {}",
                    userContext.getUserId(), serviceId);
            return true;
        }

        return false;
    }

    /**
     * Check if user can restart a service instance.
     * <p>
     * Users can restart instances if they have team ownership or RESTART_INSTANCE
     * share permission.
     *
     * @param userContext the user context
     * @param instance    the service instance
     * @return true if user can restart the instance
     */
    public boolean canRestartInstance(UserContext userContext, ServiceInstance instance) {
        log.debug("Checking if user {} can restart instance {} of service {}",
                userContext.getUserId(), instance.getInstanceId());

        // System admins can restart all instances
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Team members can restart instances of services owned by their team
        if (instance.getTeamId() != null && userContext.isMemberOfTeam(instance.getTeamId())) {
            return true;
        }

        // Check service shares for RESTART_INSTANCE permission
        if (instance.getServiceId() != null) {
            List<ServiceShare.SharePermission> permissions = findEffectivePermissions(
                    userContext, instance.getServiceId(), List.of(instance.getEnvironment()));
            if (permissions.contains(ServiceShare.SharePermission.RESTART_INSTANCE)) {
                log.debug("User {} granted RESTART_INSTANCE permission via service share for service {}",
                        userContext.getUserId(), instance.getServiceId());
                return true;
            }
        }

        return false;
    }

    /**
     * Check if user can edit service metadata.
     * <p>
     * Users can edit service metadata if they have team ownership or EDIT_SERVICE
     * share permission.
     *
     * @param userContext the user context
     * @param service     the application service
     * @return true if user can edit the service
     */
    public boolean canEditServiceMetadata(UserContext userContext, ApplicationService service) {
        log.debug("Checking if user {} can edit service metadata for service {}",
                userContext.getUserId(), service.getId());

        // System admins can edit all services
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Team members can edit services owned by their team
        if (service.getOwnerTeamId() != null && userContext.isMemberOfTeam(service.getOwnerTeamId())) {
            return true;
        }

        // Check service shares for EDIT_SERVICE permission
        List<ServiceShare.SharePermission> permissions = findEffectivePermissions(
                userContext, service.getId().id(), List.of() // No environment filter for service metadata
        );
        if (permissions.contains(ServiceShare.SharePermission.EDIT_SERVICE)) {
            log.debug("User {} granted EDIT_SERVICE permission via service share for service {}",
                    userContext.getUserId(), service.getId());
            return true;
        }

        return false;
    }

    /**
     * Find effective permissions for a user on a service.
     * <p>
     * Combines direct user permissions and team-based permissions.
     *
     * @param userContext  the user context
     * @param serviceId    the service ID
     * @param environments the environments to check
     * @return list of effective permissions
     */
    public List<ServiceShare.SharePermission> findEffectivePermissions(UserContext userContext,
                                                                       String serviceId,
                                                                       List<String> environments) {
        log.debug("Finding effective permissions for user: {} on service: {} in environments: {}",
                userContext.getUserId(), serviceId, environments);

        return serviceShareRepository.findEffectivePermissions(
                userContext.getUserId(),
                userContext.getTeamIds(),
                serviceId,
                environments);
    }
}
