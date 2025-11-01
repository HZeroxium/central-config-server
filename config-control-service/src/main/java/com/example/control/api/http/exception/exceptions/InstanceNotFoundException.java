package com.example.control.api.http.exception.exceptions;

/**
 * Exception thrown when a service instance is not found.
 */
public class InstanceNotFoundException extends ConfigControlException {

    public InstanceNotFoundException(String serviceName, String instanceId) {
        super("INSTANCE_NOT_FOUND", "Instance not found: " + instanceId + " for service: " + serviceName);
    }

    public InstanceNotFoundException(String serviceName, String instanceId, Throwable cause) {
        super("INSTANCE_NOT_FOUND", "Instance not found: " + instanceId + " for service: " + serviceName, cause);
    }
}
