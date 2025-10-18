package com.example.control.api.dto;

import com.example.control.domain.ApprovalRequest;
import com.example.control.domain.ApprovalDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for ApprovalRequest API operations.
 * <p>
 * These DTOs provide a clean separation between the domain model and the API layer,
 * allowing for versioning, validation, and documentation without affecting the domain.
 * </p>
 */
public class ApprovalRequestDtos {

    /**
     * Request DTO for creating a new approval request.
     */
    public record CreateRequest(
            @NotBlank(message = "Service ID is required")
            String serviceId,
            
            @NotBlank(message = "Target team ID is required")
            String targetTeamId,
            
            String note
    ) {}

    /**
     * Request DTO for submitting an approval decision.
     */
    public record DecisionRequest(
            @NotNull(message = "Decision is required")
            ApprovalDecision.Decision decision,
            
            @NotBlank(message = "Gate is required")
            String gate,
            
            @Size(max = 1000, message = "Note must not exceed 1000 characters")
            String note
    ) {}

    /**
     * Request DTO for canceling an approval request.
     */
    public record CancelRequest(
            @Size(max = 1000, message = "Reason must not exceed 1000 characters")
            String reason
    ) {}

    /**
     * Response DTO for approval request data.
     */
    public record Response(
            String id,
            ApprovalRequest.RequestType requestType,
            String requesterUserId,
            ApprovalRequest.ApprovalTarget target,
            ApprovalRequest.ApprovalStatus status,
            List<ApprovalRequest.ApprovalGate> gates,
            String note,
            String cancelReason,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}

    /**
     * Response DTO for approval decision data.
     */
    public record DecisionResponse(
            String id,
            String requestId,
            String approverUserId,
            ApprovalDecision.Decision decision,
            String gate,
            String note,
            Instant createdAt
    ) {}

    /**
     * Request DTO for listing approval requests with filters.
     */
    public record ListRequest(
            String requesterUserId,
            ApprovalRequest.ApprovalStatus status,
            ApprovalRequest.RequestType requestType,
            Instant fromDate,
            Instant toDate,
            String gate,
            Integer page,
            Integer size,
            String sort
    ) {}

    /**
     * Response DTO for paginated approval request list.
     */
    public record ListResponse(
            List<Response> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {}

    /**
     * Response DTO for approval request statistics.
     */
    public record StatsResponse(
            long pendingCount,
            long approvedCount,
            long rejectedCount,
            long cancelledCount
    ) {}
}
