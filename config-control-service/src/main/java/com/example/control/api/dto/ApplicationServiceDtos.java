package com.example.control.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTOs for ApplicationService API operations.
 * <p>
 * Provides request/response DTOs for creating, updating, and querying
 * application services with proper validation and JSON serialization.
 * </p>
 */
public class ApplicationServiceDtos {

    /**
     * Request DTO for creating a new application service.
     */
    public record CreateRequest(
            @NotBlank(message = "Service ID is required")
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            String id,

            @NotBlank(message = "Display name is required")
            @Size(max = 200, message = "Display name must not exceed 200 characters")
            String displayName,

            @NotBlank(message = "Owner team ID is required")
            @Size(max = 100, message = "Owner team ID must not exceed 100 characters")
            String ownerTeamId,

            @NotNull(message = "Environments list is required")
            @Size(min = 1, message = "At least one environment must be specified")
            List<String> environments,

            List<String> tags,

            @Size(max = 500, message = "Repository URL must not exceed 500 characters")
            String repoUrl,

            Map<String, String> attributes
    ) {}

    /**
     * Request DTO for updating an existing application service.
     */
    public record UpdateRequest(
            @Size(max = 200, message = "Display name must not exceed 200 characters")
            String displayName,

            @Size(max = 50, message = "Lifecycle must not exceed 50 characters")
            String lifecycle,

            List<String> tags,

            @Size(max = 500, message = "Repository URL must not exceed 500 characters")
            String repoUrl,

            Map<String, String> attributes
    ) {}

    /**
     * Response DTO for application service details.
     */
    public record Response(
            @JsonProperty("id")
            String id,

            @JsonProperty("displayName")
            String displayName,

            @JsonProperty("ownerTeamId")
            String ownerTeamId,

            @JsonProperty("environments")
            List<String> environments,

            @JsonProperty("tags")
            List<String> tags,

            @JsonProperty("repoUrl")
            String repoUrl,

            @JsonProperty("lifecycle")
            String lifecycle,

            @JsonProperty("createdAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant createdAt,

            @JsonProperty("updatedAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant updatedAt,

            @JsonProperty("createdBy")
            String createdBy,

            @JsonProperty("attributes")
            Map<String, String> attributes
    ) {}

    /**
     * Query filter DTO for searching application services.
     */
    public record QueryFilter(
            @Size(max = 100, message = "Owner team ID must not exceed 100 characters")
            String ownerTeamId,

            @Size(max = 50, message = "Lifecycle must not exceed 50 characters")
            String lifecycle,

            List<String> tags,

            @Size(max = 200, message = "Search term must not exceed 200 characters")
            String search
    ) {}
}