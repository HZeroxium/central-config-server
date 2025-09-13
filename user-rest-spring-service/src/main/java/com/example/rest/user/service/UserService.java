package com.example.rest.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.rest.user.domain.User;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.rest.exception.ThriftServiceException;
import com.example.rest.exception.UserNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
  private final ThriftUserClientPort thriftClient;

  public String ping() {
    log.debug("Pinging Thrift service");
    try {
      String response = thriftClient.ping();
      log.debug("Thrift service ping successful: {}", response);
      return response;
    } catch (Exception e) {
      log.error("Thrift service ping failed", e);
      throw new ThriftServiceException("Failed to ping Thrift service", "ping", e);
    }
  }

  public User create(User user) {
    log.debug("Creating user via Thrift client: {}", user);
    try {
      User created = thriftClient.create(user);
      log.debug("User created successfully via Thrift: {}", created);
      return created;
    } catch (Exception e) {
      log.error("Failed to create user via Thrift: {}", user, e);
      throw new ThriftServiceException("Failed to create user via Thrift service", "create", e);
    }
  }

  public Optional<User> getById(String id) {
    log.debug("Retrieving user by ID via Thrift client: {}", id);
    try {
      Optional<User> user = thriftClient.getById(id);
      if (user.isPresent()) {
        log.debug("User found via Thrift: {}", user.get());
      } else {
        log.debug("User not found via Thrift for ID: {}", id);
      }
      return user;
    } catch (Exception e) {
      log.error("Failed to retrieve user via Thrift for ID: {}", id, e);
      // Check if it's a user not found scenario
      if (e.getMessage() != null && e.getMessage().contains("not found")) {
        throw new UserNotFoundException(id, "Thrift service");
      }
      throw new ThriftServiceException("Failed to retrieve user via Thrift service", "getById", e);
    }
  }

  public User update(User user) {
    log.debug("Updating user via Thrift client: {}", user);
    try {
      User updated = thriftClient.update(user);
      log.debug("User updated successfully via Thrift: {}", updated);
      return updated;
    } catch (Exception e) {
      log.error("Failed to update user via Thrift: {}", user, e);
      throw new ThriftServiceException("Failed to update user via Thrift service", "update", e);
    }
  }

  public void delete(String id) {
    log.debug("Deleting user via Thrift client: {}", id);
    try {
      thriftClient.delete(id);
      log.debug("User deleted successfully via Thrift: {}", id);
    } catch (Exception e) {
      log.error("Failed to delete user via Thrift for ID: {}", id, e);
      throw new ThriftServiceException("Failed to delete user via Thrift service", "delete", e);
    }
  }

  public List<User> list() {
    log.debug("Listing all users via Thrift client");
    try {
      List<User> users = thriftClient.list();
      log.debug("Retrieved {} users via Thrift", users.size());
      return users;
    } catch (Exception e) {
      log.error("Failed to list users via Thrift", e);
      throw new ThriftServiceException("Failed to list users via Thrift service", "list", e);
    }
  }

  public List<User> listPaged(int page, int size) {
    log.debug("Listing users with pagination via Thrift client - page: {}, size: {}", page, size);
    try {
      List<User> users = thriftClient.listPaged(page, size);
      log.debug("Retrieved {} users via Thrift for page: {}, size: {}", users.size(), page, size);
      return users;
    } catch (Exception e) {
      log.error("Failed to list users with pagination via Thrift - page: {}, size: {}", page, size, e);
      throw new ThriftServiceException("Failed to list users with pagination via Thrift service", "listPaged", e);
    }
  }

  public long count() {
    log.debug("Counting users via Thrift client");
    try {
      long count = thriftClient.count();
      log.debug("User count via Thrift: {}", count);
      return count;
    } catch (Exception e) {
      log.error("Failed to count users via Thrift", e);
      throw new ThriftServiceException("Failed to count users via Thrift service", "count", e);
    }
  }
}
