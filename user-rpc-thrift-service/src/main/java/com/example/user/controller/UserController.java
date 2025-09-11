package com.example.user.controller;

import com.example.user.domain.User;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users", description = "Internal CRUD for RPC service")
@Validated
public class UserController {
  private final UserServiceImpl userService;

  @GetMapping("/ping")
  @Operation(summary = "Health ping")
  @ApiResponse(
      responseCode = "200",
      description = "OK",
      content = @Content(schema = @Schema(implementation = String.class)))
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok("pong");
  }

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

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete user by id")
  @ApiResponse(responseCode = "204", description = "Deleted")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  @Operation(summary = "List users")
  @ApiResponse(
      responseCode = "200",
      description = "OK",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  public ResponseEntity<List<UserResponse>> list() {
    List<UserResponse> result = userService.list().stream().map(UserMapper::toResponse).toList();
    return ResponseEntity.ok(result);
  }
}


