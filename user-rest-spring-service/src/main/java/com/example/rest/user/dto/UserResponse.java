package com.example.rest.user.dto;

import com.example.common.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/** Response view returned by the REST facade. */
@Value
@Builder
@Schema(name = "UserResponse", description = "User view returned by the API")
public class UserResponse {
  @Schema(description = "Unique identifier", example = "u-1")
  String id;

  @Schema(description = "Full name", example = "Alice Nguyen")
  String name;

  @Schema(description = "Phone number", example = "+84-912-345-678")
  String phone;

  @Schema(description = "Address", example = "123 Nguyen Trai, Ha Noi")
  String address;

  @Schema(description = "User status", example = "ACTIVE")
  User.UserStatus status;

  @Schema(description = "User role", example = "USER")
  User.UserRole role;

  @Schema(description = "Creation timestamp")
  LocalDateTime createdAt;

  @Schema(description = "User who created this user", example = "admin")
  String createdBy;

  @Schema(description = "Last update timestamp")
  LocalDateTime updatedAt;

  @Schema(description = "User who last updated this user", example = "admin")
  String updatedBy;

  @Schema(description = "Version for optimistic locking", example = "1")
  Integer version;
}
