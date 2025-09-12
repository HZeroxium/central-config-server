package com.example.user.adapter.thrift;

import com.example.user.domain.User;
import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.TUser;
import com.example.user.thrift.UserService;
import com.example.user.thrift.TPagedUsers;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrift service handler bridging Thrift-generated API to domain service port.
 * Performs translation between {@link TUser} wire model and domain {@link com.example.user.domain.User}.
 */
@Slf4j
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
    log.debug("Thrift ping request received");
    try {
      String response = userService.ping();
      log.debug("Thrift ping response: {}", response);
      return response;
    } catch (Exception e) {
      log.error("Thrift ping failed", e);
      throw e;
    }
  }

  @Override
  public TUser createUser(TUser user) {
    log.debug("Thrift createUser request received: {}", user);
    try {
      User domainUser = toDomain(user);
      log.debug("Mapped Thrift user to domain user: {}", domainUser);
      
      User created = userService.create(domainUser);
      log.info("User created via Thrift with ID: {}", created.getId());
      
      TUser thriftUser = toThrift(created);
      log.debug("Mapped domain user to Thrift user: {}", thriftUser);
      return thriftUser;
    } catch (Exception e) {
      log.error("Thrift createUser failed: {}", user, e);
      throw e;
    }
  }

  @Override
  public TUser getUser(String id) {
    log.debug("Thrift getUser request received for ID: {}", id);
    try {
      return userService.getById(id)
          .map(user -> {
            log.debug("User found via Thrift: {}", user);
            TUser thriftUser = toThrift(user);
            log.debug("Mapped domain user to Thrift user: {}", thriftUser);
            return thriftUser;
          })
          .orElseGet(() -> {
            log.debug("User not found via Thrift for ID: {}", id);
            return null;
          });
    } catch (Exception e) {
      log.error("Thrift getUser failed for ID: {}", id, e);
      throw e;
    }
  }

  @Override
  public TUser updateUser(TUser user) {
    log.debug("Thrift updateUser request received: {}", user);
    try {
      User domainUser = toDomain(user);
      log.debug("Mapped Thrift user to domain user: {}", domainUser);
      
      User updated = userService.update(domainUser);
      log.info("User updated via Thrift with ID: {}", updated.getId());
      
      TUser thriftUser = toThrift(updated);
      log.debug("Mapped domain user to Thrift user: {}", thriftUser);
      return thriftUser;
    } catch (Exception e) {
      log.error("Thrift updateUser failed: {}", user, e);
      throw e;
    }
  }

  @Override
  public void deleteUser(String id) {
    log.debug("Thrift deleteUser request received for ID: {}", id);
    try {
      userService.delete(id);
      log.info("User deleted via Thrift with ID: {}", id);
    } catch (Exception e) {
      log.error("Thrift deleteUser failed for ID: {}", id, e);
      throw e;
    }
  }

  @Override
  public TPagedUsers listUsers(int page, int size) {
    log.debug("Thrift listUsers request received - page: {}, size: {}", page, size);
    try {
      List<User> users = userService.listPaged(page, size);
      log.debug("Retrieved {} users from service for Thrift", users.size());
      
      long total = userService.count();
      log.debug("Total user count for Thrift: {}", total);
      
      int totalPages = (int) Math.ceil((double) total / (double) size);
      List<TUser> thriftUsers = users.stream()
          .map(user -> {
            log.debug("Mapping domain user to Thrift user: {}", user);
            return toThrift(user);
          })
          .collect(Collectors.toList());
      
      TPagedUsers res = new TPagedUsers();
      res.setItems(thriftUsers);
      res.setPage(page);
      res.setSize(size);
      res.setTotal(total);
      res.setTotalPages(totalPages);
      
      log.debug("Thrift listUsers response - items: {}, page: {}, size: {}, total: {}, totalPages: {}", 
                thriftUsers.size(), page, size, total, totalPages);
      return res;
    } catch (Exception e) {
      log.error("Thrift listUsers failed - page: {}, size: {}", page, size, e);
      throw e;
    }
  }
}
