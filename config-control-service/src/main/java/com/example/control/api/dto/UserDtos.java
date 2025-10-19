package com.example.control.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
}