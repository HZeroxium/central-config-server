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
import com.example.rest.metrics.ApplicationMetrics;
import io.micrometer.core.annotation.Timed;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
  private final ThriftUserClientPort thriftClient;
  private final ApplicationMetrics metrics;

  @Timed(value = "service.rest.ping", description = "Time taken to ping Thrift service")
  public String ping() {
    log.debug("Pinging Thrift service");
    
    var timer = metrics.startThriftClientTimer();
    try {
      metrics.incrementThriftClientRequests("ping");
      
      String response = thriftClient.ping();
      log.debug("Thrift service ping successful: {}", response);
      return response;
    } catch (Exception e) {
      metrics.incrementThriftClientErrors("ping", e.getClass().getSimpleName());
      log.error("Thrift service ping failed", e);
      throw new ThriftServiceException("Failed to ping Thrift service", "ping", e);
    } finally {
      metrics.recordThriftClientDuration(timer, "ping");
    }
  }

  @Timed(value = "service.rest.create", description = "Time taken to create user via Thrift")
  public User create(User user) {
    log.debug("Creating user via Thrift client: {}", user);
    
    var timer = metrics.startThriftClientTimer();
    try {
      metrics.incrementThriftClientRequests("create");
      
      User created = thriftClient.create(user);
      log.debug("User created successfully via Thrift: {}", created);
      return created;
    } catch (Exception e) {
      metrics.incrementThriftClientErrors("create", e.getClass().getSimpleName());
      log.error("Failed to create user via Thrift: {}", user, e);
      throw new ThriftServiceException("Failed to create user via Thrift service", "create", e);
    } finally {
      metrics.recordThriftClientDuration(timer, "create");
    }
  }

  @Timed(value = "service.rest.get.by.id", description = "Time taken to get user by ID via Thrift")
  public Optional<User> getById(String id) {
    log.debug("Retrieving user by ID via Thrift client: {}", id);
    
    var timer = metrics.startThriftClientTimer();
    try {
      metrics.incrementThriftClientRequests("getById");
      
      Optional<User> user = thriftClient.getById(id);
      if (user.isPresent()) {
        log.debug("User found via Thrift: {}", user.get());
      } else {
        log.debug("User not found via Thrift for ID: {}", id);
      }
      return user;
    } catch (Exception e) {
      metrics.incrementThriftClientErrors("getById", e.getClass().getSimpleName());
      log.error("Failed to retrieve user via Thrift for ID: {}", id, e);
      // Check if it's a user not found scenario
      if (e.getMessage() != null && e.getMessage().contains("not found")) {
        throw new UserNotFoundException(id, "Thrift service");
      }
      throw new ThriftServiceException("Failed to retrieve user via Thrift service", "getById", e);
    } finally {
      metrics.recordThriftClientDuration(timer, "getById");
    }
  }

  @Timed(value = "service.rest.update", description = "Time taken to update user via Thrift")
  public User update(User user) {
    log.debug("Updating user via Thrift client: {}", user);
    
    var timer = metrics.startThriftClientTimer();
    try {
      metrics.incrementThriftClientRequests("update");
      
      User updated = thriftClient.update(user);
      log.debug("User updated successfully via Thrift: {}", updated);
      return updated;
    } catch (Exception e) {
      metrics.incrementThriftClientErrors("update", e.getClass().getSimpleName());
      log.error("Failed to update user via Thrift: {}", user, e);
      throw new ThriftServiceException("Failed to update user via Thrift service", "update", e);
    } finally {
      metrics.recordThriftClientDuration(timer, "update");
    }
  }

  @Timed(value = "service.rest.delete", description = "Time taken to delete user via Thrift")
  public void delete(String id) {
    log.debug("Deleting user via Thrift client: {}", id);
    
    var timer = metrics.startThriftClientTimer();
    try {
      metrics.incrementThriftClientRequests("delete");
      
      thriftClient.delete(id);
      log.debug("User deleted successfully via Thrift: {}", id);
    } catch (Exception e) {
      metrics.incrementThriftClientErrors("delete", e.getClass().getSimpleName());
      log.error("Failed to delete user via Thrift for ID: {}", id, e);
      throw new ThriftServiceException("Failed to delete user via Thrift service", "delete", e);
    } finally {
      metrics.recordThriftClientDuration(timer, "delete");
    }
  }

  @Timed(value = "service.rest.list", description = "Time taken to list all users via Thrift")
  public List<User> list() {
    log.debug("Listing all users via Thrift client");
    
    var timer = metrics.startThriftClientTimer();
    try {
      metrics.incrementThriftClientRequests("list");
      
      List<User> users = thriftClient.list();
      log.debug("Retrieved {} users via Thrift", users.size());
      return users;
    } catch (Exception e) {
      metrics.incrementThriftClientErrors("list", e.getClass().getSimpleName());
      log.error("Failed to list users via Thrift", e);
      throw new ThriftServiceException("Failed to list users via Thrift service", "list", e);
    } finally {
      metrics.recordThriftClientDuration(timer, "list");
    }
  }

  @Timed(value = "service.rest.list.paged", description = "Time taken to list users with pagination via Thrift")
  public List<User> listPaged(int page, int size) {
    log.debug("Listing users with pagination via Thrift client - page: {}, size: {}", page, size);
    
    var timer = metrics.startThriftClientTimer();
    try {
      metrics.incrementThriftClientRequests("listPaged");
      
      List<User> users = thriftClient.listPaged(page, size);
      log.debug("Retrieved {} users via Thrift for page: {}, size: {}", users.size(), page, size);
      return users;
    } catch (Exception e) {
      metrics.incrementThriftClientErrors("listPaged", e.getClass().getSimpleName());
      log.error("Failed to list users with pagination via Thrift - page: {}, size: {}", page, size, e);
      throw new ThriftServiceException("Failed to list users with pagination via Thrift service", "listPaged", e);
    } finally {
      metrics.recordThriftClientDuration(timer, "listPaged");
    }
  }

  @Timed(value = "service.rest.count", description = "Time taken to count users via Thrift")
  public long count() {
    log.debug("Counting users via Thrift client");
    
    var timer = metrics.startThriftClientTimer();
    try {
      metrics.incrementThriftClientRequests("count");
      
      long count = thriftClient.count();
      log.debug("User count via Thrift: {}", count);
      return count;
    } catch (Exception e) {
      metrics.incrementThriftClientErrors("count", e.getClass().getSimpleName());
      log.error("Failed to count users via Thrift", e);
      throw new ThriftServiceException("Failed to count users via Thrift service", "count", e);
    } finally {
      metrics.recordThriftClientDuration(timer, "count");
    }
  }
}