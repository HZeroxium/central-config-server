package com.example.control.consulclient.exception;

/**
 * Exception thrown for network/transport errors.
 */
public class TransportException extends ConsulException {
    
    private final String endpoint;
    private final int statusCode;
    
    public TransportException(String endpoint, String message) {
        super("TRANSPORT_ERROR", String.format("Transport error for endpoint '%s': %s", endpoint, message));
        this.endpoint = endpoint;
        this.statusCode = 0;
    }
    
    public TransportException(String endpoint, String message, int statusCode) {
        super("TRANSPORT_ERROR", String.format("Transport error for endpoint '%s' (status %d): %s", endpoint, statusCode, message));
        this.endpoint = endpoint;
        this.statusCode = statusCode;
    }
    
    public TransportException(String endpoint, String message, Throwable cause) {
        super("TRANSPORT_ERROR", String.format("Transport error for endpoint '%s': %s", endpoint, message), cause);
        this.endpoint = endpoint;
        this.statusCode = 0;
    }
    
    public TransportException(String endpoint, String message, int statusCode, Throwable cause) {
        super("TRANSPORT_ERROR", String.format("Transport error for endpoint '%s' (status %d): %s", endpoint, statusCode, message), cause);
        this.endpoint = endpoint;
        this.statusCode = statusCode;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
