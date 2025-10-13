package com.example.control.consulclient.exception;

/**
 * Exception thrown when a resource is not found (404 Not Found).
 */
public class NotFoundException extends ConsulException {
    
    private final String resource;
    
    public NotFoundException(String resource) {
        super("NOT_FOUND", String.format("Resource not found: %s", resource));
        this.resource = resource;
    }
    
    public NotFoundException(String resource, Throwable cause) {
        super("NOT_FOUND", String.format("Resource not found: %s", resource), cause);
        this.resource = resource;
    }
    
    public String getResource() {
        return resource;
    }
}
