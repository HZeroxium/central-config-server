package com.example.control.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for IAM team operations.
 */
public class IamTeamDtos {

    /**
     * Response DTO for IAM team.
     */
    @Data
    @Builder
    public static class Response {
        private String teamId;
        private String displayName;
        private List<String> members;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant syncedAt;
    }

    /**
     * Query filter DTO for IAM teams.
     */
    @Data
    @Builder
    public static class QueryFilter {
        private String displayName;
        private List<String> members;
    }

    /**
     * Statistics response DTO.
     */
    @Data
    @Builder
    public static class StatsResponse {
        private long totalTeams;
    }
}
