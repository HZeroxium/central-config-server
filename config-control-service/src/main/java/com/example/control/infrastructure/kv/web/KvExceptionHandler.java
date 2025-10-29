// package com.example.control.kv.web;

// import com.example.control.kv.model.KvDtos;
// import jakarta.validation.ConstraintViolationException;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.http.converter.HttpMessageNotReadableException;
// import org.springframework.web.bind.MethodArgumentNotValidException;
// import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.web.bind.annotation.RestControllerAdvice;
// import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// import java.time.Instant;
// import java.util.stream.Collectors;

// @Slf4j
// @RestControllerAdvice
// public class KvExceptionHandler {

//   @ExceptionHandler(MethodArgumentNotValidException.class)
//   public ResponseEntity<KvDtos.ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
//     log.warn("Validation error: {}", ex.getMessage());

//     String message = ex.getBindingResult().getFieldErrors().stream()
//         .map(error -> error.getField() + ": " + error.getDefaultMessage())
//         .collect(Collectors.joining(", "));

//     KvDtos.ErrorResponse error = new KvDtos.ErrorResponse(
//         "VALIDATION_ERROR",
//         message,
//         Instant.now().toString()
//     );

//     return ResponseEntity.badRequest().body(error);
//   }

//   @ExceptionHandler(ConstraintViolationException.class)
//   public ResponseEntity<KvDtos.ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
//     log.warn("Constraint violation: {}", ex.getMessage());

//     String message = ex.getConstraintViolations().stream()
//         .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
//         .collect(Collectors.joining(", "));

//     KvDtos.ErrorResponse error = new KvDtos.ErrorResponse(
//         "CONSTRAINT_VIOLATION",
//         message,
//         Instant.now().toString()
//     );

//     return ResponseEntity.badRequest().body(error);
//   }

//   @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//   public ResponseEntity<KvDtos.ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
//     log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());

//     String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
//         ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

//     KvDtos.ErrorResponse error = new KvDtos.ErrorResponse(
//         "TYPE_MISMATCH",
//         message,
//         Instant.now().toString()
//     );

//     return ResponseEntity.badRequest().body(error);
//   }

//   @ExceptionHandler(HttpMessageNotReadableException.class)
//   public ResponseEntity<KvDtos.ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
//     log.warn("Message not readable: {}", ex.getMessage());

//     KvDtos.ErrorResponse error = new KvDtos.ErrorResponse(
//         "INVALID_JSON",
//         "Request body contains invalid JSON: " + ex.getMessage(),
//         Instant.now().toString()
//     );

//     return ResponseEntity.badRequest().body(error);
//   }

//   @ExceptionHandler(IllegalArgumentException.class)
//   public ResponseEntity<KvDtos.ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
//     log.warn("Illegal argument: {}", ex.getMessage());

//     KvDtos.ErrorResponse error = new KvDtos.ErrorResponse(
//         "ILLEGAL_ARGUMENT",
//         ex.getMessage(),
//         Instant.now().toString()
//     );

//     return ResponseEntity.badRequest().body(error);
//   }

//   @ExceptionHandler(IllegalStateException.class)
//   public ResponseEntity<KvDtos.ErrorResponse> handleIllegalState(IllegalStateException ex) {
//     log.warn("Illegal state: {}", ex.getMessage());

//     KvDtos.ErrorResponse error = new KvDtos.ErrorResponse(
//         "ILLEGAL_STATE",
//         ex.getMessage(),
//         Instant.now().toString()
//     );

//     return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
//   }

//   @ExceptionHandler(UnsupportedOperationException.class)
//   public ResponseEntity<KvDtos.ErrorResponse> handleUnsupportedOperation(UnsupportedOperationException ex) {
//     log.warn("Unsupported operation: {}", ex.getMessage());

//     KvDtos.ErrorResponse error = new KvDtos.ErrorResponse(
//         "UNSUPPORTED_OPERATION",
//         ex.getMessage(),
//         Instant.now().toString()
//     );

//     return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
//   }

//   @ExceptionHandler(Exception.class)
//   public ResponseEntity<KvDtos.ErrorResponse> handleGenericError(Exception ex) {
//     log.error("Unexpected error in KV Store", ex);

//     KvDtos.ErrorResponse error = new KvDtos.ErrorResponse(
//         "INTERNAL_ERROR",
//         "An unexpected error occurred: " + ex.getMessage(),
//         Instant.now().toString()
//     );

//     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
//   }
// }
