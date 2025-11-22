package com.example.control.api.http.dto.infra;

import com.example.control.api.http.dto.common.PageDtos;
import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.model.HeartbeatPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for FailedHeartbeat API operations.
 * <p>
 * Provides request/response DTOs for managing failed heartbeats from DLQ
 * with status tracking and re-drive capabilities.
 * </p>
 */
@Data
@Schema(name = "FailedHeartbeatDtos", description = "DTOs for FailedHeartbeat API operations")
public final class FailedHeartbeatDtos {

    private FailedHeartbeatDtos() {
        throw new UnsupportedOperationException("Utility class");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FailedHeartbeatQueryFilter", description = "Query filter for searching failed heartbeats")
    public static class QueryFilter {
        @Schema(description = "Filter by service name", example = "payment-service")
        private String serviceName;

        @Schema(description = "Filter by instance ID", example = "payment-dev-1")
        private String instanceId;

        @Schema(description = "Filter by status", example = "NEW", allowableValues = {"NEW", "INVESTIGATING", "RESOLVED", "IGNORED"})
        private FailedHeartbeat.FailedHeartbeatStatus status;

        @Schema(description = "Filter by team ID", example = "team-123")
        private String teamId;

        @Schema(description = "Filter failed heartbeats first seen after this timestamp", example = "2024-01-15T00:00:00Z")
        private Instant firstSeenAtFrom;

        @Schema(description = "Filter failed heartbeats first seen before this timestamp", example = "2024-01-15T23:59:59Z")
        private Instant firstSeenAtTo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FailedHeartbeatResponse", description = "Failed heartbeat details response")
    public static class Response {
        @Schema(description = "Unique failed heartbeat identifier", example = "failed-heartbeat-12345")
        private String id;

        @Schema(description = "Name of the service", example = "payment-service")
        private String serviceName;

        @Schema(description = "Instance identifier", example = "payment-dev-1")
        private String instanceId;

        @Schema(description = "Service ID from ApplicationService", example = "service-123")
        private String serviceId;

        @Schema(description = "Team ID that owns this service", example = "team-123")
        private String teamId;

        @Schema(description = "Environment where the heartbeat originated", example = "development")
        private String environment;

        @Schema(description = "Original heartbeat payload")
        private HeartbeatPayload payload;

        @Schema(description = "Original Kafka topic name", example = "heartbeat-queue")
        private String originalTopic;

        @Schema(description = "Original Kafka partition number", example = "0")
        private Integer originalPartition;

        @Schema(description = "Original Kafka offset", example = "12345")
        private Long originalOffset;

        @Schema(description = "Exception message that caused the failure", example = "MongoDB connection timeout")
        private String exceptionMessage;

        @Schema(description = "Exception class name", example = "com.mongodb.MongoTimeoutException")
        private String exceptionClass;

        @Schema(description = "Number of retry attempts before routing to DLQ", example = "3")
        private Integer retryCount;

        @Schema(description = "Current status", example = "NEW", allowableValues = {"NEW", "INVESTIGATING", "RESOLVED", "IGNORED"})
        private FailedHeartbeat.FailedHeartbeatStatus status;

        @Schema(description = "When the heartbeat first failed and was routed to DLQ", example = "2024-01-15T14:30:45.123Z")
        private Instant firstSeenAt;

        @Schema(description = "When the failed heartbeat was last seen", example = "2024-01-15T14:30:45.123Z")
        private Instant lastSeenAt;

        @Schema(description = "When the failed heartbeat was resolved", example = "2024-01-15T15:45:30.456Z")
        private Instant resolvedAt;

        @Schema(description = "User identifier who resolved the failed heartbeat", example = "user1")
        private String resolvedBy;

        @Schema(description = "Additional notes or investigation summary", example = "Issue resolved after MongoDB restart")
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FailedHeartbeatPageResponse", description = "Page response for failed heartbeats")
    public static class FailedHeartbeatPageResponse {
        @Schema(description = "List of failed heartbeats in current page")
        private List<FailedHeartbeatDtos.Response> items;

        @Schema(description = "Pagination metadata")
        private PageDtos.PageMetadata metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UpdateStatusRequest", description = "Request to update failed heartbeat status")
    public static class UpdateStatusRequest {
        @NotNull
        @Schema(description = "New status", example = "RESOLVED", allowableValues = {"NEW", "INVESTIGATING", "RESOLVED", "IGNORED"})
        private FailedHeartbeat.FailedHeartbeatStatus status;

        @Schema(description = "Optional notes", example = "Issue resolved after MongoDB restart")
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RedriveRequest", description = "Request to re-drive failed heartbeat to main topic")
    public static class RedriveRequest {
        @Schema(description = "Optional force flag (ignored for now)", example = "false")
        private Boolean force;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BulkUpdateStatusRequest", description = "Request to bulk update failed heartbeat status")
    public static class BulkUpdateStatusRequest {
        @NotNull
        @Schema(description = "List of failed heartbeat IDs to update")
        private List<String> ids;

        @NotNull
        @Schema(description = "New status", example = "RESOLVED", allowableValues = {"NEW", "INVESTIGATING", "RESOLVED", "IGNORED"})
        private FailedHeartbeat.FailedHeartbeatStatus status;
    }
}

