package com.example.rest.user.adapter.thrift;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.rest.user.domain.User;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.user.thrift.TUser;
import com.example.user.thrift.TPagedUsers;
import com.example.user.thrift.UserService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThriftUserClientAdapter implements ThriftUserClientPort {

  @Value("${thrift.host}")
  private String host;

  @Value("${thrift.port}")
  private int port;

  private UserService.Client client() throws Exception {
    log.debug("Creating Thrift client connection to {}:{}", host, port);
    TTransport transport = new TSocket(host, port, 5000);
    transport.open();
    TBinaryProtocol protocol = new TBinaryProtocol(transport);
    log.debug("Thrift client connection established successfully");
    return new UserService.Client(protocol);
  }

  private static TUser toThrift(User u) {
    return new TUser()
        .setId(u.getId())
        .setName(u.getName())
        .setPhone(u.getPhone())
        .setAddress(u.getAddress());
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
    log.debug("Pinging Thrift service at {}:{}", host, port);
    try {
      String response = client().ping();
      log.debug("Thrift service ping successful: {}", response);
      return response;
    } catch (Exception e) {
      log.error("Thrift service ping failed to {}:{}", host, port, e);
      throw new RuntimeException("Failed to ping Thrift service", e);
    }
  }

  @Override
  public User create(User user) {
    log.debug("Creating user via Thrift service: {}", user);
    try {
      TUser thriftUser = toThrift(user);
      log.debug("Mapped domain user to Thrift user: {}", thriftUser);
      
      TUser createdThrift = client().createUser(thriftUser);
      log.debug("User created via Thrift service: {}", createdThrift);
      
      User created = toDomain(createdThrift);
      log.debug("Mapped Thrift user to domain user: {}", created);
      return created;
    } catch (Exception e) {
      log.error("Failed to create user via Thrift service: {}", user, e);
      throw new RuntimeException("Failed to create user via Thrift service", e);
    }
  }

  @Override
  public Optional<User> getById(String id) {
    log.debug("Retrieving user by ID via Thrift service: {}", id);
    try {
      TUser thriftUser = client().getUser(id);
      if (thriftUser != null) {
        log.debug("User found via Thrift service: {}", thriftUser);
        User domainUser = toDomain(thriftUser);
        log.debug("Mapped Thrift user to domain user: {}", domainUser);
        return Optional.of(domainUser);
      } else {
        log.debug("User not found via Thrift service for ID: {}", id);
        return Optional.empty();
      }
    } catch (Exception e) {
      log.error("Failed to retrieve user via Thrift service for ID: {}", id, e);
      throw new RuntimeException("Failed to retrieve user via Thrift service", e);
    }
  }

  @Override
  public User update(User user) {
    log.debug("Updating user via Thrift service: {}", user);
    try {
      TUser thriftUser = toThrift(user);
      log.debug("Mapped domain user to Thrift user: {}", thriftUser);
      
      TUser updatedThrift = client().updateUser(thriftUser);
      log.debug("User updated via Thrift service: {}", updatedThrift);
      
      User updated = toDomain(updatedThrift);
      log.debug("Mapped Thrift user to domain user: {}", updated);
      return updated;
    } catch (Exception e) {
      log.error("Failed to update user via Thrift service: {}", user, e);
      throw new RuntimeException("Failed to update user via Thrift service", e);
    }
  }

  @Override
  public void delete(String id) {
    log.debug("Deleting user via Thrift service: {}", id);
    try {
      client().deleteUser(id);
      log.debug("User deleted successfully via Thrift service: {}", id);
    } catch (Exception e) {
      log.error("Failed to delete user via Thrift service for ID: {}", id, e);
      throw new RuntimeException("Failed to delete user via Thrift service", e);
    }
  }

  @Override
  public List<User> list() {
    log.debug("Listing all users via Thrift service");
    try {
      TPagedUsers paged = client().listUsers(0, Integer.MAX_VALUE);
      log.debug("Retrieved {} users via Thrift service", paged.getItems().size());
      
      List<User> users = paged.getItems().stream()
          .map(thriftUser -> {
            log.debug("Mapping Thrift user to domain user: {}", thriftUser);
            return toDomain(thriftUser);
          })
          .collect(Collectors.toList());
      
      log.debug("Mapped {} Thrift users to domain users", users.size());
      return users;
    } catch (Exception e) {
      log.error("Failed to list users via Thrift service", e);
      throw new RuntimeException("Failed to list users via Thrift service", e);
    }
  }

  @Override
  public List<User> listPaged(int page, int size) {
    log.debug("Listing users with pagination via Thrift service - page: {}, size: {}", page, size);
    try {
      TPagedUsers paged = client().listUsers(page, size);
      log.debug("Retrieved {} users via Thrift service for page: {}, size: {}", 
                paged.getItems().size(), page, size);
      
      List<User> users = paged.getItems().stream()
          .map(thriftUser -> {
            log.debug("Mapping Thrift user to domain user: {}", thriftUser);
            return toDomain(thriftUser);
          })
          .collect(Collectors.toList());
      
      log.debug("Mapped {} Thrift users to domain users for page: {}, size: {}", 
                users.size(), page, size);
      return users;
    } catch (Exception e) {
      log.error("Failed to list users with pagination via Thrift service - page: {}, size: {}", 
                page, size, e);
      throw new RuntimeException("Failed to list users via Thrift service", e);
    }
  }

  @Override
  public long count() {
    log.debug("Counting users via Thrift service");
    try {
      TPagedUsers paged = client().listUsers(0, 1);
      long count = paged.getTotal();
      log.debug("User count via Thrift service: {}", count);
      return count;
    } catch (Exception e) {
      log.error("Failed to count users via Thrift service", e);
      throw new RuntimeException("Failed to count users via Thrift service", e);
    }
  }
}
