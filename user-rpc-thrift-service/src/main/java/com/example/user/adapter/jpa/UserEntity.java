package com.example.user.adapter.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 32)
  private String phone;

  @Column(length = 255)
  private String address;
}


