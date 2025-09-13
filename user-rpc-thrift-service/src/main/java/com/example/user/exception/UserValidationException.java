package com.example.user.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when user data validation fails.
 */
public class UserValidationException extends UserServiceException {
    
    private static final String ERROR_CODE = "USER_VALIDATION_FAILED";
    private final Map<String, List<String>> validationErrors;
    
    public UserValidationException(String message) {
        super(ERROR_CODE, message);
        this.validationErrors = null;
    }
    
    public UserValidationException(String message, Map<String, List<String>> validationErrors) {
        super(ERROR_CODE, message);
        this.validationErrors = validationErrors;
    }
    
    public UserValidationException(String message, Map<String, List<String>> validationErrors, String context) {
        super(ERROR_CODE, message, new Object[]{validationErrors}, context);
        this.validationErrors = validationErrors;
    }
    
    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }
}
