package com.example.control.infrastructure.adapter.persistence.mongo.documents;

import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.UUID;

/**
 * MongoDB document representation of {@link FailedHeartbeat}.
 * <p>
 * Each record corresponds to a heartbeat that failed processing and was routed
 * to DLQ. Stored in the {@code failed_heartbeats} collection with TTL index
 * for automatic cleanup after 7 days (as per retention policy).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "failed_heartbeats")
@CompoundIndex(def = "{'serviceName': 1, 'instanceId': 1}")
@CompoundIndex(def = "{'teamId': 1, 'status': 1}")
@CompoundIndex(def = "{'serviceId': 1, 'status': 1}")
@CompoundIndex(def = "{'teamId': 1, 'firstSeenAt': -1}")
@CompoundIndex(def = "{'status': 1, 'firstSeenAt': -1}")
public class FailedHeartbeatDocument {

    @Id
    private String id;

    @Indexed
    @Field("serviceName")
    private String serviceName;

    @Indexed
    @Field("instanceId")
    private String instanceId;

    /**
     * Service ID from ApplicationService (for team-based access control).
     */
    @Indexed
    @Field("serviceId")
    private String serviceId;

    /**
     * Team ID that owns this service (from ApplicationService.ownerTeamId).
     */
    @Indexed
    @Field("teamId")
    private String teamId;

    @Field("environment")
    private String environment;

    /**
     * Original heartbeat payload (stored as nested document).
     */
    @Field("payload")
    private HeartbeatPayload payload;

    @Field("originalTopic")
    private String originalTopic;

    @Field("originalPartition")
    private Integer originalPartition;

    @Field("originalOffset")
    private Long originalOffset;

    @Field("exceptionMessage")
    private String exceptionMessage;

    @Field("exceptionClass")
    private String exceptionClass;

    @Field("retryCount")
    private Integer retryCount;

    @Indexed
    @Field("status")
    private String status;

    /**
     * Timestamp when the heartbeat first failed and was routed to DLQ.
     * <p>
     * TTL index ensures automatic deletion after 7 days (as per retention policy).
     */
    @Indexed(expireAfter = "7d")
    @Field("firstSeenAt")
    @CreatedDate
    private Instant firstSeenAt;

    @Field("lastSeenAt")
    private Instant lastSeenAt;

    @Field("resolvedAt")
    @LastModifiedDate
    private Instant resolvedAt;

    @Field("resolvedBy")
    private String resolvedBy;

    @Field("notes")
    private String notes;

    /**
     * Maps a {@link FailedHeartbeat} domain object to its MongoDB document equivalent.
     *
     * @param domain the domain object to map
     * @return a new {@link FailedHeartbeatDocument}
     */
    public static FailedHeartbeatDocument fromDomain(FailedHeartbeat domain) {
        FailedHeartbeatDocumentBuilder builder = FailedHeartbeatDocument.builder()
                .serviceName(domain.getServiceName())
                .instanceId(domain.getInstanceId())
                .serviceId(domain.getServiceId())
                .teamId(domain.getTeamId())
                .environment(domain.getEnvironment())
                .payload(domain.getPayload())
                .originalTopic(domain.getOriginalTopic())
                .originalPartition(domain.getOriginalPartition())
                .originalOffset(domain.getOriginalOffset())
                .exceptionMessage(domain.getExceptionMessage())
                .exceptionClass(domain.getExceptionClass())
                .retryCount(domain.getRetryCount())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .firstSeenAt(domain.getFirstSeenAt())
                .lastSeenAt(domain.getLastSeenAt())
                .resolvedAt(domain.getResolvedAt())
                .resolvedBy(domain.getResolvedBy())
                .notes(domain.getNotes());

        // Set ID if it exists (for updates), otherwise generate UUID
        if (domain.getId() != null && domain.getId().id() != null) {
            builder.id(domain.getId().id());
        } else {
            // Generate UUID for new failed heartbeats
            builder.id(UUID.randomUUID().toString());
        }

        return builder.build();
    }

    /**
     * Converts this document to a {@link FailedHeartbeat} domain model.
     *
     * @return domain representation of the failed heartbeat
     */
    public FailedHeartbeat toDomain() {
        return FailedHeartbeat.builder()
                .id(FailedHeartbeatId.of(id != null ? id : null))
                .serviceName(serviceName)
                .instanceId(instanceId)
                .serviceId(serviceId)
                .teamId(teamId)
                .environment(environment)
                .payload(payload)
                .originalTopic(originalTopic)
                .originalPartition(originalPartition)
                .originalOffset(originalOffset)
                .exceptionMessage(exceptionMessage)
                .exceptionClass(exceptionClass)
                .retryCount(retryCount)
                .status(status != null ? FailedHeartbeat.FailedHeartbeatStatus.valueOf(status) : null)
                .firstSeenAt(firstSeenAt)
                .lastSeenAt(lastSeenAt)
                .resolvedAt(resolvedAt)
                .resolvedBy(resolvedBy)
                .notes(notes)
                .build();
    }
}

