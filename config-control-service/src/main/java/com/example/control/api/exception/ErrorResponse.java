package com.example.control.api.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response following RFC-7807 Problem Details for HTTP APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
   * Timestamp when the error occurred.
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
  private Instant timestamp;

  /**
   * Trace ID for correlation across services.
   */
  private String traceId;

  /**
   * Additional context about the error.
   */
  private Map<String, Object> context;

  /**
   * Validation errors (for validation failures).
   */
  private List<ValidationError> validationErrors;

  /**
   * Individual validation error details.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidationError {
    private String field;
    private Object rejectedValue;
    private String message;
    private String code;
  }

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
}
