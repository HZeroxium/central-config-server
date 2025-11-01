package com.example.control.domain.valueobject.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for ApplicationService.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param id the application service identifier
 */
public record ApplicationServiceId(String id) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param id the application service identifier
     * @throws IllegalArgumentException if id is null or blank
     */
    public ApplicationServiceId {
        Objects.requireNonNull(id, "Application service ID cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Application service ID cannot be blank");
        }
    }

    /**
     * Factory method for creating ApplicationServiceId.
     *
     * @param id the application service identifier
     * @return a new ApplicationServiceId
     */
    public static ApplicationServiceId of(String id) {
        return new ApplicationServiceId(id);
    }

}
