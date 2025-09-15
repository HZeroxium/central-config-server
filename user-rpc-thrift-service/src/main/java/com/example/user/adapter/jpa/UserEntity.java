package com.example.user.adapter.jpa;

import com.example.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapped to table {@code users} for H2/PostgreSQL/etc.
 * Acts as the persistence representation separate from the domain model.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
  /** Unique identifier, immutable once set. */
  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  /** Full name. */
  @Column(nullable = false, length = 100)
  private String name;

  /** Phone number. */
  @Column(nullable = false, length = 32)
  private String phone;

  /** Address. */
  @Column(length = 255)
  private String address;

  /** User status. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private User.UserStatus status;

  /** User role. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private User.UserRole role;

  /** Creation timestamp. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** User who created this user. */
  @Column(nullable = false, updatable = false, length = 50)
  private String createdBy;

  /** Last update timestamp. */
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /** User who last updated this user. */
  @Column(nullable = false, length = 50)
  private String updatedBy;

  /** Version for optimistic locking. */
  @Version
  @Column(nullable = false)
  private Integer version;

  /** Soft delete flag. */
  @Column(nullable = false)
  private Boolean deleted;

  /** Deletion timestamp. */
  private LocalDateTime deletedAt;

  /** User who deleted this user. */
  @Column(length = 50)
  private String deletedBy;
}


