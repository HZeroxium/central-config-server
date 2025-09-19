package com.example.kafka.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ListenerActionResponse", description = "Response for listener pause/resume actions")
public record ListenerActionResponse(
    @Schema(description = "Listener ID", example = "P1")
    String listenerId,
    @Schema(description = "Action status", example = "paused")
    String status
) {}
