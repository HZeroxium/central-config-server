package com.example.rest.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * Request DTO for listing users with pagination.
 */
@Data
@Schema(name = "ListUsersRequest", description = "Request payload to list users with pagination")
public class ListUsersRequest {
    
    @Schema(description = "Zero-based page index", example = "0", minimum = "0")
    @Min(value = 0, message = "Page must be non-negative")
    private int page = 0;
    
    @Schema(description = "Page size", example = "20", minimum = "1", maximum = "100")
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size must not exceed 100")
    private int size = 20;
}
