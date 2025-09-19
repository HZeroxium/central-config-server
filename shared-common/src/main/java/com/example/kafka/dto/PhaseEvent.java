package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for phase events
 */
public record PhaseEvent(
    @JsonProperty("sagaId") @NotBlank String sagaId,
    @JsonProperty("phase") @NotBlank String phase,
    @JsonProperty("eventType") @NotBlank String eventType,
    @JsonProperty("payload") @NotNull Object payload,
    @JsonProperty("correlationId") String correlationId,
    @JsonProperty("causationId") String causationId,
    @JsonProperty("eventId") String eventId,
    @JsonProperty("source") @NotBlank String source,
    @JsonProperty("timestamp") Long timestamp,
    @JsonProperty("errorMessage") String errorMessage
) {
    public PhaseEvent {
        if (sagaId == null || sagaId.trim().isEmpty()) {
            throw new IllegalArgumentException("sagaId cannot be null or empty");
        }
        if (phase == null || phase.trim().isEmpty()) {
            throw new IllegalArgumentException("phase cannot be null or empty");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("eventType cannot be null or empty");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload cannot be null");
        }
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("source cannot be null or empty");
        }
    }
}
