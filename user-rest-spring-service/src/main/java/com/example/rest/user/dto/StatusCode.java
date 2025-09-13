package com.example.rest.user.dto;

/**
 * Status codes for API responses.
 * Using integers instead of booleans for better extensibility.
 */
public final class StatusCode {
    
    // Success codes
    public static final int SUCCESS = 0;
    
    // Client error codes (1-99)
    public static final int VALIDATION_ERROR = 1;
    public static final int USER_NOT_FOUND = 2;
    public static final int USER_ALREADY_EXISTS = 3;
    public static final int INVALID_INPUT = 4;
    public static final int MISSING_REQUIRED_FIELD = 5;
    public static final int INVALID_FORMAT = 6;
    public static final int UNAUTHORIZED = 7;
    public static final int FORBIDDEN = 8;
    public static final int NOT_FOUND = 9;
    public static final int METHOD_NOT_ALLOWED = 10;
    public static final int CONFLICT = 11;
    public static final int UNPROCESSABLE_ENTITY = 12;
    public static final int TOO_MANY_REQUESTS = 13;
    
    // Server error codes (100-199)
    public static final int INTERNAL_SERVER_ERROR = 100;
    public static final int DATABASE_ERROR = 101;
    public static final int DATABASE_CONNECTION_FAILED = 102;
    public static final int DATABASE_TIMEOUT = 103;
    public static final int DATABASE_CONSTRAINT_VIOLATION = 104;
    public static final int THRIFT_SERVICE_ERROR = 105;
    public static final int THRIFT_SERVICE_UNAVAILABLE = 106;
    public static final int THRIFT_SERVICE_TIMEOUT = 107;
    public static final int THRIFT_SERVICE_CONNECTION_FAILED = 108;
    public static final int SERVICE_UNAVAILABLE = 109;
    public static final int GATEWAY_TIMEOUT = 110;
    public static final int BAD_GATEWAY = 111;
    
    // Business logic error codes (200-299)
    public static final int BUSINESS_RULE_VIOLATION = 200;
    public static final int INSUFFICIENT_PERMISSIONS = 201;
    public static final int RESOURCE_LOCKED = 202;
    public static final int OPERATION_NOT_SUPPORTED = 203;
    
    private StatusCode() {
        // Utility class
    }
    
    /**
     * Get HTTP status code from status code.
     */
    public static int getHttpStatus(int statusCode) {
        if (statusCode == SUCCESS) {
            return 200;
        } else if (statusCode >= VALIDATION_ERROR && statusCode <= TOO_MANY_REQUESTS) {
            return 400 + (statusCode - VALIDATION_ERROR);
        } else if (statusCode >= INTERNAL_SERVER_ERROR && statusCode <= BAD_GATEWAY) {
            return 500 + (statusCode - INTERNAL_SERVER_ERROR);
        } else if (statusCode >= BUSINESS_RULE_VIOLATION && statusCode <= OPERATION_NOT_SUPPORTED) {
            return 422; // Unprocessable Entity
        }
        return 500; // Default to Internal Server Error
    }
    
    /**
     * Get status message from status code.
     */
    public static String getStatusMessage(int statusCode) {
        return switch (statusCode) {
            case SUCCESS -> "Success";
            case VALIDATION_ERROR -> "Validation error";
            case USER_NOT_FOUND -> "User not found";
            case USER_ALREADY_EXISTS -> "User already exists";
            case INVALID_INPUT -> "Invalid input";
            case MISSING_REQUIRED_FIELD -> "Missing required field";
            case INVALID_FORMAT -> "Invalid format";
            case UNAUTHORIZED -> "Unauthorized";
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Not found";
            case METHOD_NOT_ALLOWED -> "Method not allowed";
            case CONFLICT -> "Conflict";
            case UNPROCESSABLE_ENTITY -> "Unprocessable entity";
            case TOO_MANY_REQUESTS -> "Too many requests";
            case INTERNAL_SERVER_ERROR -> "Internal server error";
            case DATABASE_ERROR -> "Database error";
            case DATABASE_CONNECTION_FAILED -> "Database connection failed";
            case DATABASE_TIMEOUT -> "Database timeout";
            case DATABASE_CONSTRAINT_VIOLATION -> "Database constraint violation";
            case THRIFT_SERVICE_ERROR -> "Thrift service error";
            case THRIFT_SERVICE_UNAVAILABLE -> "Thrift service unavailable";
            case THRIFT_SERVICE_TIMEOUT -> "Thrift service timeout";
            case THRIFT_SERVICE_CONNECTION_FAILED -> "Thrift service connection failed";
            case SERVICE_UNAVAILABLE -> "Service unavailable";
            case GATEWAY_TIMEOUT -> "Gateway timeout";
            case BAD_GATEWAY -> "Bad gateway";
            case BUSINESS_RULE_VIOLATION -> "Business rule violation";
            case INSUFFICIENT_PERMISSIONS -> "Insufficient permissions";
            case RESOURCE_LOCKED -> "Resource locked";
            case OPERATION_NOT_SUPPORTED -> "Operation not supported";
            default -> "Unknown error";
        };
    }
}
