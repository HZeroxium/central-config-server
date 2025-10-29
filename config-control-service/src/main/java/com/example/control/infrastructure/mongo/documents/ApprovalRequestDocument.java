package com.example.control.infrastructure.mongo.documents;

import com.example.control.domain.object.ApprovalRequest;
import com.example.control.domain.id.ApprovalRequestId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB document representation of {@link ApprovalRequest}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store multi-gate
 * approval workflow requests in the {@code approval_requests} collection with
 * optimistic locking support via {@code @Version}.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "approval_requests")
public class ApprovalRequestDocument {

    /**
     * Document identifier: UUID string.
     */
    @Id
    private String id;

    /**
     * User who created this request (Keycloak user ID).
     */
    @Indexed
    @Field("requesterUserId")
    private String requesterUserId;

    /**
     * Type of request being made (stored as string value).
     */
    @Field("requestType")
    private String requestType;

    /**
     * Target service ID for the request.
     */
    @Field("targetServiceId")
    private String targetServiceId;

    /**
     * Target team ID for assignment.
     */
    @Field("targetTeamId")
    private String targetTeamId;

    /**
     * Required approval gates as JSON (stored as string for flexibility).
     */
    @Field("requiredGatesJson")
    private String requiredGatesJson;

    /**
     * Current status of the request (stored as string value).
     */
    @Indexed
    @Field("status")
    private String status;

    /**
     * Requester snapshot as JSON (stored as string for flexibility).
     */
    @Field("requesterSnapshotJson")
    private String requesterSnapshotJson;

    /**
     * Current approval counts per gate as JSON.
     */
    @Field("approvalCountsJson")
    private String approvalCountsJson;

    /**
     * Timestamp when the request was created.
     */
    @Indexed
    @Field("createdAt")
    @CreatedDate
    private Instant createdAt;

    /**
     * Timestamp when the request was last updated.
     */
    @Field("updatedAt")
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Field("version")
    private Integer version;

    /**
     * User who created this request (Keycloak user ID).
     */
    @Field("createdBy")
    @CreatedBy
    private String createdBy;

    /**
     * User who last modified this request (Keycloak user ID).
     */
    @Field("updatedBy")
    @LastModifiedBy
    private String updatedBy;

