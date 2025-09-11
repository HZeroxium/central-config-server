package com.example.user.adapter.thrift;

import com.example.user.domain.User;
import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.TUser;
import com.example.user.thrift.UserService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrift service handler bridging Thrift-generated API to domain service port.
 * Performs translation between {@link TUser} wire model and domain {@link com.example.user.domain.User}.
 */
public class UserServiceHandler implements UserService.Iface {

  private final UserServicePort userService;

  /**
   * Construct handler with a domain service port.
   *
   * @param userService domain service to delegate business logic
   */
  public UserServiceHandler(UserServicePort userService) {
    this.userService = userService;
  }

  /** Convert domain to Thrift DTO. */
  private static TUser toThrift(User user) {
    TUser t = new TUser();
    t.setId(user.getId());
    t.setName(user.getName());
    t.setPhone(user.getPhone());
    t.setAddress(user.getAddress());
    return t;
  }

  /** Convert Thrift DTO to domain. */
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
