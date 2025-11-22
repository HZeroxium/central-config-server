package com.example.control.domain.model;

import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing a failed heartbeat that was routed to DLQ.
 * <p>
 * FailedHeartbeat documents are created when heartbeat batch processing fails
 * after exhausting all retry attempts. They contain metadata about the original
 * heartbeat, the failure reason, and allow for triage and manual re-drive.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedHeartbeat {

    /**
     * Unique identifier of the failed heartbeat.
     */
    private FailedHeartbeatId id;

    /**
     * Name of the service that sent the heartbeat.
     */
    private String serviceName;

    /**
     * Instance identifier within the service.
     */
    private String instanceId;

    /**
     * Service ID from ApplicationService (for team-based access control).
     * Nullable if service doesn't exist yet.
     */
    private String serviceId;

    /**
     * Team ID that owns this service (from ApplicationService.ownerTeamId).
     * Nullable if service is orphaned.
     */
    private String teamId;

    /**
     * Environment where the heartbeat originated.
     */
    private String environment;

    /**
     * Original heartbeat payload that failed processing.
     */
    private HeartbeatPayload payload;

    /**
     * Original Kafka topic name.
     */
    private String originalTopic;

    /**
     * Original Kafka partition number.
     */
    private Integer originalPartition;

    /**
     * Original Kafka offset.
     */
    private Long originalOffset;

    /**
     * Exception message that caused the failure.
     */
    private String exceptionMessage;

    /**
     * Exception class name that caused the failure.
     */
    private String exceptionClass;

    /**
     * Number of retry attempts before routing to DLQ.
     */
    private Integer retryCount;

    /**
     * Current status of the failed heartbeat.
     */
    private FailedHeartbeatStatus status;

    /**
     * Timestamp when the heartbeat first failed and was routed to DLQ.
     */
    private Instant firstSeenAt;

    /**
     * Timestamp when the failed heartbeat was last seen (for deduplication).
     */
    private Instant lastSeenAt;

    /**
     * Timestamp when the failed heartbeat was resolved.
     * Nullable until status is RESOLVED or IGNORED.
     */
    private Instant resolvedAt;

    /**
     * User identifier who resolved the failed heartbeat.
     * Nullable until status is RESOLVED or IGNORED.
     */
    private String resolvedBy;

    /**
     * Additional notes or investigation summary.
     */
    private String notes;

    /**
     * Status lifecycle for failed heartbeats.
     * <p>
     * NEW: Just routed to DLQ, needs investigation
     * INVESTIGATING: Being investigated by team
     * RESOLVED: Issue resolved, can be ignored
     * IGNORED: Known issue, no action needed
     */
    public enum FailedHeartbeatStatus {
        NEW,
        INVESTIGATING,
        RESOLVED,
        IGNORED
    }
}

