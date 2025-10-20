                                                                                                                                                                                                                                                                                                                                                                                                                                    package com.example.control.application.service;

import com.example.control.config.security.UserContext;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing user permissions and access control discovery.
 * <p>
 * Provides methods to determine what resources and actions a user can access,
 * which is used by the frontend to show/hide UI elements and enable/disable features.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionsService {

    private final ApplicationServiceService applicationServiceService;

    /**
     * Get comprehensive permissions for a user.
     * <p>
     * Returns a structured view of what the user can access, including:
     * - Team memberships and roles
     * - Accessible routes and actions
     * - Service ownership and sharing permissions
     *
     * @param userContext the user context
     * @return user permissions data
     */
    public UserPermissions getUserPermissions(UserContext userContext) {
        log.debug("Getting permissions for user: {}", userContext.getUserId());

        UserPermissions.UserPermissionsBuilder builder = UserPermissions.builder()
                .userId(userContext.getUserId())
                .username(userContext.getUsername())
                .email(userContext.getEmail())
                .fullName(userContext.getFullName())
                .teamIds(userContext.getTeamIds())
                .roles(userContext.getRoles())
                .isSysAdmin(userContext.isSysAdmin())
                .managerId(userContext.getManagerId());

        // Determine accessible routes based on roles and teams
        Set<String> accessibleRoutes = determineAccessibleRoutes(userContext);
        builder.accessibleRoutes(accessibleRoutes);

        // Determine available actions
        Map<String, Set<String>> actions = determineAvailableActions(userContext);
        builder.actions(actions);

        // Get service ownership information
        List<String> ownedServiceIds = getOwnedServiceIds(userContext);
        builder.ownedServiceIds(ownedServiceIds);

        // Get shared service information
        List<String> sharedServiceIds = getSharedServiceIds(userContext);
        builder.sharedServiceIds(sharedServiceIds);

        UserPermissions permissions = builder.build();
        log.debug("Generated permissions for user {}: {} routes, {} owned services, {} shared services",
                userContext.getUserId(), accessibleRoutes.size(), ownedServiceIds.size(), sharedServiceIds.size());

        return permissions;
    }

    /**
     * Determine which routes the user can access based on their roles and teams.
     *
     * @param userContext the user context
     * @return set of accessible route patterns
     */
    private Set<String> determineAccessibleRoutes(UserContext userContext) {
        Set<String> routes = new java.util.HashSet<>();

        // All authenticated users can access basic routes
        routes.add("/api/user/whoami");
        routes.add("/api/user/permissions");
        routes.add("/api/application-services");

        // System admins have full access
        if (userContext.isSysAdmin()) {
            routes.add("/api/service-instances/**");
            routes.add("/api/drift-events/**");
            routes.add("/api/approval-requests/**");
            routes.add("/api/service-shares/**");
            routes.add("/api/admin/**");
            return routes;
        }

        // Team members can access team-specific resources
        if (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty()) {
            routes.add("/api/service-instances");
            routes.add("/api/drift-events");
            routes.add("/api/approval-requests");
            routes.add("/api/service-shares");
        }

        return routes;
    }

    /**
     * Determine available actions for different resource types.
     *
     * @param userContext the user context
     * @return map of resource types to available actions
     */
    private Map<String, Set<String>> determineAvailableActions(UserContext userContext) {
        Map<String, Set<String>> actions = new java.util.HashMap<>();

        // Application Services actions
        Set<String> serviceActions = new java.util.HashSet<>();
        serviceActions.add("VIEW"); // All users can view
        if (userContext.isSysAdmin() || (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty())) {
            serviceActions.add("REQUEST_OWNERSHIP");
        }
        if (userContext.isSysAdmin()) {
            serviceActions.add("EDIT");
            serviceActions.add("DELETE");
        }
        actions.put("APPLICATION_SERVICE", serviceActions);

        // Service Instances actions
        Set<String> instanceActions = new java.util.HashSet<>();
        if (userContext.isSysAdmin() || (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty())) {
            instanceActions.add("VIEW");
            instanceActions.add("EDIT");
        }
        actions.put("SERVICE_INSTANCE", instanceActions);

        // Drift Events actions
        Set<String> driftActions = new java.util.HashSet<>();
        if (userContext.isSysAdmin() || (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty())) {
            driftActions.add("VIEW");
            driftActions.add("RESOLVE");
        }
        actions.put("DRIFT_EVENT", driftActions);

        // Approval Requests actions
        Set<String> approvalActions = new java.util.HashSet<>();
        if (userContext.isSysAdmin()) {
            approvalActions.add("VIEW");
            approvalActions.add("APPROVE");
            approvalActions.add("REJECT");
        }
        if (userContext.getUserId() != null) {
            approvalActions.add("CREATE");
            approvalActions.add("CANCEL");
        }
        actions.put("APPROVAL_REQUEST", approvalActions);

        // Service Shares actions
        Set<String> shareActions = new java.util.HashSet<>();
        if (userContext.isSysAdmin() || (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty())) {
            shareActions.add("VIEW");
            shareActions.add("CREATE");
            shareActions.add("EDIT");
            shareActions.add("DELETE");
        }
        actions.put("SERVICE_SHARE", shareActions);

        return actions;
    }

    /**
     * Get service IDs owned by the user's teams.
     *
     * @param userContext the user context
     * @return list of owned service IDs
     */
    private List<String> getOwnedServiceIds(UserContext userContext) {
        if (userContext.getTeamIds() == null || userContext.getTeamIds().isEmpty()) {
            return List.of();
        }

        return userContext.getTeamIds().stream()
                .flatMap(teamId -> applicationServiceService.findByOwnerTeam(teamId).stream())
                .map(service -> service.getId().id())
                .toList();
    }

    /**
     * Get service IDs shared with the user.
     *
     * @param userContext the user context
     * @return list of shared service IDs
     */
    private List<String> getSharedServiceIds(UserContext userContext) {
        // This is a simplified implementation
        // In a full implementation, you would query ServiceShareService
        // for services shared with the user's teams or directly with the user
        return List.of(); // Placeholder
    }

    /**
     * Data transfer object for user permissions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPermissions {
        private String userId;
        private String username;
        private String email;
        private String fullName;
        private List<String> teamIds;
        private List<String> roles;
        private boolean isSysAdmin;
        private String managerId;
        private Set<String> accessibleRoutes;
        private Map<String, Set<String>> actions;
        private List<String> ownedServiceIds;
        private List<String> sharedServiceIds;
    }
}