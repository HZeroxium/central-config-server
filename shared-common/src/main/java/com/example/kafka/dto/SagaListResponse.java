package com.example.kafka.dto;

import com.example.kafka.model.SagaState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "SagaListResponse", description = "Sagas map keyed by sagaId")
public record SagaListResponse(
    @Schema(description = "Map of sagaId to SagaState")
    Map<String, SagaState> sagas
) {}
