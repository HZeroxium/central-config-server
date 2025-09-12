package com.example.rest.exception;

/**
 * Centralized error codes for the user service.
 * Provides consistent error code management across the application.
 */
public final class ErrorCode {
    
    // User related errors
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String USER_VALIDATION_FAILED = "USER_VALIDATION_FAILED";
    public static final String USER_CREATION_FAILED = "USER_CREATION_FAILED";
    public static final String USER_UPDATE_FAILED = "USER_UPDATE_FAILED";
    public static final String USER_DELETION_FAILED = "USER_DELETION_FAILED";
    
    // Thrift service errors
    public static final String THRIFT_SERVICE_ERROR = "THRIFT_SERVICE_ERROR";
    public static final String THRIFT_SERVICE_UNAVAILABLE = "THRIFT_SERVICE_UNAVAILABLE";
    public static final String THRIFT_SERVICE_TIMEOUT = "THRIFT_SERVICE_TIMEOUT";
    public static final String THRIFT_SERVICE_CONNECTION_FAILED = "THRIFT_SERVICE_CONNECTION_FAILED";
    
    // Database errors
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String DATABASE_CONNECTION_FAILED = "DATABASE_CONNECTION_FAILED";
    public static final String DATABASE_CONSTRAINT_VIOLATION = "DATABASE_CONSTRAINT_VIOLATION";
    public static final String DATABASE_TIMEOUT = "DATABASE_TIMEOUT";
    
    // Validation errors
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";
    
    // System errors
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    
    private ErrorCode() {
        // Utility class
    }
}
