package com.example.rest.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for listing users with pagination.
 */
@Data
@Builder
@Schema(name = "ListUsersResponse", description = "Response after listing users with pagination")
public class ListUsersResponse {
    
    @Schema(description = "Status code", example = "0", required = true)
    private int status; // 0 = success, 1 = validation error, 2 = database error, etc.
    
    @Schema(description = "Response message", example = "Users retrieved successfully")
    private String message;
    
    @Schema(description = "List of users")
    private List<UserResponse> items;
    
    @Schema(description = "Current page index", example = "0")
    private int page;
    
    @Schema(description = "Page size", example = "20")
    private int size;
    
    @Schema(description = "Total number of users", example = "100")
    private long total;
    
    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;
    
    @Schema(description = "Timestamp when users were retrieved")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request correlation ID for tracing")
    private String correlationId;
}
