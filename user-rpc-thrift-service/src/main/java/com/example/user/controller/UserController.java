package com.example.user.controller;

import com.example.user.domain.User;
import com.example.user.dto.UserPageResponse;
import com.example.user.dto.UserRequest;
import com.example.user.dto.UserResponse;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserServiceImpl;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST controller exposing CRUD endpoints to validate domain logic locally in the RPC
 * service. Returns {@link UserResponse} DTOs and accepts {@link UserRequest} payloads.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users", description = "Internal CRUD for RPC service")
@Validated
public class UserController {
  private final UserServiceImpl userService;

  /** Simple health endpoint. */
  @GetMapping("/ping")
  @Operation(summary = "Health ping")
  @ApiResponse(
      responseCode = "200",
      description = "OK",
      content = @Content(schema = @Schema(implementation = String.class)))
  public ResponseEntity<String> ping() {
    log.info("Health check ping requested for RPC service");
    try {
      String response = "pong";
      log.info("Health check ping successful for RPC service: {}", response);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Health check ping failed for RPC service", e);
      throw e;
    }
  }

  /** Create a new user. */
  @PostMapping
  @Operation(summary = "Create user")
  @ApiResponse(
      responseCode = "200",
      description = "Created",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  public ResponseEntity<UserResponse> create(@RequestBody @Validated UserRequest request) {
    log.info("Creating new user with name: {}", request.getName());
    try {
      var domainUser = UserMapper.toDomainFromRequest(request, null);
      log.debug("Mapped request to domain user: {}", domainUser);
      
      User created = userService.create(domainUser);
      log.info("User created successfully with ID: {}", created.getId());
      
      var response = UserMapper.toResponse(created);
      log.debug("Mapped domain user to response: {}", response);
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to create user with name: {}", request.getName(), e);
      throw e;
    }
  }

  /** Retrieve a user by id. */
  @GetMapping("/{id}")
  @Operation(summary = "Get user by id")
  @ApiResponse(
      responseCode = "200",
      description = "Found",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  @ApiResponse(responseCode = "404", description = "Not Found")
  public ResponseEntity<UserResponse> get(
      @PathVariable @Parameter(description = "User id") String id) {
    log.info("Retrieving user with ID: {}", id);
    try {
      return userService.getById(id)
          .map(user -> {
            log.debug("User found: {}", user);
            var response = UserMapper.toResponse(user);
            log.info("User retrieved successfully with ID: {}", id);
            return ResponseEntity.ok(response);
          })
          .orElseGet(() -> {
            log.warn("User not found with ID: {}", id);
            return ResponseEntity.notFound().build();
          });
    } catch (Exception e) {
      log.error("Failed to retrieve user with ID: {}", id, e);
      throw e;
    }
  }

  /** Update a user by id. */
  @PutMapping("/{id}")
  @Operation(summary = "Update user by id")
  @ApiResponse(
      responseCode = "200",
      description = "Updated",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  public ResponseEntity<UserResponse> update(
      @PathVariable @Parameter(description = "User id") String id,
      @RequestBody @Validated UserRequest request) {
    log.info("Updating user with ID: {} and name: {}", id, request.getName());
    try {
      var domainUser = UserMapper.toDomainFromRequest(request, id);
      log.debug("Mapped request to domain user: {}", domainUser);
      
      User updated = userService.update(domainUser);
      log.info("User updated successfully with ID: {}", id);
      
      var response = UserMapper.toResponse(updated);
      log.debug("Mapped domain user to response: {}", response);
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to update user with ID: {} and name: {}", id, request.getName(), e);
      throw e;
    }
  }

  /** Delete a user by id. */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete user by id")
  @ApiResponse(responseCode = "204", description = "Deleted")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    log.info("Deleting user with ID: {}", id);
    try {
      userService.delete(id);
      log.info("User deleted successfully with ID: {}", id);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      log.error("Failed to delete user with ID: {}", id, e);
      throw e;
    }
  }

  /** List users with pagination (default: page=0,size=20). */
  @GetMapping
  @Operation(summary = "List users with pagination")
  @ApiResponse(
      responseCode = "200",
      description = "OK",
      content = @Content(schema = @Schema(implementation = UserPageResponse.class)))
  public ResponseEntity<UserPageResponse> list(
      @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
    log.info("Listing users with pagination - page: {}, size: {}", page, size);
    try {
      var users = userService.listPaged(page, size);
      log.debug("Retrieved {} users from service", users.size());
      
      var total = userService.count();
      log.debug("Total user count: {}", total);
      
      var totalPages = (int) Math.ceil((double) total / (double) size);
      var items = users.stream().map(user -> {
        log.debug("Mapping user to response: {}", user);
        return UserMapper.toResponse(user);
      }).toList();
      
      var body = UserPageResponse.builder()
          .items(items)
          .page(page)
          .size(size)
          .total(total)
          .totalPages(totalPages)
          .build();
      
      log.info("Successfully listed {} users for page: {}, size: {}, total: {}, totalPages: {}", 
               items.size(), page, size, total, totalPages);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("Failed to list users with pagination - page: {}, size: {}", page, size, e);
      throw e;
    }
  }
}


