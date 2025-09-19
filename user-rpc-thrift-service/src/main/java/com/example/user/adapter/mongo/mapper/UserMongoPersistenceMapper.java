package com.example.user.adapter.mongo.mapper;

import com.example.user.adapter.mongo.UserDocument;
import com.example.common.domain.User;
import java.util.UUID;

/** Manual mapper for converting between Mongo document and domain model. */
public final class UserMongoPersistenceMapper {
  private UserMongoPersistenceMapper() {}

  public static UserDocument toDocument(User user) {
    return UserDocument.builder()
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

  public static User toDomain(UserDocument doc) {
    return User.builder()
        .id(doc.getId())
        .name(doc.getName())
        .phone(doc.getPhone())
        .address(doc.getAddress())
        .status(doc.getStatus())
        .role(doc.getRole())
        .createdAt(doc.getCreatedAt())
        .createdBy(doc.getCreatedBy())
        .updatedAt(doc.getUpdatedAt())
        .updatedBy(doc.getUpdatedBy())
        .version(doc.getVersion())
        .deleted(doc.getDeleted())
        .deletedAt(doc.getDeletedAt())
        .deletedBy(doc.getDeletedBy())
        .build();
  }
}


