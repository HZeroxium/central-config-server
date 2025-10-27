package com.example.control.domain.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for ServiceShare.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param id the service share identifier
 */
public record ServiceShareId(String id) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param id the service share identifier
     * @throws IllegalArgumentException if id is null or blank
     */
    public ServiceShareId {
        Objects.requireNonNull(id, "Service share ID cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Service share ID cannot be blank");
        }
    }

    /**
     * Factory method for creating ServiceShareId.
     *
     * @param id the service share identifier
     * @return a new ServiceShareId
     */
    public static ServiceShareId of(String id) {
        return new ServiceShareId(id);
    }
}
