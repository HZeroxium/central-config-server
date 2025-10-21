package com.example.control.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for IAM user operations.
 */
public class IamUserDtos {

    /**
     * Response DTO for IAM user.
     */
    @Data
    @Builder
    public static class Response {
        private String userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> teamIds;
        private String managerId;
        private List<String> roles;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant syncedAt;
    }

    /**
     * Query filter DTO for IAM users.
     */
    @Data
    @Builder
    public static class QueryFilter {
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> teamIds;
        private String managerId;
        private List<String> roles;
    }

    /**
     * Statistics response DTO.
     */
    @Data
    @Builder
    public static class StatsResponse {
        private long totalUsers;
        private long totalTeams;
    }
}
