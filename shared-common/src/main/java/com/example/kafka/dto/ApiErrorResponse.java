package com.example.kafka.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse", description = "Standardized error response for APIs")
public record ApiErrorResponse(
    @Schema(description = "Human-readable error message", example = "Failed to start saga: validation error")
    String error
) {}
