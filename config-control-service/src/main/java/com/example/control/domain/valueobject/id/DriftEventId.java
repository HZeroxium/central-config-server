package com.example.control.domain.valueobject.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for DriftEvent.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param id the drift event identifier
 */
public record DriftEventId(String id) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param id the drift event identifier
     * @throws IllegalArgumentException if id is null or blank
     */
    public DriftEventId {
        Objects.requireNonNull(id, "Drift event ID cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Drift event ID cannot be blank");
        }
    }

    /**
     * Factory method for creating DriftEventId.
     *
     * @param id the drift event identifier
     * @return a new DriftEventId
     */
    public static DriftEventId of(String id) {
        return new DriftEventId(id);
    }
}
