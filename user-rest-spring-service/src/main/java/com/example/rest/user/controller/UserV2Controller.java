package com.example.rest.user.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.rest.user.dto.*;
import com.example.rest.user.mapper.UserMapper;
import com.example.rest.user.service.UserService;
import com.example.rest.user.service.UserV2AsyncService;
import com.example.rest.user.constants.ApiConstants;
import com.example.rest.user.util.ResponseBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * V2 REST Controller for async user operations
 * Provides both sync (GET) and async (POST/PUT/DELETE) endpoints
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.USERS_V2_BASE_PATH)
@Tag(name = "Users V2", description = "Async User CRUD endpoints")
@Validated
public class UserV2Controller {

  private final UserService userService; // For sync operations
  private final UserV2AsyncService asyncService; // For async operations

  // Sync operations (same as V1 but under /v2/users)
  @GetMapping
  @Operation(summary = "List users (sync)", description = "Synchronous endpoint to list users with pagination and filtering")
  @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = ListUsersResponse.class)))
  @Timed(value = "rest.api.v2.list.users", description = "V2 List users endpoint timing")
  public ResponseEntity<ListUsersResponse> list(@Validated ListUsersRequest request) {
    log.info("V2 Listing users (sync) - page: {}, size: {}, search: {}",
        request.getPage(), request.getSize(), request.getSearch());

    var criteria = UserMapper.toQueryCriteria(request);
    var users = userService.listByCriteria(criteria);
    var total = userService.countByCriteria(criteria);
    var totalPages = (int) Math.ceil((double) total / (double) request.getSize());

    var items = users.stream().map(UserMapper::toResponse).toList();
    var response = ResponseBuilder.buildListUsersResponse(items, request.getPage(),
        request.getSize(), total, totalPages);

    log.info("V2 Listed {} users successfully", items.size());
    return ResponseEntity.ok(response);
  }

  @GetMapping(ApiConstants.USER_BY_ID_PATH)
  @Operation(summary = "Get user by ID (sync)", description = "Synchronous endpoint to retrieve a specific user")
  @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = GetUserResponse.class)))
  @ApiResponse(responseCode = "404", description = "User not found")
  @Timed(value = "rest.api.v2.get.user", description = "V2 Get user endpoint timing")
  public ResponseEntity<GetUserResponse> get(@PathVariable @Parameter(description = "User ID") String id) {
    log.info("V2 Retrieving user (sync) with ID: {}", id);

    return userService
        .getById(id)
        .map(user -> {
          var userResponse = UserMapper.toResponse(user);
          var response = ResponseBuilder.buildGetUserResponse(userResponse);
          log.info("V2 User retrieved successfully with ID: {}", id);
          return ResponseEntity.ok(response);
        })
        .orElseGet(() -> {
          log.warn("V2 User not found with ID: {}", id);
          var response = ResponseBuilder.buildUserNotFoundResponse();
          return ResponseEntity.status(StatusCode.getHttpStatus(StatusCode.USER_NOT_FOUND)).body(response);
        });
  }

  // Async operations (POST/PUT/DELETE return 202 Accepted)
  @PostMapping
  @Operation(summary = "Create user (async)", description = "Asynchronous endpoint to create a user. Returns operation ID for tracking.")
  @ApiResponse(responseCode = "202", description = "Request accepted for processing", content = @Content(schema = @Schema(implementation = AsyncOperationResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid request data")
  @Timed(value = "rest.api.v2.create.user.async", description = "V2 Async create user endpoint timing")
  public ResponseEntity<AsyncOperationResponse> createAsync(@RequestBody @Validated CreateUserRequest request) {
    log.info("V2 Creating user (async) with name: {}", request.getName());

    try {
      var operationId = asyncService.submitCreateCommand(request);
      var correlationId = UUID.randomUUID().toString();

      var response = AsyncOperationResponse.builder()
          .operationId(operationId)
          .status("PENDING")
          .message(ApiConstants.ASYNC_CREATE_SUBMITTED)
          .trackingUrl(ApiConstants.OPERATIONS_V2_BASE_PATH + "/" + operationId + "/status")
          .timestamp(LocalDateTime.now())
          .correlationId(correlationId)
          .build();

      log.info("V2 User creation command submitted with operation ID: {}", operationId);
      return ResponseEntity.accepted().body(response);
    } catch (Exception e) {
      log.error("V2 Failed to submit create user command", e);
      throw e;
    }
  }

  @PutMapping(ApiConstants.USER_BY_ID_PATH)
  @Operation(summary = "Update user (async)", description = "Asynchronous endpoint to update a user. Returns operation ID for tracking.")
  @ApiResponse(responseCode = "202", description = "Request accepted for processing", content = @Content(schema = @Schema(implementation = AsyncOperationResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid request data")
  @Timed(value = "rest.api.v2.update.user.async", description = "V2 Async update user endpoint timing")
  public ResponseEntity<AsyncOperationResponse> updateAsync(
      @PathVariable @Parameter(description = "User ID") String id,
      @RequestBody @Validated UpdateUserRequest request) {
    log.info("V2 Updating user (async) with ID: {}", id);

    try {
      var operationId = asyncService.submitUpdateCommand(id, request);
      var correlationId = UUID.randomUUID().toString();

      var response = AsyncOperationResponse.builder()
          .operationId(operationId)
          .status("PENDING")
          .message(ApiConstants.ASYNC_UPDATE_SUBMITTED)
          .trackingUrl(ApiConstants.OPERATIONS_V2_BASE_PATH + "/" + operationId + "/status")
          .timestamp(LocalDateTime.now())
          .correlationId(correlationId)
          .build();

      log.info("V2 User update command submitted with operation ID: {}", operationId);
      return ResponseEntity.accepted().body(response);
    } catch (Exception e) {
      log.error("V2 Failed to submit update user command for ID: {}", id, e);
      throw e;
    }
  }

  @DeleteMapping(ApiConstants.USER_BY_ID_PATH)
  @Operation(summary = "Delete user (async)", description = "Asynchronous endpoint to delete a user. Returns operation ID for tracking.")
  @ApiResponse(responseCode = "202", description = "Request accepted for processing", content = @Content(schema = @Schema(implementation = AsyncOperationResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid request data")
  @Timed(value = "rest.api.v2.delete.user.async", description = "V2 Async delete user endpoint timing")
  public ResponseEntity<AsyncOperationResponse> deleteAsync(
      @PathVariable @Parameter(description = "User ID") String id) {
    log.info("V2 Deleting user (async) with ID: {}", id);

    try {
      var operationId = asyncService.submitDeleteCommand(id);
      var correlationId = UUID.randomUUID().toString();

      var response = AsyncOperationResponse.builder()
          .operationId(operationId)
          .status("PENDING")
          .message(ApiConstants.ASYNC_DELETE_SUBMITTED)
          .trackingUrl(ApiConstants.OPERATIONS_V2_BASE_PATH + "/" + operationId + "/status")
          .timestamp(LocalDateTime.now())
          .correlationId(correlationId)
          .build();

      log.info("V2 User deletion command submitted with operation ID: {}", operationId);
      return ResponseEntity.accepted().body(response);
    } catch (Exception e) {
      log.error("V2 Failed to submit delete user command for ID: {}", id, e);
      throw e;
    }
  }
}
