package com.example.kafka.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TriggerPhaseResponse", description = "Response for triggering a specific phase of a saga")
public record TriggerPhaseResponse(
    @Schema(description = "Saga identifier", example = "7b7a3b9a-3a4e-4e20-bb4b-2a6e3b9f9a31")
    String sagaId,
    @Schema(description = "Phase number", example = "2")
    int phase,
    @Schema(description = "Operation status", example = "TRIGGERED")
    String status
) {}
