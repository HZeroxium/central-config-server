package com.example.control.domain.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing a unique identifier for ServiceInstance.
 * <p>
 * Uses instanceId as the primary identifier since instanceId is globally unique
 * across all services. This simplifies the ID structure and reduces complexity.
 * </p>
 *
 * @param instanceId the globally unique instance identifier
 */
public record ServiceInstanceId(String instanceId) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param instanceId the instance identifier
     * @throws IllegalArgumentException if parameter is null or blank
     */
    public ServiceInstanceId {
        Objects.requireNonNull(instanceId, "Instance ID cannot be null");

        if (instanceId.isBlank()) {
            throw new IllegalArgumentException("Instance ID cannot be blank");
        }
    }

    /**
     * Factory method for creating ServiceInstanceId.
     *
     * @param instanceId the instance identifier
     * @return a new ServiceInstanceId
     */
    public static ServiceInstanceId of(String instanceId) {
        return new ServiceInstanceId(instanceId);
    }

    /**
     * Converts this ID to a MongoDB document ID string.
     * <p>
     * Since instanceId is globally unique, we use it directly as the document ID.
     *
     * @return the MongoDB document ID
     */
    public String toDocumentId() {
        return instanceId;
    }

    /**
     * Creates ServiceInstanceId from a MongoDB document ID string.
     *
     * @param documentId the MongoDB document ID (instanceId)
     * @return a new ServiceInstanceId
     * @throws IllegalArgumentException if the document ID is invalid
     */
    public static ServiceInstanceId fromDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("Document ID cannot be null or blank");
        }

        return new ServiceInstanceId(documentId);
    }

    /**
     * Returns the String representation of this ID.
     *
     * @return the String representation of this ID
     */
    @Override
    public String toString() {
        return instanceId;
    }

}
