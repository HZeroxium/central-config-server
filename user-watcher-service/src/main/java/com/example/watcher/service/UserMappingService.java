package com.example.watcher.service;

import com.example.kafka.avro.*;
import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserMappingService {

  public User createUserFromRequest(UserCreateRequest request) {
    return User.builder()
        .name(request.getName())
        .phone(request.getPhone())
        .address(request.getAddress())
        .status(convertUserStatus(request.getStatus()))
        .role(convertUserRole(request.getRole()))
        .build();
  }

  public User createUserFromUpdateRequest(UserUpdateRequest request) {
    return User.builder()
        .id(request.getId())
        .name(request.getName())
        .phone(request.getPhone())
        .address(request.getAddress())
        .status(convertUserStatus(request.getStatus()))
        .role(convertUserRole(request.getRole()))
        .version(request.getVersion())
        .build();
  }

  public UserResponse createUserResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getPhone(),
        user.getAddress(),
        convertToAvroUserStatus(user.getStatus()),
        convertToAvroUserRole(user.getRole()),
        user.getCreatedAt() != null
            ? user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            : null,
        user.getCreatedBy(),
        user.getUpdatedAt() != null
            ? user.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            : null,
        user.getUpdatedBy(),
        user.getVersion(),
        user.getDeleted(),
        user.getDeletedAt() != null
            ? user.getDeletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            : null,
        user.getDeletedBy());
  }

  public UserQueryCriteria createCriteriaFromRequest(UserListRequest request) {
    return UserQueryCriteria.builder()
        .page(request.getPage() != null ? request.getPage() : 0)
        .size(request.getSize() != null ? request.getSize() : 20)
        .search(request.getSearch())
        .status(request.getStatus() != null ? convertUserStatus(request.getStatus()) : null)
        .role(request.getRole() != null ? convertUserRole(request.getRole()) : null)
        .includeDeleted(request.getIncludeDeleted() != null ? request.getIncludeDeleted() : false)
        .build();
  }

  // Helper methods for enum conversion
  private User.UserStatus convertUserStatus(UserStatus status) {
    return User.UserStatus.valueOf(status.name());
  }

  private User.UserRole convertUserRole(UserRole role) {
    return User.UserRole.valueOf(role.name());
  }

  private UserStatus convertToAvroUserStatus(User.UserStatus status) {
    return UserStatus.valueOf(status.name());
  }

  private UserRole convertToAvroUserRole(User.UserRole role) {
    return UserRole.valueOf(role.name());
  }
}
