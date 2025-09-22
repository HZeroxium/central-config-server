package com.example.rest.user.constants;

/**
 * Constants for API configuration.
 */
public final class ApiConstants {

  // API paths
  public static final String USERS_BASE_PATH = "/users";
  public static final String USERS_V2_BASE_PATH = "/v2/users";
  public static final String OPERATIONS_V2_BASE_PATH = "/v2/operations";
  public static final String PING_PATH = "/ping";
  public static final String USER_BY_ID_PATH = "/{id}";
  public static final String OPERATION_STATUS_PATH = "/{operationId}/status";

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

  // V2 Async Messages
  public static final String ASYNC_CREATE_SUBMITTED = "User creation request submitted";
  public static final String ASYNC_UPDATE_SUBMITTED = "User update request submitted";
  public static final String ASYNC_DELETE_SUBMITTED = "User deletion request submitted";
  public static final String OPERATION_NOT_FOUND = "Operation not found";
  public static final String OPERATION_STATUS_RETRIEVED = "Operation status retrieved successfully";

  private ApiConstants() {
    // Utility class
  }
}
