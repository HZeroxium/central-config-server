package com.example.user.exception;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends UserServiceException {
    
    private static final String ERROR_CODE = "USER_NOT_FOUND";
    
    public UserNotFoundException(String userId) {
        super(ERROR_CODE, String.format("User with ID '%s' not found", userId), new Object[]{userId});
    }
    
    public UserNotFoundException(String userId, String context) {
        super(ERROR_CODE, String.format("User with ID '%s' not found in context: %s", userId, context), 
              new Object[]{userId, context}, context);
    }
}
