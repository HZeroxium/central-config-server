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
    return ResponseEntity.ok("pong");
  }

  /** Create a new user. */
  @PostMapping
  @Operation(summary = "Create user")
  @ApiResponse(
      responseCode = "200",
      description = "Created",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  public ResponseEntity<UserResponse> create(@RequestBody @Validated UserRequest request) {
    User created = userService.create(UserMapper.toDomainFromRequest(request, null));
    return ResponseEntity.ok(UserMapper.toResponse(created));
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
    return userService.getById(id).map(UserMapper::toResponse).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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
    User updated = userService.update(UserMapper.toDomainFromRequest(request, id));
    return ResponseEntity.ok(UserMapper.toResponse(updated));
  }

  /** Delete a user by id. */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete user by id")
  @ApiResponse(responseCode = "204", description = "Deleted")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
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
    var users = userService.listPaged(page, size);
    var total = userService.count();
    var totalPages = (int) Math.ceil((double) total / (double) size);
    var items = users.stream().map(UserMapper::toResponse).toList();
    var body = UserPageResponse.builder()
        .items(items)
        .page(page)
        .size(size)
        .total(total)
        .totalPages(totalPages)
        .build();
    return ResponseEntity.ok(body);
  }
}


