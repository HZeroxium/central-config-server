package com.example.user.service.port;

import com.example.user.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Port for user persistence operations. Implemented by adapters (JPA, Mongo).
 */
public interface UserRepositoryPort {
  /** Persist or update a user. */
  User save(User user);

  /** Find a user by id. */
  Optional<User> findById(String id);

  /** Delete a user by id. */
  void deleteById(String id);

  /** List all users. */
  List<User> findAll();

  /**
   * Find users with pagination.
   *
   * @param page zero-based page index
   * @param size page size
   * @return list of users for the current page
   */
  List<User> findPage(int page, int size);

  /**
   * @return total number of users
   */
  long count();
}
