package com.example.control.consulclient.exception;

/**
 * Base exception for Consul SDK operations.
 */
public class ConsulException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] args;
    
    public ConsulException(String message) {
        super(message);
        this.errorCode = null;
        this.args = null;
    }
    
    public ConsulException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.args = null;
    }
    
    public ConsulException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }
    
    public ConsulException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public ConsulException(String errorCode, String message, Throwable cause, Object... args) {
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
