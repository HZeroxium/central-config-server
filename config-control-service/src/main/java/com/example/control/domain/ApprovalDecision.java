package com.example.control.domain;

import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing an individual approval or rejection decision.
 * <p>
 * Each decision is tied to a specific approval request and gate. A user can
 * only make one decision per request per gate (enforced by unique constraint).
 * </p>
 * 
 * @see ApprovalRequest for the associated request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecision {

    /** Unique decision identifier. */
    @NotNull(message = "Decision ID is required")
    private ApprovalDecisionId id;

    /** ID of the approval request this decision belongs to. */
    @NotNull(message = "Request ID is required")
    private ApprovalRequestId requestId;

    /** User who made this decision (Keycloak user ID). */
    @NotBlank(message = "Approver user ID is required")
    private String approverUserId;

    /** Gate this decision is for (e.g., SYS_ADMIN, LINE_MANAGER). */
    @NotBlank(message = "Gate is required")
    private String gate;

    /** Decision made (APPROVE or REJECT). */
    @NotNull(message = "Decision is required")
    private Decision decision;

    /** Timestamp when the decision was made. */
    private Instant decidedAt;

    /** Optional note from the approver. */
    private String note;

    /** Alias for decidedAt for backward compatibility. */
    public Instant getCreatedAt() {
        return decidedAt;
    }

    /**
     * Decision enumeration.
     */
    public enum Decision {
        /** Approve the request. */
        APPROVE,
        
        /** Reject the request. */
        REJECT
    }
}
