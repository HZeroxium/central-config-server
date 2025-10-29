package com.example.control.api.dto.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import com.example.control.api.dto.common.PageDtos;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for ApprovalDecision API operations.
 * <p>
 * Provides response DTOs for approval decisions with
 * proper JSON serialization.
 * </p>
 */
@Schema(name = "ApprovalDecisionDtos", description = "DTOs for ApprovalDecision API operations")
public class ApprovalDecisionDtos {

    /**
     * Response DTO for approval decision details.
     */
    @Schema(name = "ApprovalDecisionResponse", description = "Approval decision details response")
    public record Response(
            @JsonProperty("id") @Schema(description = "Unique decision identifier", example = "decision-12345") String id,

            @JsonProperty("requestId") @Schema(description = "Associated approval request ID", example = "request-67890") String requestId,

            @JsonProperty("approverUserId") @Schema(description = "User who made the decision", example = "admin") String approverUserId,

            @JsonProperty("gate") @Schema(description = "Approval gate", example = "SYS_ADMIN", allowableValues = {
                    "SYS_ADMIN", "LINE_MANAGER" }) String gate,

            @JsonProperty("decision") @Schema(description = "Decision made", example = "APPROVE", allowableValues = {
                    "APPROVE", "REJECT" }) String decision,

            @JsonProperty("decidedAt") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC") @Schema(description = "When the decision was made", example = "2024-01-15T14:30:45.123Z") Instant decidedAt,

            @JsonProperty("note") @Schema(description = "Decision notes", example = "Approved based on team capacity and expertise") String note){
    }

    /**
     * Query filter DTO for searching approval decisions.
     */
    @Schema(name = "ApprovalDecisionQueryFilter", description = "Query filter for searching approval decisions")
    public record QueryFilter(
            @Size(max = 100, message = "Request ID must not exceed 100 characters") @Schema(description = "Filter by approval request ID", example = "request-12345") String requestId,

            @Size(max = 100, message = "Approver user ID must not exceed 100 characters") @Schema(description = "Filter by approver user ID", example = "admin") String approverUserId,

            @Size(max = 50, message = "Gate must not exceed 50 characters") @Schema(description = "Filter by approval gate", example = "SYS_ADMIN", allowableValues = {
                    "SYS_ADMIN", "LINE_MANAGER" }) String gate,

            @Size(max = 20, message = "Decision must not exceed 20 characters") @Schema(description = "Filter by decision type", example = "APPROVE", allowableValues = {
                    "APPROVE", "REJECT" }) String decision){
    }

    /**
     * Page response DTO for approval decisions.
     */
    @Schema(name = "ApprovalDecisionPageResponse", description = "Page response for approval decisions")
    public record PageResponse(
            @JsonProperty("items") @Schema(description = "List of approval decisions in current page") List<Response> items,

            @JsonProperty("metadata") @Schema(description = "Pagination metadata") PageDtos.PageMetadata metadata) {
    }
}