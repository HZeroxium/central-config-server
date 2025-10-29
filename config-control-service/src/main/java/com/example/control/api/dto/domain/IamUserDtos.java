package com.example.control.api.dto.domain;

import com.example.control.api.dto.common.PageDtos;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for IAM user operations.
 * <p>
 * Provides DTOs for Keycloak user projection and synchronization
 * with team-based access control.
 * </p>
 */
@Schema(name = "IamUserDtos", description = "DTOs for IAM user API operations")
public class IamUserDtos {

    private IamUserDtos() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Response DTO for IAM user.
     */
    @Data
    @Builder
    @Schema(name = "IamUserResponse", description = "IAM user details response")
    public static class Response {
        @Schema(description = "Unique user identifier", example = "user1")
        private String userId;

        @Schema(description = "Username", example = "john.doe")
        private String username;

        @Schema(description = "User email address", example = "john.doe@company.com")
        private String email;

        @Schema(description = "User first name", example = "John")
        private String firstName;

        @Schema(description = "User last name", example = "Doe")
        private String lastName;

        @Schema(description = "List of team IDs user belongs to", example = "[\"team_core\"]")
        private List<String> teamIds;

        @Schema(description = "Manager user ID", example = "manager1")
        private String managerId;

        @Schema(description = "User roles", example = "[\"USER\"]")
        private List<String> roles;

        @Schema(description = "User creation timestamp", example = "2024-01-15T10:30:45.123Z")
        private Instant createdAt;

        @Schema(description = "User last update timestamp", example = "2024-01-15T14:30:45.123Z")
        private Instant updatedAt;

        @Schema(description = "Last sync timestamp from Keycloak", example = "2024-01-15T14:30:45.123Z")
        private Instant syncedAt;
    }

    /**
     * Query filter DTO for IAM users.
     */
    @Data
    @Builder
    @Schema(name = "IamUserQueryFilter", description = "Query filter for searching IAM users")
    public static class QueryFilter {
        @Schema(description = "Filter by username", example = "john.doe")
        private String username;

        @Schema(description = "Filter by email", example = "john.doe@company.com")
        private String email;

        @Schema(description = "Filter by first name", example = "John")
        private String firstName;

        @Schema(description = "Filter by last name", example = "Doe")
        private String lastName;

        @Schema(description = "Filter by team IDs", example = "[\"team_core\"]")
        private List<String> teamIds;

        @Schema(description = "Filter by manager ID", example = "manager1")
        private String managerId;

        @Schema(description = "Filter by roles", example = "[\"USER\"]")
        private List<String> roles;
    }

    /**
     * Statistics response DTO.
     */
    @Data
    @Builder
    @Schema(name = "IamUserStatsResponse", description = "IAM statistics response")
    public static class StatsResponse {
        @Schema(description = "Total number of users", example = "150")
        private long totalUsers;

        @Schema(description = "Total number of teams", example = "5")
        private long totalTeams;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "IamUserPageResponse", description = "Page response for IAM users")
    public static class IamUserPageResponse {
        @Schema(description = "List of IAM users in current page")
        private List<IamUserDtos.Response> items;

        @Schema(description = "Pagination metadata")
        private PageDtos.PageMetadata metadata;
    }
}
