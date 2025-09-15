package com.example.user.domain;

import lombok.Builder;
import lombok.Data;

/**
 * Sort criterion for flexible sorting in domain layer.
 */
@Data
@Builder
public class SortCriterion {
    
    private String fieldName;
    private String direction;
    
    /**
     * Get default sort criterion if not specified.
     */
    public static SortCriterion getDefault() {
        return SortCriterion.builder()
                .fieldName("createdAt")
                .direction("desc")
                .build();
    }
    
    /**
     * Check if this is a valid sort criterion.
     */
    public boolean isValid() {
        return fieldName != null && !fieldName.trim().isEmpty() &&
               direction != null && ("asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction));
    }
}
