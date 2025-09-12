package com.example.rest.exception;

/**
 * Exception thrown when database operations fail.
 * Maps to HTTP 500 Internal Server Error.
 */
public class DatabaseException extends UserServiceException {
    
    private static final String ERROR_CODE = "DATABASE_ERROR";
    
    public DatabaseException(String message) {
        super(ERROR_CODE, message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
    
    public DatabaseException(String message, String context) {
        super(ERROR_CODE, message, new Object[]{context}, context);
    }
    
    public DatabaseException(String message, String context, Throwable cause) {
        super(ERROR_CODE, message, new Object[]{context}, context, cause);
    }
}
