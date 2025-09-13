package com.example.rest.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for getting a user.
 */
@Data
@Builder
@Schema(name = "GetUserResponse", description = "Response after getting a user")
public class GetUserResponse {
    
    @Schema(description = "Status code", example = "0", required = true)
    private int status; // 0 = success, 1 = not found, 2 = database error, etc.
    
    @Schema(description = "Response message", example = "User retrieved successfully")
    private String message;
    
    @Schema(description = "User data")
    private UserResponse user;
    
    @Schema(description = "Timestamp when user was retrieved")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request correlation ID for tracing")
    private String correlationId;
}
