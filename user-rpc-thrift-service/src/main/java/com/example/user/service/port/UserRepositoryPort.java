package com.example.user.service.port;

import com.example.user.domain.User;
import com.example.user.domain.UserQueryCriteria;

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

  /**
   * Find users with pagination.
   *
   * @param page zero-based page index
   * @param size page size
   * @return list of users for the current page
   */
  List<User> findPage(int page, int size);

  /**
   * Find users with advanced query criteria.
   *
   * @param criteria query criteria including search, filters, sorting
   * @return list of users matching the criteria
   */
  List<User> findByCriteria(UserQueryCriteria criteria);

  /**
   * Count users matching the query criteria.
   *
   * @param criteria query criteria including search, filters
   * @return number of users matching the criteria
   */
  long countByCriteria(UserQueryCriteria criteria);

  /**
   * @return total number of users
   */
  long count();
}
