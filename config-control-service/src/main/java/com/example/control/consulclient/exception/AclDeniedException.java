package com.example.control.consulclient.exception;

/**
 * Exception thrown when ACL permissions are denied (403 Forbidden).
 */
public class AclDeniedException extends ConsulException {
    
    private final String operation;
    private final String resource;
    
    public AclDeniedException(String operation, String resource) {
        super("ACL_DENIED", String.format("ACL denied for operation '%s' on resource '%s'", operation, resource));
        this.operation = operation;
        this.resource = resource;
    }
    
    public AclDeniedException(String operation, String resource, Throwable cause) {
        super("ACL_DENIED", String.format("ACL denied for operation '%s' on resource '%s'", operation, resource), cause);
        this.operation = operation;
        this.resource = resource;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getResource() {
        return resource;
    }
}
