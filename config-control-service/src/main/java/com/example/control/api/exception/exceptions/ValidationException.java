package com.example.control.api.exception.exceptions;

import com.example.control.api.exception.ErrorResponse;

import java.util.List;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends ConfigControlException {

  private final transient List<ErrorResponse.ValidationError> validationErrors;

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
