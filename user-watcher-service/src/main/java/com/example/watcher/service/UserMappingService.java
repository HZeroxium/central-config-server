package com.example.watcher.service;

// Removed Avro imports - using Thrift only
import com.example.kafka.thrift.*;
import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserMappingService {

  // Removed Avro-based createUserFromRequest - use createUserFromThriftRequest
  // instead

  // Removed Avro-based createUserFromUpdateRequest - use
  // createUserFromThriftUpdateRequest instead

  // Removed Avro-based createUserResponse - use createThriftUserResponse instead

  // Removed Avro-based createCriteriaFromRequest - use
  // createCriteriaFromThriftRequest instead

  // Removed Avro enum conversion methods - using Thrift only

  // Thrift-specific methods
  public User createUserFromThriftRequest(TUserCreateRequest request) {
    return User.builder()
        .name(request.getName())
        .phone(request.getPhone())
        .address(request.getAddress())
        .status(convertThriftUserStatus(request.getStatus()))
        .role(convertThriftUserRole(request.getRole()))
        .build();
  }

  public User createUserFromThriftUpdateRequest(TUserUpdateRequest request) {
    return User.builder()
        .id(request.getId())
        .name(request.getName())
        .phone(request.getPhone())
        .address(request.getAddress())
        .status(convertThriftUserStatus(request.getStatus()))
        .role(convertThriftUserRole(request.getRole()))
        .version(request.getVersion())
        .build();
  }

  public TUserResponse createThriftUserResponse(User user) {
    TUserResponse response = new TUserResponse();
    response.setId(user.getId());
    response.setName(user.getName());
    response.setPhone(user.getPhone());
    response.setAddress(user.getAddress());
    response.setStatus(convertToThriftUserStatus(user.getStatus()));
    response.setRole(convertToThriftUserRole(user.getRole()));

    if (user.getCreatedAt() != null) {
      response.setCreatedAt(user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
    if (user.getCreatedBy() != null) {
      response.setCreatedBy(user.getCreatedBy());
    }
    if (user.getUpdatedAt() != null) {
      response.setUpdatedAt(user.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
    if (user.getUpdatedBy() != null) {
      response.setUpdatedBy(user.getUpdatedBy());
    }
    if (user.getVersion() != null) {
      response.setVersion(user.getVersion());
    }
    if (user.getDeleted() != null) {
      response.setDeleted(user.getDeleted());
    }
    if (user.getDeletedAt() != null) {
      response.setDeletedAt(user.getDeletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
    if (user.getDeletedBy() != null) {
      response.setDeletedBy(user.getDeletedBy());
    }

    return response;
  }

  public UserQueryCriteria createCriteriaFromThriftRequest(TUserListRequest request) {
    return UserQueryCriteria.builder()
        .page(request.isSetPage() ? request.getPage() : 0)
        .size(request.isSetSize() ? request.getSize() : 20)
        .search(request.isSetSearch() ? request.getSearch() : null)
        .status(request.isSetStatus() ? convertThriftUserStatus(request.getStatus()) : null)
        .role(request.isSetRole() ? convertThriftUserRole(request.getRole()) : null)
        .includeDeleted(request.isSetIncludeDeleted() ? request.isIncludeDeleted() : false)
        .build();
  }

  // Helper methods for Thrift enum conversion
  private User.UserStatus convertThriftUserStatus(TUserStatus status) {
    return User.UserStatus.valueOf(status.name());
  }

  private User.UserRole convertThriftUserRole(TUserRole role) {
    return User.UserRole.valueOf(role.name());
  }

  private TUserStatus convertToThriftUserStatus(User.UserStatus status) {
    return TUserStatus.valueOf(status.name());
  }

  private TUserRole convertToThriftUserRole(User.UserRole role) {
    return TUserRole.valueOf(role.name());
  }
}