    /**
     * Maps a {@link ApprovalRequest} domain object to a MongoDB document
     * representation.
     * <p>
     * For new entities (version=0 or null), the version field is set to null to
     * allow
     * Spring Data MongoDB to treat it as a fresh insert. This prevents
     * OptimisticLockingFailureException
     * when reusing domain objects that were previously saved and returned with
     * incremented version.
     * </p>
     *
     * @param domain domain model
     * @return new {@link ApprovalRequestDocument} populated from domain
     */
    public static ApprovalRequestDocument fromDomain(ApprovalRequest domain) {
        // For new entities (version 0 or null), set version to null to ensure fresh
        // insert
        // Otherwise, copy the version for optimistic locking on updates
        Integer documentVersion = (domain.getVersion() == null || domain.getVersion() == 0)
                ? null
                : domain.getVersion();

        ApprovalRequestDocumentBuilder builder = ApprovalRequestDocument.builder()
                .requesterUserId(domain.getRequesterUserId())
                .requestType(domain.getRequestType() != null ? domain.getRequestType().name() : null)
                .targetServiceId(domain.getTarget() != null ? domain.getTarget().getServiceId() : null)
                .targetTeamId(domain.getTarget() != null ? domain.getTarget().getTeamId() : null)
                .requiredGatesJson(serializeRequiredGates(domain.getRequired()))
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .requesterSnapshotJson(serializeRequesterSnapshot(domain.getSnapshot()))
                .approvalCountsJson(serializeApprovalCounts(domain.getCounts()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(documentVersion);

        // Set ID if it exists (for updates), otherwise generate UUID
        if (domain.getId() != null && domain.getId().id() != null) {
            builder.id(domain.getId().id());
        } else {
            builder.id(UUID.randomUUID().toString());
        }

        return builder.build();
    }

    /**
     * Serialize required gates to JSON string.
     * <p>
     * For simplicity, using a basic string format. In production, consider using
     * a proper JSON library like Jackson or Gson.
     */
    private static String serializeRequiredGates(List<ApprovalRequest.ApprovalGate> gates) {
        if (gates == null || gates.isEmpty()) {
            return "[]";
        }
        // Simple serialization - in production, use proper JSON library
        return gates.stream()
                .map(gate -> String.format("{\"gate\":\"%s\",\"minApprovals\":%d}",
                        gate.getGate(), gate.getMinApprovals()))
                .reduce((a, b) -> a + "," + b)
                .map(result -> "[" + result + "]")
                .orElse("[]");
    }

    /**
     * Deserialize required gates from JSON string.
     */
    private static List<ApprovalRequest.ApprovalGate> deserializeRequiredGates(String json) {
        // Simple deserialization - in production, use proper JSON library
        if (json == null || json.trim().equals("[]")) {
            return List.of();
        }
        // For now, return empty list - implement proper JSON parsing if needed
        return List.of();
    }

    /**
     * Serialize requester snapshot to JSON string.
     */
    private static String serializeRequesterSnapshot(ApprovalRequest.RequesterSnapshot snapshot) {
        if (snapshot == null) {
            return "{}";
        }
        // Simple serialization - in production, use proper JSON library
        return String.format("{\"teamIds\":%s,\"managerId\":\"%s\",\"roles\":%s}",
                snapshot.getTeamIds() != null ? snapshot.getTeamIds().toString() : "[]",
                snapshot.getManagerId() != null ? snapshot.getManagerId() : "",
                snapshot.getRoles() != null ? snapshot.getRoles().toString() : "[]");
    }

    /**
     * Deserialize requester snapshot from JSON string.
     */
    private static ApprovalRequest.RequesterSnapshot deserializeRequesterSnapshot(String json) {
        // Simple deserialization - in production, use proper JSON library
        if (json == null || json.trim().equals("{}")) {
            return ApprovalRequest.RequesterSnapshot.builder().build();
        }
        // For now, return empty snapshot - implement proper JSON parsing if needed
        return ApprovalRequest.RequesterSnapshot.builder().build();
    }

    /**
     * Serialize approval counts to JSON string.
     */
    private static String serializeApprovalCounts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return "{}";
        }
        // Simple serialization - in production, use proper JSON library
        return counts.entrySet().stream()
                .map(entry -> String.format("\"%s\":%d", entry.getKey(), entry.getValue()))
                .reduce((a, b) -> a + "," + b)
                .map(result -> "{" + result + "}")
                .orElse("{}");
    }

    /**
     * Deserialize approval counts from JSON string.
     */
    private static Map<String, Integer> deserializeApprovalCounts(String json) {
        // Simple deserialization - in production, use proper JSON library
        if (json == null || json.trim().equals("{}")) {
            return Map.of();
        }
        // For now, return empty map - implement proper JSON parsing if needed
        return Map.of();
    }

    /**
     * Converts this document back into its domain representation.
     *
     * @return new {@link ApprovalRequest} populated from document
     */
    public ApprovalRequest toDomain() {
        return ApprovalRequest.builder()
                .id(ApprovalRequestId.of(id != null ? id : null))
                .requesterUserId(requesterUserId)
                .requestType(requestType != null
                        ? ApprovalRequest.RequestType.valueOf(requestType)
                        : null)
                .target(ApprovalRequest.ApprovalTarget.builder()
                        .serviceId(targetServiceId)
                        .teamId(targetTeamId)
                        .build())
                .required(deserializeRequiredGates(requiredGatesJson))
                .status(status != null
                        ? ApprovalRequest.ApprovalStatus.valueOf(status)
                        : ApprovalRequest.ApprovalStatus.PENDING)
                .snapshot(deserializeRequesterSnapshot(requesterSnapshotJson))
                .counts(deserializeApprovalCounts(approvalCountsJson))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version)
                .build();
    }
}
