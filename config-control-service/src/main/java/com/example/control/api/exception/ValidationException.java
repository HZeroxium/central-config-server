package com.example.control.api.exception;

import java.util.List;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends ConfigControlException {

  private final List<ErrorResponse.ValidationError> validationErrors;

  public ValidationException(String message) {
    super("VALIDATION_ERROR", message);
    this.validationErrors = null;
  }

  public ValidationException(String message, List<ErrorResponse.ValidationError> validationErrors) {
    super("VALIDATION_ERROR", message);
    this.validationErrors = validationErrors;
  }

  public ValidationException(String message, Throwable cause) {
    super("VALIDATION_ERROR", message, cause);
    this.validationErrors = null;
  }

  public List<ErrorResponse.ValidationError> getValidationErrors() {
    return validationErrors;
  }
}
