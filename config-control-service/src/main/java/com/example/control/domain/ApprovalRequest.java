package com.example.control.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing a multi-gate approval workflow request.
 * <p>
 * This entity supports configurable approval gates (e.g., SYS_ADMIN, LINE_MANAGER)
 * with different minimum approval requirements. Uses optimistic locking to prevent
 * race conditions during concurrent approvals.
 * </p>
 * 
 * @see ApprovalDecision for individual approval decisions
 * @see ApprovalGate for gate configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    /** Unique request identifier. */
    @NotBlank(message = "Request ID is required")
    private String id;

    /** User who created this request (Keycloak user ID). */
    @NotBlank(message = "Requester user ID is required")
    private String requesterUserId;

    /** Type of request being made. */
    @NotNull(message = "Request type is required")
    private RequestType requestType;

    /** Target information for the request. */
    @NotNull(message = "Target is required")
    @Valid
    private ApprovalTarget target;

    /** Required approval gates with minimum approvals. */
    @NotNull(message = "Required gates cannot be null")
    @Size(min = 1, message = "At least one approval gate must be specified")
    private List<ApprovalGate> required;

    /** Current status of the request. */
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /** Snapshot of requester information at request time. */
    @NotNull(message = "Requester snapshot is required")
    @Valid
    private RequesterSnapshot snapshot;

    /** Current approval counts per gate. */
    @NotNull(message = "Approval counts cannot be null")
    private Map<String, Integer> counts;

    /** Timestamp when the request was created. */
    private Instant createdAt;

    /** Timestamp when the request was last updated. */
    private Instant updatedAt;

    /** Version for optimistic locking. */
    @Builder.Default
    private Integer version = 0;

    /**
     * Request type enumeration.
     */
    public enum RequestType {
        /** Request to assign a service to a team. */
        ASSIGN_SERVICE_TO_TEAM
    }

    /**
     * Approval status enumeration.
     */
    public enum ApprovalStatus {
        /** Request is pending approval. */
        PENDING,
        
        /** Request has been approved. */
        APPROVED,
        
        /** Request has been rejected. */
        REJECTED,
        
        /** Request has been cancelled. */
        CANCELLED
    }

    /**
     * Approval gate configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalGate {
        /** Gate name (e.g., SYS_ADMIN, LINE_MANAGER). */
        @NotBlank(message = "Gate name is required")
        private String gate;
        
        /** Minimum number of approvals required from this gate. */
        @NotNull(message = "Minimum approvals is required")
        private Integer minApprovals;
    }

    /**
     * Target information for the approval request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalTarget {
        /** Service ID being requested. */
        @NotBlank(message = "Service ID is required")
        private String serviceId;
        
        /** Target team ID for assignment. */
        @NotBlank(message = "Target team ID is required")
        private String teamId;
    }

    /**
     * Snapshot of requester information at request time.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequesterSnapshot {
        /** Team IDs the requester belongs to. */
        private List<String> teamIds;
        
        /** Manager ID of the requester. */
        private String managerId;
        
        /** Roles of the requester. */
        private List<String> roles;
    }
}
