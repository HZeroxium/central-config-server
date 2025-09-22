package com.example.rest.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.common.exception.ThriftServiceException;
import com.example.common.exception.UserNotFoundException;
import com.example.rest.user.constants.MetricsConstants;
import com.example.rest.user.constants.CacheConstants;
import io.micrometer.core.annotation.Timed;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
  private final ThriftUserClientPort thriftClient;

  @Timed(value = MetricsConstants.SERVICE_PING, description = MetricsConstants.PING_DESCRIPTION)
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "pingFallback")
  @Retry(name = "thrift-service")
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

  @Timed(value = MetricsConstants.SERVICE_CREATE, description = MetricsConstants.CREATE_DESCRIPTION)
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "createFallback")
  @Retry(name = "thrift-service")
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USER_BY_ID_CACHE, key = "'" + CacheConstants.USER_SERVICE_GET_BY_ID_KEY_PREFIX
          + "' + #user.id", condition = "#user != null && #user.id != null"),
      @CacheEvict(value = CacheConstants.USERS_BY_CRITERIA_CACHE, allEntries = true),
      @CacheEvict(value = CacheConstants.COUNT_BY_CRITERIA_CACHE, allEntries = true)
  })
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

  @Timed(value = MetricsConstants.SERVICE_GET_BY_ID, description = MetricsConstants.GET_BY_ID_DESCRIPTION)
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "getByIdFallback")
  @Retry(name = "thrift-service")
  @Cacheable(value = CacheConstants.USER_BY_ID_CACHE, key = "'" + CacheConstants.USER_SERVICE_GET_BY_ID_KEY_PREFIX
      + "' + #id", sync = true)
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
        throw new UserNotFoundException(id, "Thrift service", e);
      }
      throw new ThriftServiceException("Failed to retrieve user via Thrift service", "getById", e);
    }
  }

  @Timed(value = MetricsConstants.SERVICE_UPDATE, description = MetricsConstants.UPDATE_DESCRIPTION)
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "updateFallback")
  @Retry(name = "thrift-service")
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USER_BY_ID_CACHE, key = "'" + CacheConstants.USER_SERVICE_GET_BY_ID_KEY_PREFIX
          + "' + #user.id")
  })
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

  @Timed(value = MetricsConstants.SERVICE_DELETE, description = MetricsConstants.DELETE_DESCRIPTION)
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "deleteFallback")
  @Retry(name = "thrift-service")
  @Caching(evict = {
      @CacheEvict(value = CacheConstants.USER_BY_ID_CACHE, key = "'" + CacheConstants.USER_SERVICE_GET_BY_ID_KEY_PREFIX
          + "' + #id")
  })
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

  @Timed(value = MetricsConstants.SERVICE_LIST_BY_CRITERIA, description = MetricsConstants.LIST_BY_CRITERIA_DESCRIPTION)
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "listByCriteriaFallback")
  @Retry(name = "thrift-service")
  @Cacheable(value = CacheConstants.USERS_BY_CRITERIA_CACHE, key = "'"
      + CacheConstants.USER_SERVICE_LIST_BY_CRITERIA_KEY_PREFIX + "' + #criteria.hashCode()", sync = true)
  public List<User> listByCriteria(UserQueryCriteria criteria) {
    log.debug("Listing users by criteria via Thrift client: {}", criteria);
    try {
      List<User> users = thriftClient.listByCriteria(criteria);
      log.debug("Retrieved {} users via Thrift for criteria: {}", users.size(), criteria);
      return users;
    } catch (Exception e) {
      log.error("Failed to list users by criteria via Thrift: {}", criteria, e);
      throw new ThriftServiceException("Failed to list users by criteria via Thrift service", "listByCriteria", e);
    }
  }

  @Timed(value = MetricsConstants.SERVICE_COUNT_BY_CRITERIA, description = MetricsConstants.COUNT_BY_CRITERIA_DESCRIPTION)
  @Cacheable(value = CacheConstants.COUNT_BY_CRITERIA_CACHE, key = "'"
      + CacheConstants.USER_SERVICE_COUNT_BY_CRITERIA_KEY_PREFIX + "' + #criteria.hashCode()", sync = true)
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "countByCriteriaFallback")
  @Retry(name = "thrift-service")
  public long countByCriteria(UserQueryCriteria criteria) {
    log.debug("Counting users by criteria via Thrift client: {}", criteria);
    try {
      long count = thriftClient.countByCriteria(criteria);
      log.debug("User count by criteria via Thrift: {}", count);
      return count;
    } catch (Exception e) {
      log.error("Failed to count users by criteria via Thrift: {}", criteria, e);
      throw new ThriftServiceException("Failed to count users by criteria via Thrift service", "countByCriteria", e);
    }
  }

  // Fallback methods for circuit breaker
  public String pingFallback(Exception ex) {
    log.error("Ping fallback triggered due to: {}", ex.getMessage());
    throw new ThriftServiceException("Thrift service temporarily unavailable", "ping", ex);
  }

  public User createFallback(User user, Exception ex) {
    log.error("Create user fallback triggered for user: {}, due to: {}", user, ex.getMessage());
    throw new ThriftServiceException("User creation service temporarily unavailable", "create", ex);
  }

  public Optional<User> getByIdFallback(String id, Exception ex) {
    log.error("Get user by ID fallback triggered for ID: {}, due to: {}", id, ex.getMessage());
    throw new ThriftServiceException("User retrieval service temporarily unavailable", "getById", ex);
  }

  public User updateFallback(User user, Exception ex) {
    log.error("Update user fallback triggered for user: {}, due to: {}", user, ex.getMessage());
    throw new ThriftServiceException("User update service temporarily unavailable", "update", ex);
  }

  public Void deleteFallback(String id, Exception ex) {
    log.error("Delete user fallback triggered for ID: {}, due to: {}", id, ex.getMessage());
    throw new ThriftServiceException("User deletion service temporarily unavailable", "delete", ex);
  }

  public List<User> listByCriteriaFallback(UserQueryCriteria criteria, Exception ex) {
    log.error("List users by criteria fallback triggered for criteria: {}, due to: {}", criteria, ex.getMessage());
    throw new ThriftServiceException("User listing service temporarily unavailable", "listByCriteria", ex);
  }

  public long countByCriteriaFallback(UserQueryCriteria criteria, Exception ex) {
    log.error("Count users by criteria fallback triggered for criteria: {}, due to: {}", criteria, ex.getMessage());
    throw new ThriftServiceException("User count service temporarily unavailable", "countByCriteria", ex);
  }
}