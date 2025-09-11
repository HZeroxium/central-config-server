package com.example.user.adapter.thrift;

import com.example.user.domain.User;
import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.TUser;
import com.example.user.thrift.UserService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class UserServiceHandler implements UserService.Iface {

  private final UserServicePort userService;

  private static TUser toThrift(User user) {
    TUser t = new TUser();
    t.setId(user.getId());
    t.setName(user.getName());
    t.setPhone(user.getPhone());
    t.setAddress(user.getAddress());
    return t;
  }

  private static User toDomain(TUser t) {
    return User.builder()
        .id(t.getId())
        .name(t.getName())
        .phone(t.getPhone())
        .address(t.getAddress())
        .build();
  }

  @Override
  public String ping() {
    return userService.ping();
  }

  @Override
  public TUser createUser(TUser user) {
    return toThrift(userService.create(toDomain(user)));
  }

  @Override
  public TUser getUser(String id) {
    return userService.getById(id).map(UserServiceHandler::toThrift).orElse(null);
  }

  @Override
  public TUser updateUser(TUser user) {
    return toThrift(userService.update(toDomain(user)));
  }

  @Override
  public void deleteUser(String id) {
    userService.delete(id);
  }

  @Override
  public List<TUser> listUsers() {
    return userService.list().stream().map(UserServiceHandler::toThrift).collect(Collectors.toList());
  }
}
