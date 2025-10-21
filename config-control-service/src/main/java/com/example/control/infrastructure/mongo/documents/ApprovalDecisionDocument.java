package com.example.control.infrastructure.mongo.documents;

import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representation of {@link ApprovalDecision}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store individual
 * approval/rejection decisions in the {@code approval_decisions} collection
 * with compound unique index to enforce one decision per user per request per gate.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "approval_decisions")
@CompoundIndex(def = "{'requestId': 1, 'approverUserId': 1, 'gate': 1}", unique = true)
public class ApprovalDecisionDocument {

    /** Document identifier. */
    @Id
    private String id;

    /** ID of the approval request this decision belongs to. */
    @Indexed
    private String requestId;

    /** User who made this decision (Keycloak user ID). */
    @Indexed
    private String approverUserId;

    /** Gate this decision is for (e.g., SYS_ADMIN, LINE_MANAGER). */
    @Indexed
    private String gate;

    /** Decision made (stored as string value). */
    private String decision;

    /** Timestamp when the decision was made. */
    private Instant decidedAt;

    /** Optional note from the approver. */
    private String note;

    /**
     * Maps a {@link ApprovalDecision} domain object to a MongoDB document representation.
     *
     * @param domain domain model
     * @return new {@link ApprovalDecisionDocument} populated from domain
     */
    public static ApprovalDecisionDocument fromDomain(ApprovalDecision domain) {
        return ApprovalDecisionDocument.builder()
                .id(domain.getId().id())
                .requestId(domain.getRequestId().id())
                .approverUserId(domain.getApproverUserId())
                .gate(domain.getGate())
                .decision(domain.getDecision() != null ? domain.getDecision().name() : null)
                .decidedAt(domain.getDecidedAt())
                .note(domain.getNote())
                .build();
    }

    /**
     * Converts this document back into its domain representation.
     *
     * @return new {@link ApprovalDecision} populated from document
     */
    public ApprovalDecision toDomain() {
        return ApprovalDecision.builder()
                .id(ApprovalDecisionId.of(id))
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
