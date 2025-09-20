package com.example.rest.user.util;

import com.example.rest.user.dto.*;
import com.example.rest.user.constants.ApiConstants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for building API responses.
 */
public final class ResponseBuilder {

  private ResponseBuilder() {
    // Utility class
  }

  public static CreateUserResponse buildCreateUserResponse(UserResponse user) {
    return CreateUserResponse.builder()
        .status(StatusCode.SUCCESS)
        .message(ApiConstants.USER_CREATED_SUCCESSFULLY)
        .user(user)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
  }

  public static GetUserResponse buildGetUserResponse(UserResponse user) {
    return GetUserResponse.builder()
        .status(StatusCode.SUCCESS)
        .message(ApiConstants.USER_RETRIEVED_SUCCESSFULLY)
        .user(user)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
  }

  public static GetUserResponse buildUserNotFoundResponse() {
    return GetUserResponse.builder()
        .status(StatusCode.USER_NOT_FOUND)
        .message(ApiConstants.USER_NOT_FOUND_MESSAGE)
        .user(null)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
  }

  public static UpdateUserResponse buildUpdateUserResponse(UserResponse user) {
    return UpdateUserResponse.builder()
        .status(StatusCode.SUCCESS)
        .message(ApiConstants.USER_UPDATED_SUCCESSFULLY)
        .user(user)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
  }

  public static UpdateUserResponse buildUserNotFoundForUpdateResponse() {
    return UpdateUserResponse.builder()
        .status(StatusCode.USER_NOT_FOUND)
        .message(ApiConstants.USER_NOT_FOUND_MESSAGE)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
  }

  public static DeleteUserResponse buildDeleteUserResponse() {
    return DeleteUserResponse.builder()
        .status(StatusCode.SUCCESS)
        .message(ApiConstants.USER_DELETED_SUCCESSFULLY)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
  }

  public static DeleteUserResponse buildUserNotFoundForDeleteResponse() {
    return DeleteUserResponse.builder()
        .status(StatusCode.USER_NOT_FOUND)
        .message(ApiConstants.USER_NOT_FOUND_MESSAGE)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
  }

  public static ListUsersResponse buildListUsersResponse(List<UserResponse> items,
      int page,
      int size,
      long total,
      int totalPages) {
    return ListUsersResponse.builder()
        .status(StatusCode.SUCCESS)
        .message(ApiConstants.USERS_RETRIEVED_SUCCESSFULLY)
        .items(items)
        .page(page)
        .size(size)
        .total(total)
        .totalPages(totalPages)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
  }
}
