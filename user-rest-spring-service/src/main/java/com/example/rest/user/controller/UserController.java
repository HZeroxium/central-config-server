package com.example.rest.user.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.rest.user.dto.*;
import com.example.rest.user.mapper.UserMapper;
import com.example.rest.user.service.UserService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST facade acting as a Thrift client. Accepts {@link com.example.rest.user.dto.UserRequest}
 * and returns {@link com.example.rest.user.dto.UserResponse}, while delegating to the underlying
 * Thrift server through the application service.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users", description = "User CRUD endpoints")
@Validated
public class UserController {
  private final UserService userService;

  @GetMapping("/ping")
  @Operation(summary = "Health ping", description = "Reach underlying Thrift service")
  @ApiResponse(
      responseCode = "200",
      description = "OK",
      content = @Content(schema = @Schema(implementation = String.class)))
  public ResponseEntity<String> ping() {
    log.info("Health check ping requested");
    String response = userService.ping();
    log.info("Health check ping successful, response: {}", response);
    return ResponseEntity.ok(response);
  }

  /** Create user via Thrift server. */
  @PostMapping
  @Operation(summary = "Create user")
  @ApiResponse(
      responseCode = "200",
      description = "Created",
      content = @Content(schema = @Schema(implementation = CreateUserResponse.class)))
  public ResponseEntity<CreateUserResponse> create(@RequestBody @Validated CreateUserRequest request) {
    log.info("Creating new user with name: {}", request.getName());
    
    var domainUser = UserMapper.toDomainFromCreateRequest(request);
    log.debug("Mapped request to domain user: {}", domainUser);
    
    var created = userService.create(domainUser);
    log.info("User created successfully with ID: {}", created.getId());
    
    var userResponse = UserMapper.toResponse(created);
    var response = CreateUserResponse.builder()
        .status(StatusCode.SUCCESS)
        .message("User created successfully")
        .user(userResponse)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
    
    log.debug("Mapped domain user to response: {}", response);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get user by id")
  @ApiResponse(
      responseCode = "200",
      description = "Found",
      content = @Content(schema = @Schema(implementation = GetUserResponse.class)))
  @ApiResponse(responseCode = "404", description = "Not Found")
  public ResponseEntity<GetUserResponse> get(@PathVariable @Parameter(description = "User id") String id) {
    log.info("Retrieving user with ID: {}", id);
    
    return userService
        .getById(id)
        .map(user -> {
          log.debug("User found: {}", user);
          var userResponse = UserMapper.toResponse(user);
          var response = GetUserResponse.builder()
              .status(StatusCode.SUCCESS)
              .message("User retrieved successfully")
              .user(userResponse)
              .timestamp(LocalDateTime.now())
              .correlationId(UUID.randomUUID().toString())
              .build();
          log.info("User retrieved successfully with ID: {}", id);
          return ResponseEntity.ok(response);
        })
        .orElseGet(() -> {
          log.warn("User not found with ID: {}", id);
          var response = GetUserResponse.builder()
              .status(StatusCode.USER_NOT_FOUND)
              .message("User not found")
              .user(null)
              .timestamp(LocalDateTime.now())
              .correlationId(UUID.randomUUID().toString())
              .build();
          return ResponseEntity.status(StatusCode.getHttpStatus(StatusCode.USER_NOT_FOUND)).body(response);
        });
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update user by id")
  @ApiResponse(
      responseCode = "200",
      description = "Updated",
      content = @Content(schema = @Schema(implementation = UpdateUserResponse.class)))
  @ApiResponse(responseCode = "404", description = "Not Found")
  /** Update user via Thrift server. */
  public ResponseEntity<UpdateUserResponse> update(
      @PathVariable @Parameter(description = "User id") String id,
      @RequestBody @Validated UpdateUserRequest request) {
    log.info("Updating user with ID: {} and name: {}", id, request.getName());
    
    // Check if user exists first
    var existingUser = userService.getById(id);
    if (existingUser.isEmpty()) {
      log.warn("User not found for update with ID: {}", id);
      var response = UpdateUserResponse.builder()
          .status(StatusCode.USER_NOT_FOUND)
          .message("User not found")
          .user(null)
          .timestamp(LocalDateTime.now())
          .correlationId(UUID.randomUUID().toString())
          .build();
      return ResponseEntity.status(StatusCode.getHttpStatus(StatusCode.USER_NOT_FOUND)).body(response);
    }
    
    var domainUser = UserMapper.toDomainFromUpdateRequest(request);
    log.debug("Mapped request to domain user: {}", domainUser);
    
    var updated = userService.update(domainUser);
    log.info("User updated successfully with ID: {}", id);
    
    var userResponse = UserMapper.toResponse(updated);
    var response = UpdateUserResponse.builder()
        .status(StatusCode.SUCCESS)
        .message("User updated successfully")
        .user(userResponse)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
    
    log.debug("Mapped domain user to response: {}", response);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete user by id")
  @ApiResponse(
      responseCode = "200",
      description = "Deleted",
      content = @Content(schema = @Schema(implementation = DeleteUserResponse.class)))
  @ApiResponse(responseCode = "404", description = "Not Found")
  public ResponseEntity<DeleteUserResponse> delete(@PathVariable @Parameter(description = "User id") String id) {
    log.info("Deleting user with ID: {}", id);
    
    // Check if user exists first
    var existingUser = userService.getById(id);
    if (existingUser.isEmpty()) {
      log.warn("User not found for deletion with ID: {}", id);
      var response = DeleteUserResponse.builder()
          .status(StatusCode.USER_NOT_FOUND)
          .message("User not found")
          .timestamp(LocalDateTime.now())
          .correlationId(UUID.randomUUID().toString())
          .build();
      return ResponseEntity.status(StatusCode.getHttpStatus(StatusCode.USER_NOT_FOUND)).body(response);
    }
    
    userService.delete(id);
    log.info("User deleted successfully with ID: {}", id);
    
    var response = DeleteUserResponse.builder()
        .status(StatusCode.SUCCESS)
        .message("User deleted successfully")
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
    
    return ResponseEntity.ok(response);
  }

  @GetMapping
  @Operation(summary = "List users with pagination")
  @ApiResponse(
      responseCode = "200",
      description = "OK",
      content = @Content(schema = @Schema(implementation = ListUsersResponse.class)))
  /** List users via Thrift server with pagination support. */
  public ResponseEntity<ListUsersResponse> list(
      @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
    log.info("Listing users with pagination - page: {}, size: {}", page, size);
    
    var users = userService.listPaged(page, size);
    var total = userService.count();
    var totalPages = (int) Math.ceil((double) total / (double) size);
    log.debug("Retrieved {} users from service, total: {}", users.size(), total);
    
    var items = users.stream().map(user -> {
      log.debug("Mapping user to response: {}", user);
      return UserMapper.toResponse(user);
    }).toList();
    
    var response = ListUsersResponse.builder()
        .status(StatusCode.SUCCESS)
        .message("Users retrieved successfully")
        .items(items)
        .page(page)
        .size(size)
        .total(total)
        .totalPages(totalPages)
        .timestamp(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();
    
    log.info("Successfully listed {} users for page: {}, size: {}, total: {}, totalPages: {}",
             items.size(), page, size, total, totalPages);
    return ResponseEntity.ok(response);
  }
}
