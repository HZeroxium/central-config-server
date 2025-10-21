package com.example.control.api.dto.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for User API operations.
 * <p>
 * Provides response DTOs for user information with
 * proper JSON serialization.
 * </p>
 */
public class UserDtos {

    /**
     * Response DTO for current user information.
     */
    public record MeResponse(
            @JsonProperty("userId")
            String userId,

            @JsonProperty("username")
            String username,

            @JsonProperty("email")
            String email,

            @JsonProperty("firstName")
            String firstName,

            @JsonProperty("lastName")
            String lastName,

            @JsonProperty("teamIds")
            List<String> teamIds,

            @JsonProperty("roles")
            List<String> roles,

            @JsonProperty("managerId")
            String managerId
    ) {}

    /**
     * Response DTO for user permissions and allowed routes.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionsResponse {
        @JsonProperty("allowedRoutes")
        private List<String> allowedRoutes;

        @JsonProperty("roles")
        private List<String> roles;

        @JsonProperty("teams")
        private List<String> teams;

        @JsonProperty("features")
        private Map<String, Boolean> features;
    }

    /**
     * Permission matrix for frontend routing and authorization.
     * <p>
     * Provides structured permissions for frontend to determine
     * what routes and actions the user can access.
     * </p>
     */
    public record PermissionMatrix(
            @JsonProperty("routes")
            List<String> routes,           // Allowed frontend routes
            
            @JsonProperty("servicePermissions")
            Map<String, List<String>> servicePermissions,  // serviceId -> [VIEW, EDIT]
            
            @JsonProperty("canApproveRequests")
            boolean canApproveRequests,
            
            @JsonProperty("canManageAllServices")
            boolean canManageAllServices,
            
            @JsonProperty("ownedServiceIds")
            List<String> ownedServiceIds   // Services user's teams own
    ) {}
}