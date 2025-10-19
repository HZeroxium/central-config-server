package com.example.control.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for ServiceShare API operations.
 * <p>
 * Provides request/response DTOs for managing service shares with
 * proper validation and JSON serialization.
 * </p>
 */
public class ServiceShareDtos {

    /**
     * Request DTO for creating a new service share.
     */
    public record CreateRequest(
            @NotBlank(message = "Service ID is required")
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            String serviceId,

            @NotBlank(message = "Grant to type is required")
            @Size(max = 20, message = "Grant to type must not exceed 20 characters")
            String grantToType,

            @NotBlank(message = "Grant to ID is required")
            @Size(max = 100, message = "Grant to ID must not exceed 100 characters")
            String grantToId,

            @NotNull(message = "Permissions list is required")
            @Size(min = 1, message = "At least one permission must be specified")
            List<String> permissions,

            List<String> environments,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant expiresAt
    ) {}

    /**
     * Response DTO for service share details.
     */
    public record Response(
            @JsonProperty("id")
            String id,

            @JsonProperty("resourceLevel")
            String resourceLevel,

            @JsonProperty("serviceId")
            String serviceId,

            @JsonProperty("grantToType")
            String grantToType,

            @JsonProperty("grantToId")
            String grantToId,

            @JsonProperty("permissions")
            List<String> permissions,

            @JsonProperty("environments")
            List<String> environments,

            @JsonProperty("grantedBy")
            String grantedBy,

            @JsonProperty("createdAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant createdAt,

            @JsonProperty("expiresAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant expiresAt
    ) {}

    /**
     * Query filter DTO for searching service shares.
     */
    public record QueryFilter(
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            String serviceId,

            @Size(max = 20, message = "Grant to type must not exceed 20 characters")
            String grantToType,

            @Size(max = 100, message = "Grant to ID must not exceed 100 characters")
            String grantToId,

            List<String> environments
    ) {}
}