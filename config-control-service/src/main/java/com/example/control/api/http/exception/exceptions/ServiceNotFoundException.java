package com.example.control.api.http.exception.exceptions;

/**
 * Exception thrown when a service is not found.
 */
public class ServiceNotFoundException extends ConfigControlException {

    public ServiceNotFoundException(String serviceName) {
        super("SERVICE_NOT_FOUND", "Service not found: " + serviceName);
    }

    public ServiceNotFoundException(String serviceName, Throwable cause) {
        super("SERVICE_NOT_FOUND", "Service not found: " + serviceName, cause);
    }
}
