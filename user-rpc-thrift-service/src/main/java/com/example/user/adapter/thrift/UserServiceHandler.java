package com.example.user.adapter.thrift;

import com.example.user.domain.User;
import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.TUser;
import com.example.user.thrift.UserService;
import com.example.user.thrift.TPagedUsers;
import com.example.user.exception.DatabaseException;
import com.example.user.exception.UserServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;

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
  public String ping() throws TException {
    log.debug("Thrift ping request received");
    try {
      String response = userService.ping();
      log.debug("Thrift ping response: {}", response);
      return response;
    } catch (UserServiceException e) {
      log.error("Thrift ping failed: {}", e.getMessage(), e);
      throw new TException("Service ping failed: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during Thrift ping", e);
      throw new TException("Unexpected error during ping: " + e.getMessage(), e);
    }
  }

  @Override
  public TUser createUser(TUser user) throws TException {
    log.debug("Thrift createUser request received: {}", user);
    try {
      User domainUser = toDomain(user);
      log.debug("Mapped Thrift user to domain user: {}", domainUser);
      
      User created = userService.create(domainUser);
      log.info("User created via Thrift with ID: {}", created.getId());
      
      TUser thriftUser = toThrift(created);
      log.debug("Mapped domain user to Thrift user: {}", thriftUser);
      return thriftUser;
    } catch (DatabaseException e) {
      log.error("Database error during user creation: {}", e.getMessage(), e);
      throw new TException("Database error during user creation: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user creation: {}", e.getMessage(), e);
      throw new TException("Service error during user creation: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user creation: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user creation: " + e.getMessage(), e);
    }
  }

  @Override
  public TUser getUser(String id) throws TException {
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
    } catch (DatabaseException e) {
      log.error("Database error during user retrieval: {}", e.getMessage(), e);
      throw new TException("Database error during user retrieval: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user retrieval: {}", e.getMessage(), e);
      throw new TException("Service error during user retrieval: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user retrieval: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user retrieval: " + e.getMessage(), e);
    }
  }

  @Override
  public TUser updateUser(TUser user) throws TException {
    log.debug("Thrift updateUser request received: {}", user);
    try {
      User domainUser = toDomain(user);
      log.debug("Mapped Thrift user to domain user: {}", domainUser);
      
      User updated = userService.update(domainUser);
      log.info("User updated via Thrift with ID: {}", updated.getId());
      
      TUser thriftUser = toThrift(updated);
      log.debug("Mapped domain user to Thrift user: {}", thriftUser);
      return thriftUser;
    } catch (DatabaseException e) {
      log.error("Database error during user update: {}", e.getMessage(), e);
      throw new TException("Database error during user update: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user update: {}", e.getMessage(), e);
      throw new TException("Service error during user update: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user update: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user update: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteUser(String id) throws TException {
    log.debug("Thrift deleteUser request received for ID: {}", id);
    try {
      userService.delete(id);
      log.info("User deleted via Thrift with ID: {}", id);
    } catch (DatabaseException e) {
      log.error("Database error during user deletion: {}", e.getMessage(), e);
      throw new TException("Database error during user deletion: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user deletion: {}", e.getMessage(), e);
      throw new TException("Service error during user deletion: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user deletion: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user deletion: " + e.getMessage(), e);
    }
  }

  @Override
  public TPagedUsers listUsers(int page, int size) throws TException {
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
    } catch (DatabaseException e) {
      log.error("Database error during user listing: {}", e.getMessage(), e);
      throw new TException("Database error during user listing: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user listing: {}", e.getMessage(), e);
      throw new TException("Service error during user listing: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user listing: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user listing: " + e.getMessage(), e);
    }
  }
}
