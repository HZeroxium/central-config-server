package com.example.rest.user.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

/**
 * Sort criterion for flexible sorting in domain layer.
 */
@Data
@Builder
public class SortCriterion {
    
    @NotBlank(message = "Field name is required")
    @Pattern(regexp = "^(id|name|phone|address|status|role|createdAt|updatedAt|version)$", 
             message = "Field name must be one of: id, name, phone, address, status, role, createdAt, updatedAt, version")
    private String fieldName;
    
    @NotBlank(message = "Sort direction is required")
    @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
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
