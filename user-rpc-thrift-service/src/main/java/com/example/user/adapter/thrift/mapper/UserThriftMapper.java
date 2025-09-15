package com.example.user.adapter.thrift.mapper;

import com.example.user.domain.User;
import com.example.user.domain.UserQueryCriteria;
import com.example.user.thrift.TListUsersRequest;
import com.example.user.thrift.TUser;
import com.example.user.thrift.TUserRole;
import com.example.user.thrift.TUserStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for converting between Thrift DTOs and domain models.
 * Pure functions only; no side effects.
 */
public final class UserThriftMapper {
  private UserThriftMapper() {}

  /** Convert domain user to Thrift user. */
  public static TUser toThrift(User user) {
    TUser t = new TUser();
    t.setId(user.getId());
    t.setName(user.getName());
    t.setPhone(user.getPhone());
    t.setAddress(user.getAddress());
    t.setStatus(user.getStatus() != null ? TUserStatus.valueOf(user.getStatus().name()) : TUserStatus.ACTIVE);
    t.setRole(user.getRole() != null ? TUserRole.valueOf(user.getRole().name()) : TUserRole.USER);
    t.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli() : 0);
    t.setCreatedBy(user.getCreatedBy());
    t.setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli() : 0);
    t.setUpdatedBy(user.getUpdatedBy());
    t.setVersion(user.getVersion() != null ? user.getVersion() : 1);
    t.setDeleted(user.getDeleted() != null ? user.getDeleted() : false);
    t.setDeletedAt(user.getDeletedAt() != null ? user.getDeletedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli() : 0);
    t.setDeletedBy(user.getDeletedBy());
    return t;
  }

  /** Convert Thrift user to domain user. */
  public static User toDomain(TUser t) {
    return User.builder()
        .id(t.getId())
        .name(t.getName())
        .phone(t.getPhone())
        .address(t.getAddress())
        .status(t.getStatus() != null ? User.UserStatus.valueOf(t.getStatus().name()) : User.UserStatus.ACTIVE)
        .role(t.getRole() != null ? User.UserRole.valueOf(t.getRole().name()) : User.UserRole.USER)
        .createdAt(t.getCreatedAt() > 0 ? LocalDateTime.ofEpochSecond(t.getCreatedAt() / 1000, 0, ZoneOffset.UTC) : null)
        .createdBy(t.getCreatedBy())
        .updatedAt(t.getUpdatedAt() > 0 ? LocalDateTime.ofEpochSecond(t.getUpdatedAt() / 1000, 0, ZoneOffset.UTC) : null)
        .updatedBy(t.getUpdatedBy())
        .version(t.getVersion())
        .deleted(t.isDeleted())
        .deletedAt(t.getDeletedAt() > 0 ? LocalDateTime.ofEpochSecond(t.getDeletedAt() / 1000, 0, ZoneOffset.UTC) : null)
        .deletedBy(t.getDeletedBy())
        .build();
  }

  /** Convert Thrift list request to domain query criteria. */
  public static UserQueryCriteria toQueryCriteria(TListUsersRequest request) {
    List<com.example.user.domain.SortCriterion> sortCriteria = null;
    if (request.getSortCriteria() != null && !request.getSortCriteria().isEmpty()) {
      sortCriteria = request.getSortCriteria().stream()
          .map(sc -> com.example.user.domain.SortCriterion.builder()
              .fieldName(sc.getFieldName())
              .direction(sc.getDirection())
              .build())
          .collect(Collectors.toList());
    }

    return UserQueryCriteria.builder()
        .page(request.getPage() > 0 ? request.getPage() : 0)
        .size(request.getSize() > 0 ? request.getSize() : 20)
        .search(request.getSearch())
        .status(request.getStatus() != null ? User.UserStatus.valueOf(request.getStatus().name()) : null)
        .role(request.getRole() != null ? User.UserRole.valueOf(request.getRole().name()) : null)
        .includeDeleted(request.isIncludeDeleted())
        .createdAfter(request.getCreatedAfter() > 0 ? LocalDateTime.ofEpochSecond(request.getCreatedAfter() / 1000, 0, ZoneOffset.UTC) : null)
        .createdBefore(request.getCreatedBefore() > 0 ? LocalDateTime.ofEpochSecond(request.getCreatedBefore() / 1000, 0, ZoneOffset.UTC) : null)
        .sortCriteria(sortCriteria)
        .build();
  }
}


