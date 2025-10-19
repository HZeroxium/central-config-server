package com.example.control.domain.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for IamUser.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param userId the IAM user identifier
 */
public record IamUserId(String userId) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param userId the IAM user identifier
     * @throws IllegalArgumentException if userId is null or blank
     */
    public IamUserId {
        Objects.requireNonNull(userId, "IAM user ID cannot be null");
        
        if (userId.isBlank()) {
            throw new IllegalArgumentException("IAM user ID cannot be blank");
        }
    }

    /**
     * Factory method for creating IamUserId.
     *
     * @param userId the IAM user identifier
     * @return a new IamUserId
     */
    public static IamUserId of(String userId) {
        return new IamUserId(userId);
    }
}
