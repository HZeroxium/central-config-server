package com.example.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain aggregate representing a user in the system.
 * This model is independent of transport (REST/Thrift) and persistence (JPA/Mongo) concerns.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "User", description = "Domain user entity for RPC service")
public class User {
  @Schema(description = "Unique identifier", example = "u-1")
  private String id;

  @Schema(description = "Full name", example = "Alice Nguyen")
  @NotBlank
  @Size(max = 100)
  private String name;

  @Schema(description = "Phone number", example = "+84-912-345-678")
  @NotBlank
  @Size(max = 32)
  private String phone;

  @Schema(description = "Address", example = "123 Nguyen Trai, Ha Noi")
  @Size(max = 255)
  private String address;

  @Schema(description = "User status", example = "ACTIVE")
  private UserStatus status;

  @Schema(description = "User role", example = "USER")
  private UserRole role;

  @Schema(description = "Creation timestamp")
  private LocalDateTime createdAt;

  @Schema(description = "User who created this user", example = "admin")
  private String createdBy;

  @Schema(description = "Last update timestamp")
  private LocalDateTime updatedAt;

  @Schema(description = "User who last updated this user", example = "admin")
  private String updatedBy;

  @Schema(description = "Version for optimistic locking", example = "1")
  private Integer version;

  @Schema(description = "Soft delete flag", example = "false")
  private Boolean deleted;

  @Schema(description = "Deletion timestamp")
  private LocalDateTime deletedAt;

  @Schema(description = "User who deleted this user", example = "admin")
  private String deletedBy;

  // Enums
  public enum UserStatus {
    ACTIVE, INACTIVE, SUSPENDED
  }

  public enum UserRole {
    ADMIN, USER, MODERATOR, GUEST
  }
}
