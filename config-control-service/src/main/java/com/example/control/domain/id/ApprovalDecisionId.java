package com.example.control.domain.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for ApprovalDecision.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param id the approval decision identifier
 */
public record ApprovalDecisionId(String id) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param id the approval decision identifier
     * @throws IllegalArgumentException if id is null or blank
     */
    public ApprovalDecisionId {
        Objects.requireNonNull(id, "Approval decision ID cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Approval decision ID cannot be blank");
        }
    }

    /**
     * Factory method for creating ApprovalDecisionId.
     *
     * @param id the approval decision identifier
     * @return a new ApprovalDecisionId
     */
    public static ApprovalDecisionId of(String id) {
        return new ApprovalDecisionId(id);
    }
}
