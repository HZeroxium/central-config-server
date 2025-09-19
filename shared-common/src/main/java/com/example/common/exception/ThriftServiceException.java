package com.example.common.exception;

/**
 * Exception thrown when Thrift service operations fail.
 * Maps to HTTP 502 Bad Gateway or 503 Service Unavailable.
 */
public class ThriftServiceException extends UserServiceException {
    
    private static final String ERROR_CODE = "THRIFT_SERVICE_ERROR";
    
    public ThriftServiceException(String message) {
        super(ERROR_CODE, message);
    }
    
    public ThriftServiceException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
    
    public ThriftServiceException(String message, String context) {
        super(ERROR_CODE, message, new Object[]{context}, context);
    }
    
    public ThriftServiceException(String message, String context, Throwable cause) {
        super(ERROR_CODE, message, new Object[]{context}, context, cause);
    }
}
