package com.example.control.api.exception;

/**
 * Exception thrown when there are configuration-related errors.
 */
public class ConfigurationException extends ConfigControlException {

  public ConfigurationException(String message) {
    super("CONFIGURATION_ERROR", message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super("CONFIGURATION_ERROR", message, cause);
  }

  public ConfigurationException(String serviceName, String profile, String message) {
    super("CONFIGURATION_ERROR", "Configuration error for service " + serviceName +
        " (profile: " + profile + "): " + message);
  }
}
