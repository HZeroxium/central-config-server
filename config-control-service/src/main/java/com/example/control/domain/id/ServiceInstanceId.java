package com.example.control.domain.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing a composite identifier for ServiceInstance.
 * <p>
 * Combines serviceName and instanceId into a single immutable identifier.
 * This ensures type safety and prevents mixing up parameters in method calls.
 * </p>
 *
 * @param serviceName the service name
 * @param instanceId the instance identifier
 */
public record ServiceInstanceId(String serviceName, String instanceId) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param serviceName the service name
     * @param instanceId the instance identifier
     * @throws IllegalArgumentException if either parameter is null or blank
     */
    public ServiceInstanceId {
        Objects.requireNonNull(serviceName, "Service name cannot be null");
        Objects.requireNonNull(instanceId, "Instance ID cannot be null");
        
        if (serviceName.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be blank");
        }
        if (instanceId.isBlank()) {
            throw new IllegalArgumentException("Instance ID cannot be blank");
        }
    }

    /**
     * Factory method for creating ServiceInstanceId.
     *
     * @param serviceName the service name
     * @param instanceId the instance identifier
     * @return a new ServiceInstanceId
     */
    public static ServiceInstanceId of(String serviceName, String instanceId) {
        return new ServiceInstanceId(serviceName, instanceId);
    }

    /**
     * Converts this ID to a MongoDB document ID string.
     * <p>
     * Uses the format "serviceName:instanceId" for composite keys.
     *
     * @return the MongoDB document ID
     */
    public String toDocumentId() {
        return serviceName + ":" + instanceId;
    }

    /**
     * Creates ServiceInstanceId from a MongoDB document ID string.
     *
     * @param documentId the MongoDB document ID in format "serviceName:instanceId"
     * @return a new ServiceInstanceId
     * @throws IllegalArgumentException if the format is invalid
     */
    public static ServiceInstanceId fromDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("Document ID cannot be null or blank");
        }
        
        String[] parts = documentId.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid document ID format: " + documentId);
        }
        
        return new ServiceInstanceId(parts[0], parts[1]);
    }

}
