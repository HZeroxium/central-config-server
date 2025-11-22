package com.example.control.domain.valueobject.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for ServiceCredential.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param id the service credential identifier
 */
public record ServiceCredentialId(String id) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param id the service credential identifier
     * @throws IllegalArgumentException if id is null or blank
     */
    public ServiceCredentialId {
        Objects.requireNonNull(id, "Service credential ID cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Service credential ID cannot be blank");
        }
    }

    /**
     * Factory method for creating ServiceCredentialId.
     *
     * @param id the service credential identifier
     * @return a new ServiceCredentialId
     */
    public static ServiceCredentialId of(String id) {
        return new ServiceCredentialId(id);
    }

}

