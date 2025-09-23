package com.example.kafka.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SimpleStatusResponse", description = "Simple status response with message")
public record SimpleStatusResponse(
    @Schema(description = "Status", example = "ok")
    String status,
    @Schema(description = "Additional message", example = "All settings reset to defaults")
    String message
) {}
