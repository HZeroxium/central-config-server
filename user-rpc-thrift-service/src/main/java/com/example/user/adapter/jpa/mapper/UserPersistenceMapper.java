package com.example.user.adapter.jpa.mapper;

import com.example.user.adapter.jpa.UserEntity;
import com.example.user.domain.User;
import java.util.UUID;

/** Manual mapper for converting between JPA entity and domain model. */
public final class UserPersistenceMapper {
  private UserPersistenceMapper() {}

  public static UserEntity toEntity(User user) {
    return UserEntity.builder()
        .id(user.getId() != null && !user.getId().isBlank() ? user.getId() : UUID.randomUUID().toString())
        .name(user.getName())
        .phone(user.getPhone())
        .address(user.getAddress())
        .status(user.getStatus())
        .role(user.getRole())
        .createdAt(user.getCreatedAt())
        .createdBy(user.getCreatedBy())
        .updatedAt(user.getUpdatedAt())
        .updatedBy(user.getUpdatedBy())
        .version(user.getVersion())
        .deleted(user.getDeleted())
        .deletedAt(user.getDeletedAt())
        .deletedBy(user.getDeletedBy())
        .build();
  }

  public static User toDomain(UserEntity e) {
    return User.builder()
        .id(e.getId())
        .name(e.getName())
        .phone(e.getPhone())
        .address(e.getAddress())
        .status(e.getStatus())
        .role(e.getRole())
        .createdAt(e.getCreatedAt())
        .createdBy(e.getCreatedBy())
        .updatedAt(e.getUpdatedAt())
        .updatedBy(e.getUpdatedBy())
        .version(e.getVersion())
        .deleted(e.getDeleted())
        .deletedAt(e.getDeletedAt())
        .deletedBy(e.getDeletedBy())
        .build();
  }
}


