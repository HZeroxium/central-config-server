package com.example.kafka.mapping;

import com.example.kafka.avro.*;
import com.example.kafka.thrift.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Avro and Thrift objects during migration
 * This will help maintain compatibility during the transition period
 */
@Slf4j
@Component
public class AvroToThriftMapper {

  // User Status conversion
  public com.example.kafka.thrift.TUserStatus toThriftUserStatus(com.example.kafka.avro.UserStatus avroStatus) {
    if (avroStatus == null)
      return null;
    return switch (avroStatus) {
      case ACTIVE -> com.example.kafka.thrift.TUserStatus.ACTIVE;
      case INACTIVE -> com.example.kafka.thrift.TUserStatus.INACTIVE;
      case SUSPENDED -> com.example.kafka.thrift.TUserStatus.SUSPENDED;
    };
  }

  public com.example.kafka.avro.UserStatus toAvroUserStatus(com.example.kafka.thrift.TUserStatus thriftStatus) {
    if (thriftStatus == null)
      return null;
    return switch (thriftStatus) {
      case ACTIVE -> com.example.kafka.avro.UserStatus.ACTIVE;
      case INACTIVE -> com.example.kafka.avro.UserStatus.INACTIVE;
      case SUSPENDED -> com.example.kafka.avro.UserStatus.SUSPENDED;
    };
  }

  // User Role conversion
  public com.example.kafka.thrift.TUserRole toThriftUserRole(com.example.kafka.avro.UserRole avroRole) {
    if (avroRole == null)
      return null;
    return switch (avroRole) {
      case ADMIN -> com.example.kafka.thrift.TUserRole.ADMIN;
      case USER -> com.example.kafka.thrift.TUserRole.USER;
      case MODERATOR -> com.example.kafka.thrift.TUserRole.MODERATOR;
      case GUEST -> com.example.kafka.thrift.TUserRole.GUEST;
    };
  }

  public com.example.kafka.avro.UserRole toAvroUserRole(com.example.kafka.thrift.TUserRole thriftRole) {
    if (thriftRole == null)
      return null;
    return switch (thriftRole) {
      case ADMIN -> com.example.kafka.avro.UserRole.ADMIN;
      case USER -> com.example.kafka.avro.UserRole.USER;
      case MODERATOR -> com.example.kafka.avro.UserRole.MODERATOR;
      case GUEST -> com.example.kafka.avro.UserRole.GUEST;
    };
  }

  // User Response conversion
  public TUserResponse toThriftUserResponse(UserResponse avroUser) {
    if (avroUser == null)
      return null;

    TUserResponse thriftUser = new TUserResponse();
    thriftUser.setId(avroUser.getId());
    thriftUser.setName(avroUser.getName());
    thriftUser.setPhone(avroUser.getPhone());
    thriftUser.setAddress(avroUser.getAddress());
    thriftUser.setStatus(toThriftUserStatus(avroUser.getStatus()));
    thriftUser.setRole(toThriftUserRole(avroUser.getRole()));

    if (avroUser.getCreatedAt() != null) {
      thriftUser.setCreatedAt(avroUser.getCreatedAt());
    }
    if (avroUser.getCreatedBy() != null) {
      thriftUser.setCreatedBy(avroUser.getCreatedBy());
    }
    if (avroUser.getUpdatedAt() != null) {
      thriftUser.setUpdatedAt(avroUser.getUpdatedAt());
    }
    if (avroUser.getUpdatedBy() != null) {
      thriftUser.setUpdatedBy(avroUser.getUpdatedBy());
    }
    if (avroUser.getVersion() != null) {
      thriftUser.setVersion(avroUser.getVersion());
    }
    if (avroUser.getDeleted() != null) {
      thriftUser.setDeleted(avroUser.getDeleted());
    }
    if (avroUser.getDeletedAt() != null) {
      thriftUser.setDeletedAt(avroUser.getDeletedAt());
    }
    if (avroUser.getDeletedBy() != null) {
      thriftUser.setDeletedBy(avroUser.getDeletedBy());
    }

    return thriftUser;
  }

