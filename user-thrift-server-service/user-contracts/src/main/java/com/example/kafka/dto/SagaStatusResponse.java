package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for saga status
 */
public record SagaStatusResponse(
    @JsonProperty("sagaId") String sagaId,
    @JsonProperty("state") String state,
    @JsonProperty("currentPhase") String currentPhase,
    @JsonProperty("startedAt") LocalDateTime startedAt,
    @JsonProperty("lastUpdatedAt") LocalDateTime lastUpdatedAt,
    @JsonProperty("completedAt") LocalDateTime completedAt,
    @JsonProperty("failedAt") LocalDateTime failedAt,
    @JsonProperty("errorMessage") String errorMessage,
    @JsonProperty("phaseHistory") List<PhaseHistory> phaseHistory,
    @JsonProperty("correlationId") String correlationId
) {
    
    public record PhaseHistory(
        @JsonProperty("phase") String phase,
        @JsonProperty("status") String status,
        @JsonProperty("startedAt") LocalDateTime startedAt,
        @JsonProperty("completedAt") LocalDateTime completedAt,
        @JsonProperty("errorMessage") String errorMessage
    ) {}
}
