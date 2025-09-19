package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for phase commands
 */
public record PhaseCommand(
    @JsonProperty("sagaId") @NotBlank String sagaId,
    @JsonProperty("phase") @NotBlank String phase,
    @JsonProperty("commandType") @NotBlank String commandType,
    @JsonProperty("payload") @NotNull Object payload,
    @JsonProperty("correlationId") String correlationId,
    @JsonProperty("causationId") String causationId,
    @JsonProperty("eventId") String eventId,
    @JsonProperty("source") @NotBlank String source,
    @JsonProperty("timestamp") Long timestamp
) {
    public PhaseCommand {
        if (sagaId == null || sagaId.trim().isEmpty()) {
            throw new IllegalArgumentException("sagaId cannot be null or empty");
        }
        if (phase == null || phase.trim().isEmpty()) {
            throw new IllegalArgumentException("phase cannot be null or empty");
        }
        if (commandType == null || commandType.trim().isEmpty()) {
            throw new IllegalArgumentException("commandType cannot be null or empty");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload cannot be null");
        }
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("source cannot be null or empty");
        }
    }
}
