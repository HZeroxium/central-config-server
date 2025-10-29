package com.example.control.api.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response following RFC-7807 Problem Details for HTTP APIs.
 * <p>
 * This class provides a consistent error response format across all API endpoints,
 * ensuring proper error handling and client integration. All error responses
 * follow the RFC-7807 standard for problem details.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response following RFC-7807 Problem Details for HTTP APIs")
public class ErrorResponse {

    /**
     * A URI reference that identifies the problem type.
     */
    @Schema(description = "A URI reference that identifies the problem type",
            example = "https://api.example.com/problems/validation-error")
    private String type;

    /**
     * A short, human-readable summary of the problem type.
     */
    @Schema(description = "A short, human-readable summary of the problem type",
            example = "Validation Failed")
    private String title;

    /**
     * The HTTP status code.
     */
    @Schema(description = "The HTTP status code",
            example = "400")
    private int status;

    /**
     * A human-readable explanation specific to this occurrence of the problem.
     */
    @Schema(description = "A human-readable explanation specific to this occurrence of the problem",
            example = "Request validation failed")
    private String detail;

    /**
     * A URI reference that identifies the specific occurrence of the problem.
     */
    @Schema(description = "A URI reference that identifies the specific occurrence of the problem",
            example = "/api/application-services")
    private String instance;

    /**
     * Timestamp when the error occurred.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Timestamp when the error occurred",
            example = "2024-01-15T10:30:45.123Z")
    private Instant timestamp;

    /**
     * Trace ID for correlation across services.
     */
    @Schema(description = "Trace ID for correlation across services",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String traceId;

    /**
     * Additional context about the error.
     */
    @Schema(description = "Additional context about the error")
    private Map<String, Object> context;

    /**
     * Validation errors (for validation failures).
     */
    @Schema(description = "Validation errors (for validation failures)")
    private List<ValidationError> validationErrors;

    /**
     * Create a simple error response.
     */
    public static ErrorResponse of(String title, int status, String detail) {
        return ErrorResponse.builder()
                .title(title)
                .status(status)
                .detail(detail)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with type and instance.
     */
    public static ErrorResponse of(String type, String title, int status, String detail, String instance) {
        return ErrorResponse.builder()
                .type(type)
                .title(title)
                .status(status)
                .detail(detail)
                .instance(instance)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Individual validation error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual validation error details")
    public static class ValidationError {
        @Schema(description = "The field that failed validation",
                example = "serviceId")
        private String field;

        @Schema(description = "The value that was rejected",
                example = "invalid-service-id")
        private Object rejectedValue;

        @Schema(description = "Human-readable error message",
                example = "Service ID must not exceed 100 characters")
        private String message;

        @Schema(description = "Validation constraint code",
                example = "Size")
        private String code;
    }
}
