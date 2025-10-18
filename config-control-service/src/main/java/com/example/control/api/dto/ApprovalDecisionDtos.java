package com.example.control.api.dto;

import com.example.control.domain.ApprovalDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTOs for ApprovalDecision API operations.
 * <p>
 * These DTOs provide a clean separation between the domain model and the API layer,
 * allowing for versioning, validation, and documentation without affecting the domain.
 * </p>
 */
public class ApprovalDecisionDtos {

    /**
     * Request DTO for submitting an approval decision.
     */
    public record CreateRequest(
            @NotNull(message = "Decision is required")
            ApprovalDecision.Decision decision,
            
            @NotBlank(message = "Gate is required")
            String gate,
            
            @Size(max = 1000, message = "Note must not exceed 1000 characters")
            String note
    ) {}

    /**
     * Response DTO for approval decision data.
     */
    public record Response(
            String id,
            String requestId,
            String approverUserId,
            ApprovalDecision.Decision decision,
            String gate,
            String note,
            Instant createdAt
    ) {}

    /**
     * Request DTO for listing approval decisions with filters.
     */
    public record ListRequest(
            String requestId,
            String approverUserId,
            String gate,
            ApprovalDecision.Decision decision,
            Integer page,
            Integer size,
            String sort
    ) {}

    /**
     * Response DTO for paginated approval decision list.
     */
    public record ListResponse(
            java.util.List<Response> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {}
}
