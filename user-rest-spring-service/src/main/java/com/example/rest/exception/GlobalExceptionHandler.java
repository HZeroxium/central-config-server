package com.example.rest.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for the REST service.
 * Provides centralized error handling and consistent error responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final String DEFAULT_ERROR_TYPE = "about:blank";
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {
        
        log.warn("User not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("User Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .code(ex.getErrorCode())
                .timestamp(LocalDateTime.now())
                .context(ex.getContext())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(UserValidationException.class)
    public ResponseEntity<ErrorResponse> handleUserValidationException(
            UserValidationException ex, HttpServletRequest request) {
        
        log.warn("User validation failed: {}", ex.getMessage());
        
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .code(ex.getErrorCode())
                .timestamp(LocalDateTime.now())
                .context(ex.getContext())
                .correlationId(generateCorrelationId());
        
        if (ex.getValidationErrors() != null) {
            builder.validationErrors(Map.of("errors", (Object) ex.getValidationErrors()));
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(builder.build());
    }
    
    @ExceptionHandler(ThriftServiceException.class)
    public ResponseEntity<ErrorResponse> handleThriftServiceException(
            ThriftServiceException ex, HttpServletRequest request) {
        
        log.error("Thrift service error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Service Unavailable")
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .code(ex.getErrorCode())
                .timestamp(LocalDateTime.now())
                .context(ex.getContext())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(
            DatabaseException ex, HttpServletRequest request) {
        
        log.error("Database error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("A database error occurred. Please try again later.")
                .instance(request.getRequestURI())
                .code(ex.getErrorCode())
                .timestamp(LocalDateTime.now())
                .context(ex.getContext())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        log.warn("Validation failed: {}", ex.getMessage());
        
        Map<String, Object> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (Object) entry.getValue()
                ));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Request validation failed")
                .instance(request.getRequestURI())
                .code(ErrorCode.VALIDATION_ERROR)
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        log.warn("Constraint violation: {}", ex.getMessage());
        
        Map<String, Object> validationErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.groupingBy(
                        violation -> violation.getPropertyPath().toString(),
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (Object) entry.getValue()
                ));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Request validation failed")
                .instance(request.getRequestURI())
                .code(ErrorCode.VALIDATION_ERROR)
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        log.warn("Invalid request body: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Bad Request")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Invalid request body format")
                .instance(request.getRequestURI())
                .code(ErrorCode.INVALID_INPUT)
                .timestamp(LocalDateTime.now())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Bad Request")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(String.format("Invalid value for parameter '%s'", ex.getName()))
                .instance(request.getRequestURI())
                .code(ErrorCode.INVALID_FORMAT)
                .timestamp(LocalDateTime.now())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        log.warn("Missing required parameter: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Bad Request")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(String.format("Missing required parameter: %s", ex.getParameterName()))
                .instance(request.getRequestURI())
                .code(ErrorCode.MISSING_REQUIRED_FIELD)
                .timestamp(LocalDateTime.now())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        log.warn("Method not supported: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Method Not Allowed")
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .detail(String.format("Method '%s' is not supported for this endpoint", ex.getMethod()))
                .instance(request.getRequestURI())
                .code("METHOD_NOT_ALLOWED")
                .timestamp(LocalDateTime.now())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
    
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        log.warn("No handler found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail("The requested endpoint was not found")
                .instance(request.getRequestURI())
                .code("ENDPOINT_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceException(
            UserServiceException ex, HttpServletRequest request) {
        
        log.error("User service error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Service Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .code(ex.getErrorCode())
                .timestamp(LocalDateTime.now())
                .context(ex.getContext())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(DEFAULT_ERROR_TYPE)
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred. Please try again later.")
                .instance(request.getRequestURI())
                .code(ErrorCode.INTERNAL_SERVER_ERROR)
                .timestamp(LocalDateTime.now())
                .correlationId(generateCorrelationId())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
