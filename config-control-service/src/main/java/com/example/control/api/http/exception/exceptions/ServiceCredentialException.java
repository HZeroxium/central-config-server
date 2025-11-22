package com.example.control.api.http.exception.exceptions;

/**
 * Base exception for service credential related errors.
 * <p>
 * All service credential exceptions extend this class for consistent error handling.
 * </p>
 */
public abstract class ServiceCredentialException extends ConfigControlException {

    protected ServiceCredentialException(String errorCode, String message) {
        super(errorCode, message);
    }

    protected ServiceCredentialException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

