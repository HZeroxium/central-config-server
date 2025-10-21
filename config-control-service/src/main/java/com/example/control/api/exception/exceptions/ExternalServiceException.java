package com.example.control.api.exception.exceptions;

/**
 * Exception thrown when external service calls fail.
 */
public class ExternalServiceException extends ConfigControlException {

  private final String serviceName;
  private final int statusCode;

  public ExternalServiceException(String serviceName, String message) {
    super("EXTERNAL_SERVICE_ERROR", "External service error: " + serviceName + " - " + message);
    this.serviceName = serviceName;
    this.statusCode = 0;
  }

  public ExternalServiceException(String serviceName, String message, int statusCode) {
    super("EXTERNAL_SERVICE_ERROR", "External service error: " + serviceName + " - " + message);
    this.serviceName = serviceName;
    this.statusCode = statusCode;
  }

  public ExternalServiceException(String serviceName, String message, Throwable cause) {
    super("EXTERNAL_SERVICE_ERROR", "External service error: " + serviceName + " - " + message, cause);
    this.serviceName = serviceName;
    this.statusCode = 0;
  }

  public ExternalServiceException(String serviceName, String message, int statusCode, Throwable cause) {
    super("EXTERNAL_SERVICE_ERROR", "External service error: " + serviceName + " - " + message, cause);
    this.serviceName = serviceName;
    this.statusCode = statusCode;
  }

  public String getServiceName() {
    return serviceName;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
