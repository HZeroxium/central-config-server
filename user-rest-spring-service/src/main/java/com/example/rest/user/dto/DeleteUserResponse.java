package com.example.rest.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for deleting a user.
 */
@Data
@Builder
@Schema(name = "DeleteUserResponse", description = "Response after deleting a user")
public class DeleteUserResponse {

    @Schema(description = "Status code", example = "0", required = true)
    private int status; // 0 = success, 1 = not found, 2 = database error, etc.

    @Schema(description = "Response message", example = "User deleted successfully")
    private String message;

    @Schema(description = "Timestamp when user was deleted")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;

    @Schema(description = "Request correlation ID for tracing")
    private String correlationId;
}
