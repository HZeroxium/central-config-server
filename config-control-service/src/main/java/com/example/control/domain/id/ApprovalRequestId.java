package com.example.control.domain.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for ApprovalRequest.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param id the approval request identifier
 */
public record ApprovalRequestId(String id) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param id the approval request identifier
     * @throws IllegalArgumentException if id is null or blank
     */
    public ApprovalRequestId {
        Objects.requireNonNull(id, "Approval request ID cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Approval request ID cannot be blank");
        }
    }

    /**
     * Factory method for creating ApprovalRequestId.
     *
     * @param id the approval request identifier
     * @return a new ApprovalRequestId
     */
    public static ApprovalRequestId of(String id) {
        return new ApprovalRequestId(id);
    }
}
