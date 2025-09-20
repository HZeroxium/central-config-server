package com.example.rest.user.dto;

import com.example.common.domain.User;
import com.example.common.domain.SortCriterion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for listing users with pagination, sorting, filtering, and
 * searching.
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

    @Schema(description = "Include soft-deleted users", example = "false")
    private Boolean includeDeleted = false;

    @Schema(description = "Created after (ISO date time)", example = "2025-01-01T00:00:00")
    private LocalDateTime createdAfter;

    @Schema(description = "Created before (ISO date time)", example = "2025-12-31T23:59:59")
    private LocalDateTime createdBefore;

    @Schema(description = "Sort criteria for flexible sorting", example = "[{\"fieldName\": \"createdAt\", \"direction\": \"desc\"}]")
    @Valid
    @Size(max = 5, message = "Maximum 5 sort criteria allowed")
    private List<SortCriterion> sortCriteria;
}
