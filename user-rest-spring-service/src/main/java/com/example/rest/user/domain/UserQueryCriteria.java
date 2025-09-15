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
    
    // Flexible sorting criteria
    private List<SortCriterion> sortCriteria;
    
    /**
     * Get sort criteria with default if not specified
     */
    public List<SortCriterion> getSortCriteria() {
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            return List.of(SortCriterion.getDefault());
        }
        return sortCriteria;
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
     * Check if sorting is enabled
     */
    public boolean hasSorting() {
        return sortCriteria != null && !sortCriteria.isEmpty();
    }
}