  public UserResponse toAvroUserResponse(TUserResponse thriftUser) {
    if (thriftUser == null)
      return null;

    UserResponse.Builder builder = UserResponse.newBuilder()
        .setId(thriftUser.getId())
        .setName(thriftUser.getName())
        .setPhone(thriftUser.getPhone())
        .setAddress(thriftUser.getAddress())
        .setStatus(toAvroUserStatus(thriftUser.getStatus()))
        .setRole(toAvroUserRole(thriftUser.getRole()));

    if (thriftUser.isSetCreatedAt()) {
      builder.setCreatedAt(thriftUser.getCreatedAt());
    }
    if (thriftUser.isSetCreatedBy()) {
      builder.setCreatedBy(thriftUser.getCreatedBy());
    }
    if (thriftUser.isSetUpdatedAt()) {
      builder.setUpdatedAt(thriftUser.getUpdatedAt());
    }
    if (thriftUser.isSetUpdatedBy()) {
      builder.setUpdatedBy(thriftUser.getUpdatedBy());
    }
    if (thriftUser.isSetVersion()) {
      builder.setVersion(thriftUser.getVersion());
    }
    if (thriftUser.isSetDeleted()) {
      builder.setDeleted(thriftUser.isDeleted());
    }
    if (thriftUser.isSetDeletedAt()) {
      builder.setDeletedAt(thriftUser.getDeletedAt());
    }
    if (thriftUser.isSetDeletedBy()) {
      builder.setDeletedBy(thriftUser.getDeletedBy());
    }

    return builder.build();
  }

  // Create Request conversion
  public TUserCreateRequest toThriftCreateRequest(UserCreateRequest avroRequest) {
    if (avroRequest == null)
      return null;

    TUserCreateRequest thriftRequest = new TUserCreateRequest();
    thriftRequest.setName(avroRequest.getName());
    thriftRequest.setPhone(avroRequest.getPhone());
    thriftRequest.setAddress(avroRequest.getAddress());
    thriftRequest.setStatus(toThriftUserStatus(avroRequest.getStatus()));
    thriftRequest.setRole(toThriftUserRole(avroRequest.getRole()));

    return thriftRequest;
  }

  public UserCreateRequest toAvroCreateRequest(TUserCreateRequest thriftRequest) {
    if (thriftRequest == null)
      return null;

    return UserCreateRequest.newBuilder()
        .setName(thriftRequest.getName())
        .setPhone(thriftRequest.getPhone())
        .setAddress(thriftRequest.getAddress())
        .setStatus(toAvroUserStatus(thriftRequest.getStatus()))
        .setRole(toAvroUserRole(thriftRequest.getRole()))
        .build();
  }

  // Create Response conversion
  public TUserCreateResponse toThriftCreateResponse(UserCreateResponse avroResponse) {
    if (avroResponse == null)
      return null;

    TUserCreateResponse thriftResponse = new TUserCreateResponse();
    thriftResponse.setUser(toThriftUserResponse(avroResponse.getUser()));

    return thriftResponse;
  }

  public UserCreateResponse toAvroCreateResponse(TUserCreateResponse thriftResponse) {
    if (thriftResponse == null)
      return null;

    return UserCreateResponse.newBuilder()
        .setUser(toAvroUserResponse(thriftResponse.getUser()))
        .build();
  }

  // Get Request conversion
  public TUserGetRequest toThriftGetRequest(UserGetRequest avroRequest) {
    if (avroRequest == null)
      return null;

    TUserGetRequest thriftRequest = new TUserGetRequest();
    thriftRequest.setId(avroRequest.getId());

    return thriftRequest;
  }

  public UserGetRequest toAvroGetRequest(TUserGetRequest thriftRequest) {
    if (thriftRequest == null)
      return null;

    return UserGetRequest.newBuilder()
        .setId(thriftRequest.getId())
        .build();
  }

  // Get Response conversion
  public TUserGetResponse toThriftGetResponse(UserGetResponse avroResponse) {
    if (avroResponse == null)
      return null;

    TUserGetResponse thriftResponse = new TUserGetResponse();
    if (avroResponse.getUser() != null) {
      thriftResponse.setUser(toThriftUserResponse(avroResponse.getUser()));
    }
    thriftResponse.setFound(avroResponse.getFound());

    return thriftResponse;
  }

  public UserGetResponse toAvroGetResponse(TUserGetResponse thriftResponse) {
    if (thriftResponse == null)
      return null;

    UserGetResponse.Builder builder = UserGetResponse.newBuilder()
        .setFound(thriftResponse.isFound());

    if (thriftResponse.isSetUser()) {
      builder.setUser(toAvroUserResponse(thriftResponse.getUser()));
    }

    return builder.build();
  }

  // Ping conversion
  public TPingRequest toThriftPingRequest(com.example.kafka.avro.PingRequest avroRequest) {
    if (avroRequest == null)
      return null;

    TPingRequest thriftRequest = new TPingRequest();
    thriftRequest.setMessage("ping"); // Default message for Thrift

    return thriftRequest;
  }

