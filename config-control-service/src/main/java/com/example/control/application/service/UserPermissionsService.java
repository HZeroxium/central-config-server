package com.example.control.application.service;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.ServiceShareQueryService;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ServiceShare;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing user permissions and access control discovery.
 * <p>
 * Provides methods to determine what resources and actions a user can access,
 * which is used by the frontend to show/hide UI elements and enable/disable
 * features.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionsService {

    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final ServiceShareQueryService serviceShareQueryService;

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
        Set<String> accessibleApiRoutes = determineAccessibleApiRoutes(userContext);
        Set<String> accessibleUiRoutes = determineAccessibleUiRoutes(userContext);
        builder.accessibleApiRoutes(accessibleApiRoutes);
        builder.accessibleUiRoutes(accessibleUiRoutes);

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
        log.debug(
                "Generated permissions for user {}: {} API routes, {} UI routes, {} owned services, {} shared services",
                userContext.getUserId(), accessibleApiRoutes.size(), accessibleUiRoutes.size(), ownedServiceIds.size(),
                sharedServiceIds.size());

        return permissions;
    }

    /**
     * Determine which API routes the user can access based on their roles and
     * teams.
     *
     * @param userContext the user context
     * @return set of accessible API route patterns
     */
    private Set<String> determineAccessibleApiRoutes(UserContext userContext) {
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
     * Determine which UI routes the user can access based on their roles and teams.
     *
     * @param userContext the user context
     * @return set of accessible UI route patterns
     */
    private Set<String> determineAccessibleUiRoutes(UserContext userContext) {
        Set<String> uiRoutes = new java.util.HashSet<>();

        // All authenticated users can access basic routes
        uiRoutes.add("/dashboard");
        uiRoutes.add("/profile");

        // System admins have full access
        if (userContext.isSysAdmin()) {
            uiRoutes.add("/services");
            uiRoutes.add("/instances");
            uiRoutes.add("/drift-events");
            uiRoutes.add("/approvals");
            uiRoutes.add("/service-shares");
            uiRoutes.add("/iam/users");
            uiRoutes.add("/iam/teams");
            return uiRoutes;
        }

        // Team members can access team-specific resources
        if (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty()) {
            uiRoutes.add("/services");
            uiRoutes.add("/instances");
            uiRoutes.add("/drift-events");
            uiRoutes.add("/approvals");
            uiRoutes.add("/service-shares");
        }

        return uiRoutes;
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
                .flatMap(teamId -> {
                    ApplicationServiceCriteria criteria = ApplicationServiceCriteria.forTeam(teamId);
                    return applicationServiceQueryService.findAll(criteria, Pageable.unpaged())
                            .getContent().stream();
                })
                .map(service -> ((ApplicationService) service).getId().id())
                .toList();
    }

    /**
     * Get service IDs shared with the user.
     *
     * @param userContext the user context
     * @return list of shared service IDs
     */
    @Cacheable(value = "user-shared-services", key = "#userContext.userId")
    private List<String> getSharedServiceIds(UserContext userContext) {
        if (userContext.getTeamIds() == null || userContext.getTeamIds().isEmpty()) {
            return List.of();
        }

        ServiceShareCriteria criteria = ServiceShareCriteria.forTeams(userContext.getTeamIds());
        List<ServiceShare> shares = serviceShareQueryService.findAll(criteria, Pageable.unpaged()).getContent();
        return shares.stream()
                .map(ServiceShare::getServiceId)
                .distinct()
                .toList();
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
        private Set<String> accessibleApiRoutes;
        private Set<String> accessibleUiRoutes;
        private Map<String, Set<String>> actions;
        private List<String> ownedServiceIds;
        private List<String> sharedServiceIds;
    }
}