package com.example.rest.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for user creation.
 */
@Data
@Builder
@Schema(name = "CreateUserResponse", description = "Response after creating a user")
public class CreateUserResponse {
    
    @Schema(description = "Status code", example = "0", required = true)
    private int status; // 0 = success, 1 = validation error, 2 = database error, etc.
    
    @Schema(description = "Response message", example = "User created successfully")
    private String message;
    
    @Schema(description = "Created user data")
    private UserResponse user;
    
    @Schema(description = "Timestamp when user was created")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request correlation ID for tracing")
    private String correlationId;
}
