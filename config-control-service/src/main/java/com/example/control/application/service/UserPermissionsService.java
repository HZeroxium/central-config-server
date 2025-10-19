                                                                                                                                                                                                                                                                                                                                                                                                                                    package com.example.control.application.service;

import com.example.control.api.dto.UserDtos;
import com.example.control.config.security.UserContext;
import com.example.control.domain.ApplicationService;
import com.example.control.domain.ServiceShare;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for calculating user permissions and authorization matrix.
 * <p>
 * Provides business logic for determining what routes, services, and actions
 * a user can access based on their roles, team memberships, and service shares.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPermissionsService {

    private final ApplicationServiceService applicationServiceService;
    private final ServiceShareService serviceShareService;

    /**
     * Calculate comprehensive permission matrix for a user.
     * <p>
     * This method determines:
     * <ul>
     *   <li>Allowed frontend routes based on roles</li>
     *   <li>Service-specific permissions (VIEW, EDIT) based on ownership and shares</li>
     *   <li>System-wide capabilities (approval, management)</li>
     *   <li>List of services owned by user's teams</li>
     * </ul>
     *
     * @param userContext the user context
     * @return comprehensive permission matrix
     */
    @Cacheable(value = "user-permissions", key = "#userContext.userId")
    public UserDtos.PermissionMatrix calculatePermissions(UserContext userContext) {
        log.debug("Calculating permissions for user: {}", userContext.getUserId());

        // 1. Determine allowed routes based on roles
        List<String> allowedRoutes = calculateAllowedRoutes(userContext);

        // 2. Calculate service-specific permissions
        Map<String, List<String>> servicePermissions = calculateServicePermissions(userContext);

        // 3. Determine system-wide capabilities
        boolean canApproveRequests = userContext.hasRole("SYS_ADMIN");
        boolean canManageAllServices = userContext.hasRole("SYS_ADMIN");

        // 4. Find services owned by user's teams
        List<String> ownedServiceIds = calculateOwnedServices(userContext);

        UserDtos.PermissionMatrix matrix = new UserDtos.PermissionMatrix(
                allowedRoutes,
                servicePermissions,
                canApproveRequests,
                canManageAllServices,
                ownedServiceIds
        );

        log.debug("Calculated permissions for user {}: {} routes, {} services, {} owned services",
                userContext.getUserId(), allowedRoutes.size(), servicePermissions.size(), ownedServiceIds.size());

        return matrix;
    }

    /**
     * Calculate allowed frontend routes based on user roles.
     *
     * @param userContext the user context
     * @return list of allowed routes
     */
    private List<String> calculateAllowedRoutes(UserContext userContext) {
        List<String> routes = new ArrayList<>();

        // Base routes for all authenticated users
        routes.add("/dashboard");
        routes.add("/services");
        routes.add("/instances");
        routes.add("/drift-events");
        routes.add("/profile");

        // Admin routes
        if (userContext.hasRole("SYS_ADMIN")) {
            routes.add("/admin");
            routes.add("/admin/services");
            routes.add("/admin/approvals");
            routes.add("/admin/users");
            routes.add("/admin/teams");
            routes.add("/admin/settings");
        }

        // User-specific routes
        routes.add("/my-requests");
        routes.add("/my-shares");

        return routes;
    }

    /**
     * Calculate service-specific permissions based on ownership and shares.
     *
     * @param userContext the user context
     * @return map of serviceId -> list of permissions
     */
    private Map<String, List<String>> calculateServicePermissions(UserContext userContext) {
        Map<String, List<String>> servicePermissions = new HashMap<>();

        // Get all services
        List<ApplicationService> allServices = applicationServiceService.findAll();

        for (ApplicationService service : allServices) {
            List<String> permissions = new ArrayList<>();

            // Check ownership
            if (userContext.isMemberOfTeam(service.getOwnerTeamId())) {
                permissions.add("VIEW");
                permissions.add("EDIT");
                permissions.add("MANAGE_SHARES");
            } else {
                // Check for shared permissions
                List<ServiceShare.SharePermission> effectivePermissions = serviceShareService
                        .findEffectivePermissions(userContext, service.getId().id(), List.of());

                if (effectivePermissions.contains(ServiceShare.SharePermission.VIEW_INSTANCE)) {
                    permissions.add("VIEW");
                }
                if (effectivePermissions.contains(ServiceShare.SharePermission.EDIT_INSTANCE)) {
                    permissions.add("EDIT");
                }
            }

            // System admins have all permissions
            if (userContext.hasRole("SYS_ADMIN")) {
                if (!permissions.contains("VIEW")) permissions.add("VIEW");
                if (!permissions.contains("EDIT")) permissions.add("EDIT");
                if (!permissions.contains("MANAGE_SHARES")) permissions.add("MANAGE_SHARES");
            }

            if (!permissions.isEmpty()) {
                servicePermissions.put(service.getId().id(), permissions);
            }
        }

        return servicePermissions;
    }

    /**
     * Calculate services owned by user's teams.
     *
     * @param userContext the user context
     * @return list of owned service IDs
     */
    private List<String> calculateOwnedServices(UserContext userContext) {
        List<String> ownedServiceIds = new ArrayList<>();

        // Get all services
        List<ApplicationService> allServices = applicationServiceService.findAll();

        for (ApplicationService service : allServices) {
            if (userContext.isMemberOfTeam(service.getOwnerTeamId())) {
                ownedServiceIds.add(service.getId().id());
            }
        }

        return ownedServiceIds;
    }
}
