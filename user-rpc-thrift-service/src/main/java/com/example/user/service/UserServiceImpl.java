package com.example.user.service;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
import com.example.user.service.port.UserServicePort;
import com.example.user.exception.DatabaseException;
import com.example.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementing {@link UserServicePort} to orchestrate domain operations.
 * Delegates persistence to {@link com.example.user.service.port.UserRepositoryPort}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserServicePort {

  private final UserRepositoryPort userRepository;

  /** {@inheritDoc} */
  @Override
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
  public User create(User user) {
    log.debug("Creating user in repository: {}", user);
    try {
      User created = userRepository.save(user);
      log.info("User created successfully with ID: {}", created.getId());
      return created;
    } catch (Exception e) {
      log.error("Failed to create user: {}", user, e);
      throw new DatabaseException("Failed to create user in database", "create", e);
    }
  }

  /** {@inheritDoc} */
  @Override
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
  public User update(User user) {
    log.debug("Updating user in repository: {}", user);
    try {
      User updated = userRepository.save(user);
      log.info("User updated successfully with ID: {}", updated.getId());
      return updated;
    } catch (Exception e) {
      log.error("Failed to update user in repository: {}", user, e);
      throw new DatabaseException("Failed to update user in database", "update", e);
    }
  }

  /** {@inheritDoc} */
  @Override
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
  public List<User> list() {
    log.debug("Listing all users from repository");
    try {
      List<User> users = userRepository.findAll();
      log.debug("Retrieved {} users from repository", users.size());
      return users;
    } catch (Exception e) {
      log.error("Failed to list users from repository", e);
      throw new DatabaseException("Failed to list users from database", "list", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<User> listPaged(int page, int size) {
    log.debug("Listing users with pagination from repository - page: {}, size: {}", page, size);
    try {
      List<User> users = userRepository.findPage(page, size);
      log.debug("Retrieved {} users from repository for page: {}, size: {}", users.size(), page, size);
      return users;
    } catch (Exception e) {
      log.error("Failed to list users with pagination from repository - page: {}, size: {}", page, size, e);
      throw new DatabaseException("Failed to list users with pagination from database", "listPaged", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public long count() {
    log.debug("Counting users in repository");
    try {
      long count = userRepository.count();
      log.debug("User count in repository: {}", count);
      return count;
    } catch (Exception e) {
      log.error("Failed to count users in repository", e);
      throw new DatabaseException("Failed to count users in database", "count", e);
    }
  }
}
