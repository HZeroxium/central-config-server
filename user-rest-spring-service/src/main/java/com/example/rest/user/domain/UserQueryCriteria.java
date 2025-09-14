package com.example.rest.user.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Query criteria for advanced user search, filtering, and sorting.
 */
@Data
@Builder
public class UserQueryCriteria {
    
    // Pagination
    private int page;
    private int size;
    
    // Search
    private String search;
    
    // Filters
    private User.UserStatus status;
    private User.UserRole role;
    private Boolean includeDeleted;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    
    // Single field sorting
    private String sortBy;
    private String sortDir;
    
    // Multiple field sorting
    private List<String> sortByMultiple;
    private List<String> sortDirMultiple;
    
    /**
     * Get default sort field if not specified
     */
    public String getSortBy() {
        return sortBy != null ? sortBy : "createdAt";
    }
    
    /**
     * Get default sort direction if not specified
     */
    public String getSortDir() {
        return sortDir != null ? sortDir : "desc";
    }
    
    /**
     * Check if search is enabled
     */
    public boolean hasSearch() {
        return search != null && !search.trim().isEmpty();
    }
    
    /**
     * Check if status filter is enabled
     */
    public boolean hasStatusFilter() {
        return status != null;
    }
    
    /**
     * Check if role filter is enabled
     */
    public boolean hasRoleFilter() {
        return role != null;
    }
    
    /**
     * Check if date range filter is enabled
     */
    public boolean hasDateRangeFilter() {
        return createdAfter != null || createdBefore != null;
    }
    
    /**
     * Check if multiple field sorting is enabled
     */
    public boolean hasMultipleSorting() {
        return sortByMultiple != null && !sortByMultiple.isEmpty();
    }
}
