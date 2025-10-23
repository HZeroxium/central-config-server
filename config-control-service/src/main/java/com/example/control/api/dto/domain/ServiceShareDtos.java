package com.example.control.api.dto.domain;

import com.example.control.api.dto.common.PageDtos;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for ServiceShare API operations.
 * <p>
 * Provides request/response DTOs for managing service shares with
 * proper validation and JSON serialization.
 * </p>
 */
@Schema(name = "ServiceShareDtos", description = "DTOs for ServiceShare API operations")
public class ServiceShareDtos {

    /**
     * Request DTO for creating a new service share.
     */
    @Schema(name = "ServiceShareCreateRequest", description = "Request to create a new service share")
    public record CreateRequest(
            @NotBlank(message = "Service ID is required")
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            @Schema(description = "Service ID to share", example = "payment-service", maxLength = 100)
            String serviceId,

            @NotBlank(message = "Grant to type is required")
            @Size(max = 20, message = "Grant to type must not exceed 20 characters")
            @Schema(description = "Type of grantee", example = "TEAM", allowableValues = {"TEAM", "USER"})
            String grantToType,

            @NotBlank(message = "Grant to ID is required")
            @Size(max = 100, message = "Grant to ID must not exceed 100 characters")
            @Schema(description = "ID of the grantee (team or user)", example = "team_analytics", maxLength = 100)
            String grantToId,

            @NotNull(message = "Permissions list is required")
            @Size(min = 1, message = "At least one permission must be specified")
            @Schema(description = "List of permissions to grant", example = "[\"VIEW_INSTANCE\", \"VIEW_DRIFT\"]")
            List<String> permissions,

            @Schema(description = "Environments where permissions apply", example = "[\"dev\", \"staging\"]")
            List<String> environments,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            @Schema(description = "Optional expiration timestamp", example = "2024-12-31T23:59:59.999Z")
            Instant expiresAt
    ) {}

    /**
     * Response DTO for service share details.
     */
    @Schema(name = "ServiceShareResponse", description = "Service share details response")
    public record Response(
            @JsonProperty("id")
            @Schema(description = "Unique share identifier", example = "share-12345")
            String id,

            @JsonProperty("resourceLevel")
            @Schema(description = "Resource level of the share", example = "SERVICE")
            String resourceLevel,

            @JsonProperty("serviceId")
            @Schema(description = "Service ID being shared", example = "payment-service")
            String serviceId,

            @JsonProperty("grantToType")
            @Schema(description = "Type of grantee", example = "TEAM", allowableValues = {"TEAM", "USER"})
            String grantToType,

            @JsonProperty("grantToId")
            @Schema(description = "ID of the grantee", example = "team_analytics")
            String grantToId,

            @JsonProperty("permissions")
            @Schema(description = "Granted permissions", example = "[\"VIEW_INSTANCE\", \"VIEW_DRIFT\"]")
            List<String> permissions,

            @JsonProperty("environments")
            @Schema(description = "Environments where permissions apply", example = "[\"dev\", \"staging\"]")
            List<String> environments,

            @JsonProperty("grantedBy")
            @Schema(description = "User who granted the share", example = "user1")
            String grantedBy,

            @JsonProperty("createdAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            @Schema(description = "Share creation timestamp", example = "2024-01-15T10:30:45.123Z")
            Instant createdAt,

            @JsonProperty("expiresAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            @Schema(description = "Share expiration timestamp", example = "2024-12-31T23:59:59.999Z")
            Instant expiresAt
    ) {}

    /**
     * Query filter DTO for searching service shares.
     */
    @Schema(name = "ServiceShareQueryFilter", description = "Query filter for searching service shares")
    public record QueryFilter(
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            @Schema(description = "Filter by service ID", example = "payment-service", maxLength = 100)
            String serviceId,

            @Size(max = 20, message = "Grant to type must not exceed 20 characters")
            @Schema(description = "Filter by grantee type", example = "TEAM", allowableValues = {"TEAM", "USER"})
            String grantToType,

            @Size(max = 100, message = "Grant to ID must not exceed 100 characters")
            @Schema(description = "Filter by grantee ID", example = "team_analytics", maxLength = 100)
            String grantToId,

            @Schema(description = "Filter by environments", example = "[\"dev\", \"staging\"]")
            List<String> environments
    ) {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ServiceSharePageResponse", description = "Page response for service shares")
    public static class ServiceSharePageResponse {
        @Schema(description = "List of service shares in current page")
        private List<ServiceShareDtos.Response> items;
        
        @Schema(description = "Pagination metadata")
        private PageDtos.PageMetadata metadata;
    }
}