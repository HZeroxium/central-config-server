package com.example.rest.user.constants;

/**
 * Constants for API configuration.
 */
public final class ApiConstants {

  // API paths
  public static final String USERS_BASE_PATH = "/users";
  public static final String PING_PATH = "/ping";
  public static final String USER_BY_ID_PATH = "/{id}";

  // Default values
  public static final String DEFAULT_CREATED_BY = "admin";
  public static final String DEFAULT_UPDATED_BY = "admin";
  public static final int DEFAULT_VERSION = 1;

  // Messages
  public static final String USER_CREATED_SUCCESSFULLY = "User created successfully";
  public static final String USER_RETRIEVED_SUCCESSFULLY = "User retrieved successfully";
  public static final String USER_UPDATED_SUCCESSFULLY = "User updated successfully";
  public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";
  public static final String USERS_RETRIEVED_SUCCESSFULLY = "Users retrieved successfully";
  public static final String USER_NOT_FOUND_MESSAGE = "User not found";

  private ApiConstants() {
    // Utility class
  }
}
