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

import com.example.rest.user.dto.UserRequest;
import com.example.rest.user.dto.UserResponse;
import com.example.rest.user.dto.UserPageResponse;
import com.example.rest.user.mapper.UserMapper;
import com.example.rest.user.service.UserService;

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
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  public ResponseEntity<UserResponse> create(@RequestBody @Validated UserRequest request) {
    log.info("Creating new user with name: {}", request.getName());
    
    var domainUser = UserMapper.toDomainFromRequest(request, null);
    log.debug("Mapped request to domain user: {}", domainUser);
    
    var created = userService.create(domainUser);
    log.info("User created successfully with ID: {}", created.getId());
    
    var response = UserMapper.toResponse(created);
    log.debug("Mapped domain user to response: {}", response);
    
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get user by id")
  @ApiResponse(
      responseCode = "200",
      description = "Found",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  @ApiResponse(responseCode = "404", description = "Not Found")
  public ResponseEntity<UserResponse> get(@PathVariable @Parameter(description = "User id") String id) {
    log.info("Retrieving user with ID: {}", id);
    
    return userService
        .getById(id)
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
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update user by id")
  @ApiResponse(
      responseCode = "200",
      description = "Updated",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  /** Update user via Thrift server. */
  public ResponseEntity<UserResponse> update(
      @PathVariable @Parameter(description = "User id") String id,
      @RequestBody @Validated UserRequest request) {
    log.info("Updating user with ID: {} and name: {}", id, request.getName());
    
    var domainUser = UserMapper.toDomainFromRequest(request, id);
    log.debug("Mapped request to domain user: {}", domainUser);
    
    var updated = userService.update(domainUser);
    log.info("User updated successfully with ID: {}", id);
    
    var response = UserMapper.toResponse(updated);
    log.debug("Mapped domain user to response: {}", response);
    
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete user by id")
  @ApiResponse(responseCode = "204", description = "Deleted")
  public ResponseEntity<Void> delete(@PathVariable @Parameter(description = "User id") String id) {
    log.info("Deleting user with ID: {}", id);
    
    userService.delete(id);
    log.info("User deleted successfully with ID: {}", id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  @Operation(summary = "List users with pagination")
  @ApiResponse(
      responseCode = "200",
      description = "OK",
      content = @Content(schema = @Schema(implementation = UserPageResponse.class)))
  /** List users via Thrift server with pagination support. */
  public ResponseEntity<UserPageResponse> list(
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
  }
}
