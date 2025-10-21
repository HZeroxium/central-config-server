package com.example.control.api.dto.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTOs for ApprovalDecision API operations.
 * <p>
 * Provides response DTOs for approval decisions with
 * proper JSON serialization.
 * </p>
 */
public class ApprovalDecisionDtos {

    /**
     * Response DTO for approval decision details.
     */
    public record Response(
            @JsonProperty("id")
            String id,

            @JsonProperty("requestId")
            String requestId,

            @JsonProperty("approverUserId")
            String approverUserId,

            @JsonProperty("gate")
            String gate,

            @JsonProperty("decision")
            String decision,

            @JsonProperty("decidedAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant decidedAt,

            @JsonProperty("note")
            String note
    ) {}
}