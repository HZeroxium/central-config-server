package com.example.user.adapter.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


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
}


