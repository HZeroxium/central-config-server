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
import com.example.user.thrift.*;
import com.example.rest.exception.UserNotFoundException;
import com.example.rest.exception.ThriftServiceException;

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
      TPingResponse response = client().ping();
      if (response.getStatus() == 0) {
        log.debug("Thrift service ping successful: {}", response.getResponse());
        return response.getResponse();
      } else {
        log.error("Thrift service ping failed with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Thrift service ping failed: " + response.getMessage(), "ping");
      }
    } catch (Exception e) {
      log.error("Thrift service ping failed to {}:{}", host, port, e);
      throw new ThriftServiceException("Failed to ping Thrift service", "ping", e);
    }
  }

  @Override
  public User create(User user) {
    log.debug("Creating user via Thrift service: {}", user);
    try {
      TCreateUserRequest request = new TCreateUserRequest()
          .setName(user.getName())
          .setPhone(user.getPhone())
          .setAddress(user.getAddress());
      
      TCreateUserResponse response = client().createUser(request);
      if (response.getStatus() == 0) {
        log.debug("User created via Thrift service: {}", response.getUser());
        User created = toDomain(response.getUser());
        log.debug("Mapped Thrift user to domain user: {}", created);
        return created;
      } else {
        log.error("Failed to create user via Thrift service with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Failed to create user: " + response.getMessage(), "create");
      }
    } catch (Exception e) {
      log.error("Failed to create user via Thrift service: {}", user, e);
      throw new ThriftServiceException("Failed to create user via Thrift service", "create", e);
    }
  }

  @Override
  public Optional<User> getById(String id) {
    log.debug("Retrieving user by ID via Thrift service: {}", id);
    try {
      TGetUserRequest request = new TGetUserRequest().setId(id);
      TGetUserResponse response = client().getUser(request);
      
      if (response.getStatus() == 0) {
        log.debug("User found via Thrift service: {}", response.getUser());
        User domainUser = toDomain(response.getUser());
        log.debug("Mapped Thrift user to domain user: {}", domainUser);
        return Optional.of(domainUser);
      } else if (response.getStatus() == 2) { // USER_NOT_FOUND
        log.debug("User not found via Thrift service for ID: {}", id);
        return Optional.empty();
      } else {
        log.error("Failed to retrieve user via Thrift service with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Failed to retrieve user: " + response.getMessage(), "getById");
      }
    } catch (Exception e) {
      log.error("Failed to retrieve user via Thrift service for ID: {}", id, e);
      throw new ThriftServiceException("Failed to retrieve user via Thrift service", "getById", e);
    }
  }

  @Override
  public User update(User user) {
    log.debug("Updating user via Thrift service: {}", user);
    try {
      TUpdateUserRequest request = new TUpdateUserRequest()
          .setId(user.getId())
          .setName(user.getName())
          .setPhone(user.getPhone())
          .setAddress(user.getAddress());
      
      TUpdateUserResponse response = client().updateUser(request);
      if (response.getStatus() == 0) {
        log.debug("User updated via Thrift service: {}", response.getUser());
        User updated = toDomain(response.getUser());
        log.debug("Mapped Thrift user to domain user: {}", updated);
        return updated;
      } else if (response.getStatus() == 1) { // USER_NOT_FOUND
        log.error("User not found for update via Thrift service: {}", user.getId());
        throw new UserNotFoundException(user.getId(), "Thrift service");
      } else {
        log.error("Failed to update user via Thrift service with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Failed to update user: " + response.getMessage(), "update");
      }
    } catch (Exception e) {
      log.error("Failed to update user via Thrift service: {}", user, e);
      throw new ThriftServiceException("Failed to update user via Thrift service", "update", e);
    }
  }

  @Override
  public void delete(String id) {
    log.debug("Deleting user via Thrift service: {}", id);
    try {
      TDeleteUserRequest request = new TDeleteUserRequest().setId(id);
      TDeleteUserResponse response = client().deleteUser(request);
      
      if (response.getStatus() == 0) {
        log.debug("User deleted successfully via Thrift service: {}", id);
      } else if (response.getStatus() == 1) { // USER_NOT_FOUND
        log.error("User not found for deletion via Thrift service: {}", id);
        throw new UserNotFoundException(id, "Thrift service");
      } else {
        log.error("Failed to delete user via Thrift service with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Failed to delete user: " + response.getMessage(), "delete");
      }
    } catch (Exception e) {
      log.error("Failed to delete user via Thrift service for ID: {}", id, e);
      throw new ThriftServiceException("Failed to delete user via Thrift service", "delete", e);
    }
  }

  @Override
  public List<User> list() {
    log.debug("Listing all users via Thrift service");
    try {
      TListUsersRequest request = new TListUsersRequest()
          .setPage(0)
          .setSize(Integer.MAX_VALUE);
      
      TListUsersResponse response = client().listUsers(request);
      if (response.getStatus() == 0) {
        log.debug("Retrieved {} users via Thrift service", response.getItems().size());
        
        List<User> users = response.getItems().stream()
            .map(thriftUser -> {
              log.debug("Mapping Thrift user to domain user: {}", thriftUser);
              return toDomain(thriftUser);
            })
            .collect(Collectors.toList());
        
        log.debug("Mapped {} Thrift users to domain users", users.size());
        return users;
      } else {
        log.error("Failed to list users via Thrift service with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Failed to list users: " + response.getMessage(), "list");
      }
    } catch (Exception e) {
      log.error("Failed to list users via Thrift service", e);
      throw new ThriftServiceException("Failed to list users via Thrift service", "list", e);
    }
  }

  @Override
  public List<User> listPaged(int page, int size) {
    log.debug("Listing users with pagination via Thrift service - page: {}, size: {}", page, size);
    try {
      TListUsersRequest request = new TListUsersRequest()
          .setPage(page)
          .setSize(size);
      
      TListUsersResponse response = client().listUsers(request);
      if (response.getStatus() == 0) {
        log.debug("Retrieved {} users via Thrift service for page: {}, size: {}", 
                  response.getItems().size(), page, size);
        
        List<User> users = response.getItems().stream()
            .map(thriftUser -> {
              log.debug("Mapping Thrift user to domain user: {}", thriftUser);
              return toDomain(thriftUser);
            })
            .collect(Collectors.toList());
        
        log.debug("Mapped {} Thrift users to domain users for page: {}, size: {}", 
                  users.size(), page, size);
        return users;
      } else {
        log.error("Failed to list users via Thrift service with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Failed to list users: " + response.getMessage(), "listPaged");
      }
    } catch (Exception e) {
      log.error("Failed to list users with pagination via Thrift service - page: {}, size: {}", 
                page, size, e);
      throw new ThriftServiceException("Failed to list users via Thrift service", "listPaged", e);
    }
  }

  @Override
  public long count() {
    log.debug("Counting users via Thrift service");
    try {
      TListUsersRequest request = new TListUsersRequest()
          .setPage(0)
          .setSize(1);
      
      TListUsersResponse response = client().listUsers(request);
      if (response.getStatus() == 0) {
        long count = response.getTotal();
        log.debug("User count via Thrift service: {}", count);
        return count;
      } else {
        log.error("Failed to count users via Thrift service with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Failed to count users: " + response.getMessage(), "count");
      }
    } catch (Exception e) {
      log.error("Failed to count users via Thrift service", e);
      throw new ThriftServiceException("Failed to count users via Thrift service", "count", e);
    }
  }
}
