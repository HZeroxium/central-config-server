package com.example.control.api.http.exception.exceptions;

/**
 * Exception thrown when service credentials exist but are not in ACTIVE status.
 */
public class ServiceCredentialNotActiveException extends ServiceCredentialException {

    public ServiceCredentialNotActiveException(String message) {
        super("SERVICE_CREDENTIAL_NOT_ACTIVE", message);
    }

    public ServiceCredentialNotActiveException(String message, Throwable cause) {
        super("SERVICE_CREDENTIAL_NOT_ACTIVE", message, cause);
    }
}

