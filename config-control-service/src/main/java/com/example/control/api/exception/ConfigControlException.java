package com.example.control.api.exception;

/**
 * Base exception for Config Control Service.
 */
public class ConfigControlException extends RuntimeException {

  private final String errorCode;
  private final Object[] args;

  public ConfigControlException(String message) {
    super(message);
    this.errorCode = null;
    this.args = null;
  }

  public ConfigControlException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = null;
    this.args = null;
  }

  public ConfigControlException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
    this.args = null;
  }

  public ConfigControlException(String errorCode, String message, Object... args) {
    super(message);
    this.errorCode = errorCode;
    this.args = args;
  }

  public ConfigControlException(String errorCode, String message, Throwable cause, Object... args) {
    super(message, cause);
    this.errorCode = errorCode;
    this.args = args;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public Object[] getArgs() {
    return args;
  }
}
