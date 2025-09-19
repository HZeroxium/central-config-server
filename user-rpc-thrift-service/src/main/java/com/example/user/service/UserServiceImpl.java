package com.example.user.service;

import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.user.service.port.UserRepositoryPort;
import com.example.user.service.port.UserServicePort;
import com.example.common.exception.DatabaseException;
 
import io.micrometer.core.annotation.Timed;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementing {@link UserServicePort} to orchestrate domain operations.
 * Delegates persistence to {@link com.example.user.service.port.UserRepositoryPort}.
 * 
 * Enhanced with comprehensive profiling and metrics collection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserServicePort {

  private final UserRepositoryPort userRepository;

  /** {@inheritDoc} */
  @Override
  @Timed(value = "service.user.ping", description = "Time taken to handle service ping")
  public String ping() {
    log.debug("Service ping requested");
    try {
      String response = "pong";
      log.debug("Service ping successful: {}", response);
      return response;
    } catch (Exception e) {
      log.error("Service ping failed", e);
      throw e;
    }
  }

  /** {@inheritDoc} */
  @Override
  @Timed(value = "service.user.create", description = "Time taken to create user")
  @Caching(evict = {
    @CacheEvict(value = "userById", key = "'UserServiceImpl:getById:v1:' + #user.id", condition = "#user != null && #user.id != null"),
    @CacheEvict(value = "usersByCriteria", allEntries = true),
    @CacheEvict(value = "countByCriteria", allEntries = true)
  })
  public User create(User user) {
    log.debug("Creating user in repository: {}", user);
    try {
      // Set audit fields for new user
      User userWithAudit = user.toBuilder()
          .createdAt(java.time.LocalDateTime.now())
          .createdBy("admin")
          .updatedAt(java.time.LocalDateTime.now())
          .updatedBy("admin")
          .version(null) // Let repository handle version
          .deleted(false)
          .deletedAt(null)
          .deletedBy(null)
          .build();
      
      User created = userRepository.save(userWithAudit);
      log.info("User created successfully with ID: {}", created.getId());
      return created;
    } catch (Exception e) {
      log.error("Failed to create user: {}", user, e);
      throw new DatabaseException("Failed to create user in database", "create", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  @Timed(value = "service.user.get.by.id", description = "Time taken to get user by ID")
  @Cacheable(value = "userById", key = "'UserServiceImpl:getById:v1:' + #id", sync = true)
  public Optional<User> getById(String id) {
    log.debug("Retrieving user by ID from repository: {}", id);
    try {
      Optional<User> user = userRepository.findById(id);
      if (user.isPresent()) {
        log.debug("User found in repository: {}", user.get());
      } else {
        log.debug("User not found in repository for ID: {}", id);
      }
      return user;
    } catch (Exception e) {
      log.error("Failed to retrieve user from repository for ID: {}", id, e);
      throw new DatabaseException("Failed to retrieve user from database", "getById", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  @Timed(value = "service.user.update", description = "Time taken to update user")
  @Caching(evict = {
    @CacheEvict(value = "userById", key = "'UserServiceImpl:getById:v1:' + #user.id"),
    @CacheEvict(value = "usersByCriteria", allEntries = true),
    @CacheEvict(value = "countByCriteria", allEntries = true)
  })
  public User update(User user) {
    log.debug("Updating user in repository: {}", user);
    try {
      // Get existing user to preserve audit fields
      Optional<User> existingUser = userRepository.findById(user.getId());
      if (existingUser.isEmpty()) {
        throw new DatabaseException("User not found for update: " + user.getId(), "update");
      }
      
      // Preserve audit fields and update only what's needed
      User userWithAudit = user.toBuilder()
          .createdAt(existingUser.get().getCreatedAt()) // Preserve original
          .createdBy(existingUser.get().getCreatedBy()) // Preserve original
          .updatedAt(java.time.LocalDateTime.now()) // Update timestamp
          .updatedBy("admin") // Update user
          .version(user.getVersion() != null ? user.getVersion() : existingUser.get().getVersion()) // Use provided or existing
          .deleted(false) // Ensure not deleted
          .deletedAt(null) // Clear deletion
          .deletedBy(null) // Clear deletion
          .build();
      
      User updated = userRepository.save(userWithAudit);
      log.info("User updated successfully with ID: {}", updated.getId());
      return updated;
    } catch (Exception e) {
      log.error("Failed to update user in repository: {}", user, e);
      throw new DatabaseException("Failed to update user in database", "update", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  @Timed(value = "service.user.delete", description = "Time taken to delete user")
  @Caching(evict = {
    @CacheEvict(value = "userById", key = "'UserServiceImpl:getById:v1:' + #id"),
    @CacheEvict(value = "usersByCriteria", allEntries = true),
    @CacheEvict(value = "countByCriteria", allEntries = true)
  })
  public void delete(String id) {
    log.debug("Deleting user from repository: {}", id);
    try {
      userRepository.deleteById(id);
      log.info("User deleted successfully with ID: {}", id);
    } catch (Exception e) {
      log.error("Failed to delete user from repository for ID: {}", id, e);
      throw new DatabaseException("Failed to delete user from database", "delete", e);
    }
  }

  

  /** {@inheritDoc} */
  @Override
  @Timed(value = "service.user.list.by.criteria", description = "Time taken to list users by criteria")
  @Cacheable(value = "usersByCriteria", key = "'UserServiceImpl:listByCriteria:v1:' + #criteria.hashCode()", sync = true)
  public List<User> listByCriteria(UserQueryCriteria criteria) {
    log.debug("Listing users by criteria from repository: {}", criteria);
    try {
      List<User> users = userRepository.findByCriteria(criteria);
      log.debug("Retrieved {} users from repository for criteria: {}", users.size(), criteria);
      return users;
    } catch (Exception e) {
      log.error("Failed to list users by criteria from repository: {}", criteria, e);
      throw new DatabaseException("Failed to list users by criteria from database", "listByCriteria", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  @Timed(value = "service.user.count.by.criteria", description = "Time taken to count users by criteria")
  @Cacheable(value = "countByCriteria", key = "'UserServiceImpl:countByCriteria:v1:' + #criteria.hashCode()", sync = true)
  public long countByCriteria(UserQueryCriteria criteria) {
    log.debug("Counting users by criteria in repository: {}", criteria);
    try {
      long count = userRepository.countByCriteria(criteria);
      log.debug("User count by criteria in repository: {}", count);
      return count;
    } catch (Exception e) {
      log.error("Failed to count users by criteria in repository: {}", criteria, e);
      throw new DatabaseException("Failed to count users by criteria in database", "countByCriteria", e);
    }
  }
}