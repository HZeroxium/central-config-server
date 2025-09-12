package com.example.rest.user.mapper;

import com.example.rest.user.domain.User;
import com.example.rest.user.dto.UserRequest;
import com.example.rest.user.dto.UserResponse;

/** Utility mapping functions between REST DTOs and domain model. */
public final class UserMapper {
  private UserMapper() {}

  /**
   * Map {@link UserRequest} to domain {@link User}.
   *
   * @param req request payload
   * @param id optional identifier; when null the domain user will be created without id
   * @return domain user
   */
  public static User toDomainFromRequest(UserRequest req, String id) {
    return User.builder()
        .id(id)
        .name(req.getName())
        .phone(req.getPhone())
        .address(req.getAddress())
        .build();
  }

  /** Map domain {@link User} to API {@link UserResponse}. */
  public static UserResponse toResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .phone(user.getPhone())
        .address(user.getAddress())
        .build();
  }
}


