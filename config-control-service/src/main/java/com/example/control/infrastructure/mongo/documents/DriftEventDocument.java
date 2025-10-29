package com.example.control.infrastructure.mongo.documents;

import com.example.control.domain.object.DriftEvent;
import com.example.control.domain.id.DriftEventId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.UUID;

/**
 * MongoDB document representation of {@link DriftEvent}.
 * <p>
 * Each record corresponds to a configuration drift detection event and is
 * stored in
 * the {@code drift_events} collection with a TTL index for automatic cleanup
 * after 30 days.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drift_events")
@CompoundIndex(def = "{'serviceId': 1, 'teamId': 1, 'status': 1}")
@CompoundIndex(def = "{'teamId': 1, 'status': 1}")
@CompoundIndex(def = "{'serviceId': 1, 'detectedAt': -1}")
@CompoundIndex(def = "{'teamId': 1, 'severity': 1}")
public class DriftEventDocument {

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

    /**
     * Environment where the drift occurred (from ServiceInstance.environment).
     */
    @Indexed
    @Field("environment")
    private String environment;

    @Field("expectedHash")
    private String expectedHash;

    @Field("appliedHash")
    private String appliedHash;

    @Indexed
    @Field("severity")
    private String severity;

    @Indexed
    @Field("status")
    private String status;

    /**
     * Timestamp when drift was detected.
     * <p>
     * TTL index ensures automatic deletion after 30 days.
     */
    @Indexed(expireAfter = "30d")
    @Field("detectedAt")
    @CreatedDate
    private Instant detectedAt;

    @Field("resolvedAt")
    @LastModifiedDate
    private Instant resolvedAt;

    @Field("detectedBy")
    @CreatedBy
    private String detectedBy;

    @Field("resolvedBy")
    @LastModifiedBy
    private String resolvedBy;

    @Field("notes")
    private String notes;

    /**
     * Maps a {@link DriftEvent} domain object to its MongoDB document equivalent.
     *
     * @param domain the domain object to map
     * @return a new {@link DriftEventDocument}
     */
    public static DriftEventDocument fromDomain(DriftEvent domain) {
        DriftEventDocumentBuilder builder = DriftEventDocument.builder()
                .serviceName(domain.getServiceName())
                .instanceId(domain.getInstanceId())
                .serviceId(domain.getServiceId())
                .teamId(domain.getTeamId())
                .environment(domain.getEnvironment())
                .expectedHash(domain.getExpectedHash())
                .appliedHash(domain.getAppliedHash())
                .severity(domain.getSeverity() != null ? domain.getSeverity().name() : null)
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .detectedAt(domain.getDetectedAt())
                .resolvedAt(domain.getResolvedAt())
                .detectedBy(domain.getDetectedBy())
                .resolvedBy(domain.getResolvedBy())
                .notes(domain.getNotes());

        // Set ID if it exists (for updates), otherwise generate UUID
        if (domain.getId() != null && domain.getId().id() != null) {
            builder.id(domain.getId().id());
        } else {
            // Generate UUID for new drift events
            builder.id(UUID.randomUUID().toString());
        }

        return builder.build();
    }

    /**
     * Converts this document to a {@link DriftEvent} domain model.
     *
     * @return domain representation of the drift event
     */
    public DriftEvent toDomain() {
        return DriftEvent.builder()
                .id(DriftEventId.of(id != null ? id : null))
                .serviceName(serviceName)
                .instanceId(instanceId)
                .serviceId(serviceId)
                .teamId(teamId)
                .environment(environment)
                .expectedHash(expectedHash)
                .appliedHash(appliedHash)
                .severity(severity != null ? DriftEvent.DriftSeverity.valueOf(severity) : null)
                .status(status != null ? DriftEvent.DriftStatus.valueOf(status) : null)
                .detectedAt(detectedAt)
                .resolvedAt(resolvedAt)
                .detectedBy(detectedBy)
                .resolvedBy(resolvedBy)
                .notes(notes)
                .build();
    }
}
