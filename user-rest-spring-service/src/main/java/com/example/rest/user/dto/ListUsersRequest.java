package com.example.rest.user.dto;

import com.example.rest.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO for listing users with pagination, sorting, filtering, and searching.
 */
@Data
@Schema(name = "ListUsersRequest", description = "Request payload to list users with pagination, sorting, filtering, searching")
public class ListUsersRequest {
    
    @Schema(description = "Zero-based page index", example = "0", minimum = "0")
    @Min(value = 0, message = "Page must be non-negative")
    private int page = 0;
    
    @Schema(description = "Page size", example = "20", minimum = "1", maximum = "100")
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size must not exceed 100")
    private int size = 20;
    
    @Schema(description = "General search term to match name, phone, or address", example = "Alice")
    private String search;
    
    @Schema(description = "Filter by user status", example = "ACTIVE")
    private User.UserStatus status;
    
    @Schema(description = "Filter by user role", example = "USER")
    private User.UserRole role;
    
    @Schema(description = "Sort by field", example = "createdAt")
    @Pattern(regexp = "^(id|name|phone|address|status|role|createdAt|updatedAt|version)$", 
             message = "Sort field must be one of: id, name, phone, address, status, role, createdAt, updatedAt, version")
    private String sortBy = "createdAt";
    
    @Schema(description = "Sort direction: asc or desc", example = "desc")
    @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
    private String sortDir = "desc";
    
    @Schema(description = "Include soft-deleted users", example = "false")
    private Boolean includeDeleted = false;
    
    @Schema(description = "Created after (ISO date time)", example = "2025-01-01T00:00:00")
    private LocalDateTime createdAfter;
    
    @Schema(description = "Created before (ISO date time)", example = "2025-12-31T23:59:59")
    private LocalDateTime createdBefore;
        
    @Schema(description = "Sort by multiple fields, comma separated", example = "status,createdAt")
    @Pattern(regexp = "^[a-zA-Z,]+$", message = "Multiple sort fields must contain only letters and commas")
    private String sortByMultiple;
    
    @Schema(description = "Sort directions for multiple fields, comma separated matching sortByMultiple", example = "asc,desc")
    @Pattern(regexp = "^[a-zA-Z,]+$", message = "Multiple sort directions must contain only letters and commas")
    private String sortDirMultiple;
}
