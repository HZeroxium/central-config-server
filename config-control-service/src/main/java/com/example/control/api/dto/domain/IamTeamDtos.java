package com.example.control.api.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for IAM team operations.
 * <p>
 * Provides DTOs for Keycloak group projection and synchronization
 * with team-based access control.
 * </p>
 */
@Schema(name = "IamTeamDtos", description = "DTOs for IAM team API operations")
public class IamTeamDtos {

    /**
     * Response DTO for IAM team.
     */
    @Data
    @Builder
    @Schema(name = "IamTeamResponse", description = "IAM team details response")
    public static class Response {
        @Schema(description = "Unique team identifier", example = "team_core")
        private String teamId;
        
        @Schema(description = "Team display name", example = "Core Team")
        private String displayName;
        
        @Schema(description = "List of team member user IDs", example = "[\"user1\", \"user2\", \"user3\"]")
        private List<String> members;
        
        @Schema(description = "Team creation timestamp", example = "2024-01-15T10:30:45.123Z")
        private Instant createdAt;
        
        @Schema(description = "Team last update timestamp", example = "2024-01-15T14:30:45.123Z")
        private Instant updatedAt;
        
        @Schema(description = "Last sync timestamp from Keycloak", example = "2024-01-15T14:30:45.123Z")
        private Instant syncedAt;
    }

    /**
     * Query filter DTO for IAM teams.
     */
    @Data
    @Builder
    @Schema(name = "IamTeamQueryFilter", description = "Query filter for searching IAM teams")
    public static class QueryFilter {
        @Schema(description = "Filter by team display name", example = "Core Team")
        private String displayName;
        
        @Schema(description = "Filter by team members", example = "[\"user1\", \"user2\"]")
        private List<String> members;
    }

    /**
     * Statistics response DTO.
     */
    @Data
    @Builder
    @Schema(name = "IamTeamStatsResponse", description = "IAM team statistics response")
    public static class StatsResponse {
        @Schema(description = "Total number of teams", example = "5")
        private long totalTeams;
    }
}
