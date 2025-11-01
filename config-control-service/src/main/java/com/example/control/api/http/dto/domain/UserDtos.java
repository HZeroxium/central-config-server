package com.example.control.api.http.dto.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "UserDtos", description = "DTOs for User API operations")
public class UserDtos {

    /**
     * Response DTO for current user information.
     */
    @Schema(name = "MeResponse", description = "Current user information response")
    public record MeResponse(
            @JsonProperty("userId")
            @Schema(description = "Unique user identifier", example = "user1")
            String userId,

            @JsonProperty("username")
            @Schema(description = "Username", example = "john.doe")
            String username,

            @JsonProperty("email")
            @Schema(description = "User email address", example = "john.doe@company.com")
            String email,

            @JsonProperty("firstName")
            @Schema(description = "User first name", example = "John")
            String firstName,

            @JsonProperty("lastName")
            @Schema(description = "User last name", example = "Doe")
            String lastName,

            @JsonProperty("teamIds")
            @Schema(description = "List of team IDs user belongs to", example = "[\"team_core\"]")
            List<String> teamIds,

            @JsonProperty("roles")
            @Schema(description = "User roles", example = "[\"USER\"]")
            List<String> roles,

            @JsonProperty("managerId")
            @Schema(description = "Manager user ID", example = "manager1")
            String managerId
    ) {
    }

    /**
     * Response DTO for user permissions and allowed routes.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PermissionsResponse", description = "User permissions and allowed routes response")
    public static class PermissionsResponse {
        @JsonProperty("allowedApiRoutes")
        @Schema(description = "API routes user can access", example = "[\"/api/application-services\", \"/api/service-instances\"]")
        private List<String> allowedApiRoutes;

        @JsonProperty("allowedUiRoutes")
        @Schema(description = "UI routes user can access", example = "[\"/dashboard\", \"/services\"]")
        private List<String> allowedUiRoutes;

        @JsonProperty("roles")
        @Schema(description = "User roles", example = "[\"USER\"]")
        private List<String> roles;

        @JsonProperty("teams")
        @Schema(description = "User team memberships", example = "[\"team_core\"]")
        private List<String> teams;

        @JsonProperty("features")
        @Schema(description = "Feature flags for user", example = "{\"advanced_analytics\": true, \"beta_features\": false}")
        private Map<String, Boolean> features;

        @JsonProperty("actions")
        @Schema(description = "Actions user can perform", example = "{\"service\": [\"VIEW\", \"EDIT\"], \"approval\": [\"APPROVE\"]}")
        private Map<String, List<String>> actions;

        @JsonProperty("ownedServiceIds")
        @Schema(description = "Service IDs owned by user's teams", example = "[\"payment-service\", \"user-service\"]")
        private List<String> ownedServiceIds;

        @JsonProperty("sharedServiceIds")
        @Schema(description = "Service IDs shared with user", example = "[\"analytics-service\"]")
        private List<String> sharedServiceIds;
    }

    /**
     * Permission matrix for frontend routing and authorization.
     * <p>
     * Provides structured permissions for frontend to determine
     * what routes and actions the user can access.
     * </p>
     */
    @Schema(name = "PermissionMatrix", description = "Permission matrix for frontend routing and authorization")
    public record PermissionMatrix(
            @JsonProperty("routes")
            @Schema(description = "Allowed frontend routes", example = "[\"/dashboard\", \"/services\", \"/approvals\"]")
            List<String> routes,           // Allowed frontend routes

            @JsonProperty("servicePermissions")
            @Schema(description = "Service-specific permissions", example = "{\"payment-service\": [\"VIEW\", \"EDIT\"], \"user-service\": [\"VIEW\"]}")
            Map<String, List<String>> servicePermissions,  // serviceId -> [VIEW, EDIT]

            @JsonProperty("canApproveRequests")
            @Schema(description = "Whether user can approve requests", example = "true")
            boolean canApproveRequests,

            @JsonProperty("canManageAllServices")
            @Schema(description = "Whether user can manage all services", example = "false")
            boolean canManageAllServices,

            @JsonProperty("ownedServiceIds")
            @Schema(description = "Service IDs owned by user's teams", example = "[\"payment-service\", \"user-service\"]")
            List<String> ownedServiceIds   // Services user's teams own
    ) {
    }
}