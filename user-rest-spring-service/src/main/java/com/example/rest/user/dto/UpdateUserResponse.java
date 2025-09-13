package com.example.rest.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for updating a user.
 */
@Data
@Builder
@Schema(name = "UpdateUserResponse", description = "Response after updating a user")
public class UpdateUserResponse {
    
    @Schema(description = "Status code", example = "0", required = true)
    private int status; // 0 = success, 1 = not found, 2 = validation error, 3 = database error, etc.
    
    @Schema(description = "Response message", example = "User updated successfully")
    private String message;
    
    @Schema(description = "Updated user data")
    private UserResponse user;
    
    @Schema(description = "Timestamp when user was updated")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request correlation ID for tracing")
    private String correlationId;
}
