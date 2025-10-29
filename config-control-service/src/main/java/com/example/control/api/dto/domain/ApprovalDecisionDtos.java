package com.example.control.api.dto.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

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
            @JsonProperty("id")
            @Schema(description = "Unique decision identifier", example = "decision-12345")
            String id,

            @JsonProperty("requestId")
            @Schema(description = "Associated approval request ID", example = "request-67890")
            String requestId,

            @JsonProperty("approverUserId")
            @Schema(description = "User who made the decision", example = "admin")
            String approverUserId,

            @JsonProperty("gate")
            @Schema(description = "Approval gate", example = "SYS_ADMIN", allowableValues = {"SYS_ADMIN", "LINE_MANAGER"})
            String gate,

            @JsonProperty("decision")
            @Schema(description = "Decision made", example = "APPROVE", allowableValues = {"APPROVE", "REJECT"})
            String decision,

            @JsonProperty("decidedAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            @Schema(description = "When the decision was made", example = "2024-01-15T14:30:45.123Z")
            Instant decidedAt,

            @JsonProperty("note")
            @Schema(description = "Decision notes", example = "Approved based on team capacity and expertise")
            String note
    ) {
    }
}