  public com.example.kafka.avro.PingRequest toAvroPingRequest(TPingRequest thriftRequest) {
    if (thriftRequest == null)
      return null;

    return com.example.kafka.avro.PingRequest.newBuilder()
        .setTimestamp(System.currentTimeMillis()) // Set current timestamp
        .build();
  }

  public TPingResponse toThriftPingResponse(com.example.kafka.avro.PingResponse avroResponse) {
    if (avroResponse == null)
      return null;

    TPingResponse thriftResponse = new TPingResponse();
    thriftResponse.setMessage(avroResponse.getMessage());

    return thriftResponse;
  }

  public com.example.kafka.avro.PingResponse toAvroPingResponse(TPingResponse thriftResponse) {
    if (thriftResponse == null)
      return null;

    return com.example.kafka.avro.PingResponse.newBuilder()
        .setMessage(thriftResponse.getMessage())
        .build();
  }

  // Update Request conversion
  public TUserUpdateRequest toThriftUpdateRequest(com.example.kafka.avro.UserUpdateRequest avroRequest) {
    if (avroRequest == null)
      return null;

    TUserUpdateRequest thriftRequest = new TUserUpdateRequest();
    thriftRequest.setId(avroRequest.getId());
    thriftRequest.setName(avroRequest.getName());
    thriftRequest.setPhone(avroRequest.getPhone());
    thriftRequest.setAddress(avroRequest.getAddress());
    thriftRequest.setStatus(toThriftUserStatus(avroRequest.getStatus()));
    thriftRequest.setRole(toThriftUserRole(avroRequest.getRole()));
    thriftRequest.setVersion(avroRequest.getVersion());

    return thriftRequest;
  }

  public com.example.kafka.avro.UserUpdateRequest toAvroUpdateRequest(TUserUpdateRequest thriftRequest) {
    if (thriftRequest == null)
      return null;

    return com.example.kafka.avro.UserUpdateRequest.newBuilder()
        .setId(thriftRequest.getId())
        .setName(thriftRequest.getName())
        .setPhone(thriftRequest.getPhone())
        .setAddress(thriftRequest.getAddress())
        .setStatus(toAvroUserStatus(thriftRequest.getStatus()))
        .setRole(toAvroUserRole(thriftRequest.getRole()))
        .setVersion(thriftRequest.getVersion())
        .build();
  }

  // List Request conversion
  public TUserListRequest toThriftListRequest(com.example.kafka.avro.UserListRequest avroRequest) {
    if (avroRequest == null)
      return null;

    TUserListRequest thriftRequest = new TUserListRequest();
    if (avroRequest.getPage() != null) {
      thriftRequest.setPage(avroRequest.getPage());
    }
    if (avroRequest.getSize() != null) {
      thriftRequest.setSize(avroRequest.getSize());
    }
    if (avroRequest.getSearch() != null) {
      thriftRequest.setSearch(avroRequest.getSearch());
    }
    if (avroRequest.getStatus() != null) {
      thriftRequest.setStatus(toThriftUserStatus(avroRequest.getStatus()));
    }
    if (avroRequest.getRole() != null) {
      thriftRequest.setRole(toThriftUserRole(avroRequest.getRole()));
    }
    if (avroRequest.getIncludeDeleted() != null) {
      thriftRequest.setIncludeDeleted(avroRequest.getIncludeDeleted());
    }
    // Note: Avro UserListRequest doesn't have createdAfter/createdBefore fields
    // These are available in Thrift but not in the original Avro schema

    return thriftRequest;
  }

  public com.example.kafka.avro.UserListRequest toAvroListRequest(TUserListRequest thriftRequest) {
    if (thriftRequest == null)
      return null;

    com.example.kafka.avro.UserListRequest.Builder builder = com.example.kafka.avro.UserListRequest.newBuilder();

    if (thriftRequest.isSetPage()) {
      builder.setPage(thriftRequest.getPage());
    }
    if (thriftRequest.isSetSize()) {
      builder.setSize(thriftRequest.getSize());
    }
    if (thriftRequest.isSetSearch()) {
      builder.setSearch(thriftRequest.getSearch());
    }
    if (thriftRequest.isSetStatus()) {
      builder.setStatus(toAvroUserStatus(thriftRequest.getStatus()));
    }
    if (thriftRequest.isSetRole()) {
      builder.setRole(toAvroUserRole(thriftRequest.getRole()));
    }
    if (thriftRequest.isSetIncludeDeleted()) {
      builder.setIncludeDeleted(thriftRequest.isIncludeDeleted());
    }
    // Note: Avro UserListRequest doesn't have createdAfter/createdBefore fields
    // These extra Thrift fields are ignored when converting to Avro

    return builder.build();
  }
}
