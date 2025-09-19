package com.example.common.exception;

import lombok.Getter;

/**
 * Base exception for all user service related errors.
 * Provides structured error handling with error codes and context.
 */
@Getter
public class UserServiceException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] messageArgs;
    private final String context;
    
    public UserServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.messageArgs = null;
        this.context = null;
    }
    
    public UserServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.messageArgs = null;
        this.context = null;
    }
    
    public UserServiceException(String errorCode, String message, Object[] messageArgs) {
        super(message);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
        this.context = null;
    }
    
    public UserServiceException(String errorCode, String message, Object[] messageArgs, String context) {
        super(message);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
        this.context = context;
    }
    
    public UserServiceException(String errorCode, String message, Object[] messageArgs, String context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
        this.context = context;
    }
}
