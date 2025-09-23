package com.example.kafka.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;

@Schema(name = "AssignmentsResponse", description = "Response containing partition assignments and status of a listener")
public record AssignmentsResponse(
    @Schema(description = "Listener ID", example = "P1")
    String listenerId,
    @ArraySchema(arraySchema = @Schema(description = "Assigned partitions"))
    Collection<?> assignments,
    @Schema(description = "Whether the listener is running")
    boolean running,
    @Schema(description = "Whether the listener is paused")
    boolean paused
) {}
