package com.example.control.api.dto.domain;

import com.example.control.api.dto.common.PageDtos;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Schema(name = "ApplicationServiceDtos", description = "DTOs for ApplicationService API operations")
public class ApplicationServiceDtos {

    /**
     * Request DTO for creating a new application service.
     */
    @Schema(name = "ApplicationServiceCreateRequest", description = "Request to create a new application service")
    public record CreateRequest(
            @NotBlank(message = "Service ID is required")
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            @Schema(description = "Unique service identifier", example = "payment-service", maxLength = 100)
            String id,

            @NotBlank(message = "Display name is required")
            @Size(max = 200, message = "Display name must not exceed 200 characters")
            @Schema(description = "Human-readable service name", example = "Payment Service", maxLength = 200)
            String displayName,

            @Size(max = 100, message = "Owner team ID must not exceed 100 characters")
            @Schema(description = "Team that owns this service (null for orphaned services, only SYS_ADMIN can create orphaned)", example = "team_core", maxLength = 100)
            String ownerTeamId,

            @Size(min = 1, message = "If provided, at least one environment must be specified")
            @Schema(description = "List of environments where this service is deployed (defaults to [\"dev\", \"staging\", \"prod\"] if not provided)",
                    example = "[\"dev\", \"staging\", \"prod\"]")
            List<String> environments,

            @Schema(description = "Optional tags for categorization", example = "[\"microservice\", \"payment\", \"critical\"]")
            List<String> tags,

            @Size(max = 500, message = "Repository URL must not exceed 500 characters")
            @Schema(description = "Git repository URL", example = "https://github.com/company/payment-service", maxLength = 500)
            String repoUrl,

            @Schema(description = "Additional service attributes", example = "{\"version\": \"1.2.0\", \"framework\": \"spring-boot\"}")
            Map<String, String> attributes
    ) {
    }

    /**
     * Request DTO for updating an existing application service.
     */
    @Schema(name = "ApplicationServiceUpdateRequest", description = "Request to update an existing application service")
    public record UpdateRequest(
            @Size(max = 200, message = "Display name must not exceed 200 characters")
            @Schema(description = "Human-readable service name", example = "Payment Service v2", maxLength = 200)
            String displayName,

            @Size(max = 50, message = "Lifecycle must not exceed 50 characters")
            @Schema(description = "Service lifecycle stage", example = "ACTIVE", allowableValues = {"ACTIVE", "DEPRECATED", "RETIRED"})
            String lifecycle,

            @Schema(description = "Optional tags for categorization", example = "[\"microservice\", \"payment\", \"critical\", \"updated\"]")
            List<String> tags,

            @Size(max = 500, message = "Repository URL must not exceed 500 characters")
            @Schema(description = "Git repository URL", example = "https://github.com/company/payment-service", maxLength = 500)
            String repoUrl,

            @Schema(description = "Additional service attributes", example = "{\"version\": \"2.0.0\", \"framework\": \"spring-boot\", \"javaVersion\": \"17\"}")
            Map<String, String> attributes
    ) {
    }

    /**
     * Response DTO for application service details.
     */
    @Schema(name = "ApplicationServiceResponse", description = "Application service details response")
    public record Response(
            @JsonProperty("id")
            @Schema(description = "Unique service identifier", example = "payment-service")
            String id,

            @JsonProperty("displayName")
            @Schema(description = "Human-readable service name", example = "Payment Service")
            String displayName,

            @JsonProperty("ownerTeamId")
            @Schema(description = "Team that owns this service", example = "team_core")
            String ownerTeamId,

            @JsonProperty("environments")
            @Schema(description = "List of environments where this service is deployed", example = "[\"dev\", \"staging\", \"prod\"]")
            List<String> environments,

            @JsonProperty("tags")
            @Schema(description = "Service tags for categorization", example = "[\"microservice\", \"payment\", \"critical\"]")
            List<String> tags,

            @JsonProperty("repoUrl")
            @Schema(description = "Git repository URL", example = "https://github.com/company/payment-service")
            String repoUrl,

            @JsonProperty("lifecycle")
            @Schema(description = "Service lifecycle stage", example = "ACTIVE", allowableValues = {"ACTIVE", "DEPRECATED", "RETIRED"})
            String lifecycle,

            @JsonProperty("createdAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            @Schema(description = "Service creation timestamp", example = "2024-01-15T10:30:45.123Z")
            Instant createdAt,

            @JsonProperty("updatedAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            @Schema(description = "Service last update timestamp", example = "2024-01-15T14:22:18.456Z")
            Instant updatedAt,

            @JsonProperty("createdBy")
            @Schema(description = "User who created this service", example = "user1")
            String createdBy,

            @JsonProperty("attributes")
            @Schema(description = "Additional service attributes", example = "{\"version\": \"1.2.0\", \"framework\": \"spring-boot\"}")
            Map<String, String> attributes
    ) {
    }

    /**
     * Query filter DTO for searching application services.
     */
    @Schema(name = "ApplicationServiceQueryFilter", description = "Query filter for searching application services")
    public record QueryFilter(
            @Size(max = 100, message = "Owner team ID must not exceed 100 characters")
            @Schema(description = "Filter by owner team ID", example = "team_core", maxLength = 100)
            String ownerTeamId,

            @Size(max = 50, message = "Lifecycle must not exceed 50 characters")
            @Schema(description = "Filter by lifecycle stage", example = "ACTIVE", allowableValues = {"ACTIVE", "DEPRECATED", "RETIRED"})
            String lifecycle,

            @Schema(description = "Filter by tags", example = "[\"microservice\", \"payment\"]")
            List<String> tags,

            @Size(max = 200, message = "Search term must not exceed 200 characters")
            @Schema(description = "Search term for service name or description", example = "payment", maxLength = 200)
            String search
    ) {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ApplicationServicePageResponse", description = "Page response for application services")
    public static class ApplicationServicePageResponse {
        @Schema(description = "List of application services in current page")
        private List<ApplicationServiceDtos.Response> items;

        @Schema(description = "Pagination metadata")
        private PageDtos.PageMetadata metadata;
    }

}