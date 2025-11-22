package com.example.control.api.http.exception.exceptions;

/**
 * Exception thrown when attempting to create credentials for a service that already has credentials.
 */
public class ServiceCredentialAlreadyExistsException extends ServiceCredentialException {

    public ServiceCredentialAlreadyExistsException(String message) {
        super("SERVICE_CREDENTIAL_ALREADY_EXISTS", message);
    }

    public ServiceCredentialAlreadyExistsException(String message, Throwable cause) {
        super("SERVICE_CREDENTIAL_ALREADY_EXISTS", message, cause);
    }
}

