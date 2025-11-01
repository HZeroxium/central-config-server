package com.example.control.infrastructure.adapter.persistence.mongo.documents;

import com.example.control.domain.model.ApprovalDecision;
import com.example.control.domain.valueobject.id.ApprovalDecisionId;
import com.example.control.domain.valueobject.id.ApprovalRequestId;
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

/**
 * MongoDB document representation of {@link ApprovalDecision}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store individual
 * approval/rejection decisions in the {@code approval_decisions} collection
 * with compound unique index to enforce one decision per user per request per
 * gate.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "approval_decisions")
@CompoundIndex(def = "{'requestId': 1, 'approverUserId': 1, 'gate': 1}", unique = true)
public class ApprovalDecisionDocument {

    /**
     * Document identifier: UUID string.
     */
    @Id
    private String id;

    /**
     * ID of the approval request this decision belongs to.
     */
    @Indexed
    @Field("requestId")
    private String requestId;

    /**
     * User who made this decision (Keycloak user ID).
     */
    @Indexed
    @Field("approverUserId")
    private String approverUserId;

    /**
     * Gate this decision is for (e.g., SYS_ADMIN, LINE_MANAGER).
     */
    @Indexed
    @Field("gate")
    private String gate;

    /**
     * Decision made (stored as string value).
     */
    @Field("decision")
    private String decision;

    /**
     * Timestamp when the decision was made.
     */
    @Field("decidedAt")
    @CreatedDate
    private Instant decidedAt;

    /**
     * Optional note from the approver.
     */
    @Field("note")
    private String note;

    /**
     * User who created this decision (Keycloak user ID).
     */
    @Field("createdBy")
    @CreatedBy
    private String createdBy;

    /**
     * User who last modified this decision (Keycloak user ID).
     */
    @Field("updatedBy")
    @LastModifiedBy
    private String updatedBy;

    /**
     * Timestamp when the decision was last updated.
     */
    @Field("updatedAt")
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Maps a {@link ApprovalDecision} domain object to a MongoDB document
     * representation.
     *
     * @param domain domain model
     * @return new {@link ApprovalDecisionDocument} populated from domain
     */
    public static ApprovalDecisionDocument fromDomain(ApprovalDecision domain) {
        ApprovalDecisionDocumentBuilder builder = ApprovalDecisionDocument.builder()
                .requestId(domain.getRequestId().id())
                .approverUserId(domain.getApproverUserId())
                .gate(domain.getGate())
                .decision(domain.getDecision() != null ? domain.getDecision().name() : null)
                .decidedAt(domain.getDecidedAt())
                .note(domain.getNote());

        // Set ID if it exists (for updates), otherwise use the provided ID
        if (domain.getId() != null && domain.getId().id() != null) {
            builder.id(domain.getId().id());
        }

        return builder.build();
    }

    /**
     * Converts this document back into its domain representation.
     *
     * @return new {@link ApprovalDecision} populated from document
     */
    public ApprovalDecision toDomain() {
        return ApprovalDecision.builder()
                .id(ApprovalDecisionId.of(id != null ? id : null))
                .requestId(ApprovalRequestId.of(requestId))
                .approverUserId(approverUserId)
                .gate(gate)
                .decision(decision != null
                        ? ApprovalDecision.Decision.valueOf(decision)
                        : null)
                .decidedAt(decidedAt)
                .note(note)
                .build();
    }
}
