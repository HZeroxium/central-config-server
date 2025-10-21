package com.example.control.api.exception;

import com.example.control.api.exception.exceptions.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.kafka.KafkaException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Global exception handler for Config Control Service.
 * Provides centralized error handling and standardized error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String INTERNAL_ERROR_TYPE = "https://api.example.com/problems/internal-error";
  private static final String VALIDATION_ERROR_TYPE = "https://api.example.com/problems/validation-error";
  private static final String NOT_FOUND_TYPE = "https://api.example.com/problems/not-found";
  private static final String BAD_REQUEST_TYPE = "https://api.example.com/problems/bad-request";
  private static final String EXTERNAL_SERVICE_TYPE = "https://api.example.com/problems/external-service-error";

  /**
   * Handle ConfigControlException and its subclasses.
   */
  @ExceptionHandler(ConfigControlException.class)
  public ResponseEntity<ErrorResponse> handleConfigControlException(ConfigControlException ex, WebRequest request) {
    log.warn("ConfigControlException: {}", ex.getMessage(), ex);

    HttpStatus status = determineHttpStatus(ex);
    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(getErrorType(ex))
        .title(ex.getErrorCode() != null ? ex.getErrorCode() : "Config Control Error")
        .status(status.value())
        .detail(ex.getMessage())
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.status(status).body(errorResponse);
  }

  /**
   * Handle validation errors from @Valid annotations.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
      WebRequest request) {
    log.warn("Validation error: {}", ex.getMessage());

    List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      if (error instanceof FieldError fieldError) {
        validationErrors.add(ErrorResponse.ValidationError.builder()
            .field(fieldError.getField())
            .rejectedValue(fieldError.getRejectedValue())
            .message(fieldError.getDefaultMessage())
            .code(fieldError.getCode())
            .build());
      }
    });

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(VALIDATION_ERROR_TYPE)
        .title("Validation Failed")
        .status(HttpStatus.BAD_REQUEST.value())
        .detail("Request validation failed")
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .validationErrors(validationErrors)
        .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handle constraint violation exceptions.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex,
      WebRequest request) {
    log.warn("Constraint violation: {}", ex.getMessage());

    List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      String fieldName = violation.getPropertyPath().toString();
      validationErrors.add(ErrorResponse.ValidationError.builder()
          .field(fieldName)
          .rejectedValue(violation.getInvalidValue())
          .message(violation.getMessage())
          .code(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
          .build());
    }

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(VALIDATION_ERROR_TYPE)
        .title("Constraint Violation")
        .status(HttpStatus.BAD_REQUEST.value())
        .detail("Request validation failed")
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .validationErrors(validationErrors)
        .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handle binding exceptions.
   */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBindException(BindException ex, WebRequest request) {
    log.warn("Binding error: {}", ex.getMessage());

    List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      if (error instanceof FieldError fieldError) {
        validationErrors.add(ErrorResponse.ValidationError.builder()
            .field(fieldError.getField())
            .rejectedValue(fieldError.getRejectedValue())
            .message(fieldError.getDefaultMessage())
            .code(fieldError.getCode())
            .build());
      }
    });

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(VALIDATION_ERROR_TYPE)
        .title("Binding Error")
        .status(HttpStatus.BAD_REQUEST.value())
        .detail("Request binding failed")
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .validationErrors(validationErrors)
        .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handle JSON parsing errors.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
      WebRequest request) {
    log.warn("JSON parsing error: {}", ex.getMessage());

    String detail = "Invalid JSON format";
    if (ex.getCause() instanceof InvalidFormatException formatEx) {
      detail = String.format("Invalid value '%s' for field '%s'. Expected %s",
          formatEx.getValue(),
          formatEx.getPath().get(0).getFieldName(),
          formatEx.getTargetType().getSimpleName());
    } else if (ex.getCause() instanceof MismatchedInputException mismatchEx) {
      detail = String.format("Invalid input for field '%s'. Expected %s",
          mismatchEx.getPath().get(0).getFieldName(),
          mismatchEx.getTargetType().getSimpleName());
    }

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(BAD_REQUEST_TYPE)
        .title("Invalid JSON")
        .status(HttpStatus.BAD_REQUEST.value())
        .detail(detail)
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handle missing request parameters.
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex, WebRequest request) {
    log.warn("Missing request parameter: {}", ex.getMessage());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(BAD_REQUEST_TYPE)
        .title("Missing Parameter")
        .status(HttpStatus.BAD_REQUEST.value())
        .detail(String.format("Required parameter '%s' is not present", ex.getParameterName()))
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handle method argument type mismatch.
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex,
      WebRequest request) {
    log.warn("Type mismatch error: {}", ex.getMessage());

    String expectedType = "unknown type";
    Class<?> requiredType = ex.getRequiredType();
    if (requiredType != null) {
      expectedType = requiredType.getSimpleName();
    }

    String detail = String.format("Invalid value '%s' for parameter '%s'. Expected %s",
        ex.getValue(),
        ex.getName(),
        expectedType);

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(BAD_REQUEST_TYPE)
        .title("Type Mismatch")
        .status(HttpStatus.BAD_REQUEST.value())
        .detail(detail)
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handle unsupported HTTP methods.
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException ex, WebRequest request) {
    log.warn("Unsupported HTTP method: {}", ex.getMessage());

    String detail = String.format("Method '%s' is not supported for this endpoint. Supported methods: %s",
        ex.getMethod(),
        String.join(", ", ex.getSupportedMethods()));

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(BAD_REQUEST_TYPE)
        .title("Method Not Allowed")
        .status(HttpStatus.METHOD_NOT_ALLOWED.value())
        .detail(detail)
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
  }

  /**
   * Handle 404 errors.
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
    log.warn("No handler found: {}", ex.getMessage());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(NOT_FOUND_TYPE)
        .title("Not Found")
        .status(HttpStatus.NOT_FOUND.value())
        .detail(String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()))
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /**
   * Handle database access exceptions.
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, WebRequest request) {
    log.error("Database access error: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(INTERNAL_ERROR_TYPE)
        .title("Database Error")
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .detail("A database error occurred while processing the request")
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /**
   * Handle Kafka exceptions.
   */
  @ExceptionHandler(KafkaException.class)
  public ResponseEntity<ErrorResponse> handleKafkaException(KafkaException ex, WebRequest request) {
    log.error("Kafka error: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(EXTERNAL_SERVICE_TYPE)
        .title("Message Queue Error")
        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
        .detail("A message queue error occurred while processing the request")
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
  }

  /**
   * Handle all other exceptions.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.builder()
        .type(INTERNAL_ERROR_TYPE)
        .title("Internal Server Error")
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .detail("An unexpected error occurred while processing the request")
        .instance(request.getDescription(false))
        .timestamp(Instant.now())
        .traceId(generateTraceId())
        .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /**
   * Determine HTTP status based on exception type.
   */
  private HttpStatus determineHttpStatus(ConfigControlException ex) {
    if (ex instanceof ServiceNotFoundException || ex instanceof InstanceNotFoundException) {
      return HttpStatus.NOT_FOUND;
    } else if (ex instanceof ValidationException) {
      return HttpStatus.BAD_REQUEST;
    } else if (ex instanceof ConfigurationException) {
      return HttpStatus.UNPROCESSABLE_ENTITY;
    } else if (ex instanceof ExternalServiceException) {
      return HttpStatus.SERVICE_UNAVAILABLE;
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /**
   * Get error type based on exception class.
   */
  private String getErrorType(ConfigControlException ex) {
    if (ex instanceof ServiceNotFoundException || ex instanceof InstanceNotFoundException) {
      return NOT_FOUND_TYPE;
    } else if (ex instanceof ValidationException) {
      return VALIDATION_ERROR_TYPE;
    } else if (ex instanceof ConfigurationException) {
      return BAD_REQUEST_TYPE;
    } else if (ex instanceof ExternalServiceException) {
      return EXTERNAL_SERVICE_TYPE;
    }
    return INTERNAL_ERROR_TYPE;
  }

  /**
   * Generate a trace ID for correlation.
   */
  private String generateTraceId() {
    return UUID.randomUUID().toString();
  }
}
