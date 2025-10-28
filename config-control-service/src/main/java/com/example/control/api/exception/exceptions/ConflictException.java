package com.example.control.api.exception.exceptions;

/**
 * Conflict (HTTP 409) exception for business rule violations such as
 * duplicate pending requests or ownership conflicts during approval.
 */
public class ConflictException extends ConfigControlException {

  public ConflictException(String errorCode, String message) {
    super(errorCode, message);
  }

  public ConflictException(String message) {
    super("conflict", message);
  }
}


