package com.example.control.api.exception.exceptions;

/**
 * Exception thrown when there are configuration-related errors.
 */
public class ConfigurationException extends ConfigControlException {

  private static final String ERROR_CODE = "CONFIGURATION_ERROR";

  public ConfigurationException(String message) {
    super(ERROR_CODE, message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }

  public ConfigurationException(String serviceName, String profile, String message) {
    super(ERROR_CODE, "Configuration error for service " + serviceName +
        " (profile: " + profile + "): " + message);
  }
}
