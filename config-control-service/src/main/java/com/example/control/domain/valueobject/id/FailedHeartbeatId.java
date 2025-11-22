package com.example.control.domain.valueobject.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for FailedHeartbeat.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param id the failed heartbeat identifier
 */
public record FailedHeartbeatId(String id) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param id the failed heartbeat identifier
     * @throws IllegalArgumentException if id is null or blank
     */
    public FailedHeartbeatId {
        Objects.requireNonNull(id, "Failed heartbeat ID cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Failed heartbeat ID cannot be blank");
        }
    }

    /**
     * Factory method for creating FailedHeartbeatId.
     *
     * @param id the failed heartbeat identifier
     * @return a new FailedHeartbeatId
     */
    public static FailedHeartbeatId of(String id) {
        return new FailedHeartbeatId(id);
    }
}

