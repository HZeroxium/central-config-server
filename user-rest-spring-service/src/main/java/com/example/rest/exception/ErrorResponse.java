package com.example.rest.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Structured error response DTO for consistent API error formatting.
 * Follows RFC 7807 Problem Details for HTTP APIs standard.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * A URI reference that identifies the problem type.
     */
    private String type;
    
    /**
     * A short, human-readable summary of the problem type.
     */
    private String title;
    
    /**
     * The HTTP status code.
     */
    private int status;
    
    /**
     * A human-readable explanation specific to this occurrence of the problem.
     */
    private String detail;
    
    /**
     * A URI reference that identifies the specific occurrence of the problem.
     */
    private String instance;
    
    /**
     * Application-specific error code.
     */
    private String code;
    
    /**
     * Timestamp when the error occurred.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    /**
     * Additional context information.
     */
    private String context;
    
    /**
     * Validation errors (for validation failures).
     */
    private Map<String, Object> validationErrors;
    
    /**
     * Additional error details.
     */
    private Map<String, Object> details;
    
    /**
     * Request correlation ID for tracing.
     */
    private String correlationId;
}
