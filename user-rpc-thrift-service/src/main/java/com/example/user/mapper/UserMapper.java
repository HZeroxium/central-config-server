package com.example.user.mapper;

import com.example.user.domain.User;
import com.example.user.dto.UserRequest;
import com.example.user.dto.UserResponse;

public final class UserMapper {
  private UserMapper() {}

  public static User toDomainFromRequest(UserRequest req, String id) {
    return User.builder().id(id).name(req.getName()).phone(req.getPhone()).address(req.getAddress()).build();
  }

  public static UserResponse toResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .phone(user.getPhone())
        .address(user.getAddress())
        .build();
  }
}


