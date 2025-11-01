package com.example.control.api.http.dto.domain;

import com.example.control.api.http.dto.common.PageDtos;
import com.example.control.domain.model.ServiceInstance;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;


/**
 * DTOs for ServiceInstance API operations.
 * <p>
 * Provides request/response DTOs for managing service instances with
 * drift detection and configuration tracking.
 * </p>
 */
@Data
@Schema(name = "ServiceInstanceDtos", description = "DTOs for ServiceInstance API operations")
public final class ServiceInstanceDtos {

    private ServiceInstanceDtos() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ServiceInstanceCreateRequest", description = "Request to create a new service instance")
    public static class CreateRequest {
        @NotBlank
        @Schema(description = "ID of the service", example = "payment-service")
        private String serviceId;

        @NotBlank
        @Schema(description = "Unique instance identifier", example = "payment-dev-1")
        private String instanceId;

        @Schema(description = "Instance host address", example = "payment-dev-1.internal.company.com")
        private String host;

        @Positive
        @Schema(description = "Instance port number", example = "8080")
        private Integer port;

        @Schema(description = "Deployment environment", example = "dev", allowableValues = {"dev", "staging", "prod"})
        private String environment;

        @Schema(description = "Service version", example = "1.2.0")
        private String version;

        @Schema(description = "Current configuration hash", example = "abc123def456")
        private String configHash;

        @Schema(description = "Last applied configuration hash", example = "abc123def456")
        private String lastAppliedHash;

        @Schema(description = "Expected configuration hash", example = "abc123def456")
        private String expectedHash;

        @Schema(description = "Instance metadata", example = "{\"region\": \"us-east-1\", \"zone\": \"us-east-1a\"}")
        private Map<String, String> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ServiceInstanceUpdateRequest", description = "Request to update an existing service instance")
    public static class UpdateRequest {
        @Schema(description = "Instance host address", example = "payment-dev-1.internal.company.com")
        private String host;

        @Positive
        @Schema(description = "Instance port number", example = "8080")
        private Integer port;

        @Schema(description = "Deployment environment", example = "dev", allowableValues = {"dev", "staging", "prod"})
        private String environment;

        @Schema(description = "Service version", example = "1.2.1")
        private String version;

        @Schema(description = "Current configuration hash", example = "def456ghi789")
        private String configHash;

        @Schema(description = "Last applied configuration hash", example = "abc123def456")
        private String lastAppliedHash;

        @Schema(description = "Expected configuration hash", example = "def456ghi789")
        private String expectedHash;

        @Schema(description = "Whether drift is detected", example = "true")
        private Boolean hasDrift;

        @Schema(description = "Instance status", example = "HEALTHY", allowableValues = {"HEALTHY", "UNHEALTHY", "DRIFT", "UNKNOWN"})
        private ServiceInstance.InstanceStatus status;

        @Schema(description = "Instance metadata", example = "{\"region\": \"us-east-1\", \"zone\": \"us-east-1a\", \"updated\": \"2024-01-15\"}")
        private Map<String, String> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ServiceInstanceQueryFilter", description = "Query filter for searching service instances")
    public static class QueryFilter {
        @Schema(description = "Filter by service ID", example = "payment-service")
        private String serviceId;

        @Schema(description = "Filter by instance ID", example = "payment-dev-1")
        private String instanceId;

        @Schema(description = "Filter by instance status", example = "HEALTHY", allowableValues = {"HEALTHY", "UNHEALTHY", "DRIFT", "UNKNOWN"})
        private ServiceInstance.InstanceStatus status;

        @Schema(description = "Filter by drift status", example = "true")
        private Boolean hasDrift;

        @Schema(description = "Filter by environment", example = "dev", allowableValues = {"dev", "staging", "prod"})
        private String environment;

        @Schema(description = "Filter by service version", example = "1.2.0")
        private String version;

        @Schema(description = "Filter instances seen after this timestamp", example = "2024-01-15T00:00:00Z")
        private Instant lastSeenAtFrom;

        @Schema(description = "Filter instances seen before this timestamp", example = "2024-01-15T23:59:59Z")
        private Instant lastSeenAtTo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ServiceInstanceResponse", description = "Service instance details response")
    public static class Response {
        @Schema(description = "ID of the service", example = "payment-service")
        private String serviceId;

        @Schema(description = "Unique instance identifier", example = "payment-dev-1")
        private String instanceId;

        @Schema(description = "Instance host address", example = "payment-dev-1.internal.company.com")
        private String host;

        @Schema(description = "Instance port number", example = "8080")
        private Integer port;

        @Schema(description = "Deployment environment", example = "dev", allowableValues = {"dev", "staging", "prod"})
        private String environment;

        @Schema(description = "Service version", example = "1.2.0")
        private String version;

        @Schema(description = "Current configuration hash", example = "abc123def456")
        private String configHash;

        @Schema(description = "Last applied configuration hash", example = "abc123def456")
        private String lastAppliedHash;

        @Schema(description = "Expected configuration hash", example = "abc123def456")
        private String expectedHash;

        @Schema(description = "Instance status", example = "HEALTHY", allowableValues = {"HEALTHY", "UNHEALTHY", "DRIFT", "UNKNOWN"})
        private ServiceInstance.InstanceStatus status;

        @Schema(description = "Last time instance was seen", example = "2024-01-15T14:30:45.123Z")
        private Instant lastSeenAt;

        @Schema(description = "Instance creation timestamp", example = "2024-01-15T10:30:45.123Z")
        private Instant createdAt;

        @Schema(description = "Instance last update timestamp", example = "2024-01-15T14:30:45.123Z")
        private Instant updatedAt;

        @Schema(description = "Instance metadata", example = "{\"region\": \"us-east-1\", \"zone\": \"us-east-1a\"}")
        private Map<String, String> metadata;

        @Schema(description = "Whether drift is detected", example = "false")
        private Boolean hasDrift;

        @Schema(description = "Timestamp when drift was first detected", example = "2024-01-15T14:30:45.123Z")
        private Instant driftDetectedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ServiceInstancePageResponse", description = "Page response for service instances")
    public static class ServiceInstancePageResponse {
        @Schema(description = "List of service instances in current page")
        private List<ServiceInstanceDtos.Response> items;

        @Schema(description = "Pagination metadata")
        private PageDtos.PageMetadata metadata;
    }
}


