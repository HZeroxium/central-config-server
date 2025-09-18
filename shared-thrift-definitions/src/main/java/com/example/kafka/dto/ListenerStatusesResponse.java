package com.example.kafka.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ListenerStatusesResponse", description = "Aggregated listeners status and current settings")
public record ListenerStatusesResponse(
    @Schema(description = "Per-listener status map (P1..P4)")
    java.util.Map<String, java.util.Map<String, Object>> listeners,
    @Schema(description = "Current worker settings snapshot")
    WorkerSettingsResponse settings
) {}
