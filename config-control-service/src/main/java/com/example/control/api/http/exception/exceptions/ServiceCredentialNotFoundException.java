package com.example.control.api.http.exception.exceptions;

/**
 * Exception thrown when service credentials are not found.
 */
public class ServiceCredentialNotFoundException extends ServiceCredentialException {

    public ServiceCredentialNotFoundException(String message) {
        super("SERVICE_CREDENTIAL_NOT_FOUND", message);
    }

    public ServiceCredentialNotFoundException(String message, Throwable cause) {
        super("SERVICE_CREDENTIAL_NOT_FOUND", message, cause);
    }
}

