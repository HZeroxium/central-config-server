package com.example.user.service.port;

import com.example.user.domain.User;
import com.example.user.domain.UserQueryCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Port exposing domain-level user operations used by external interfaces (REST/Thrift).
 */
public interface UserServicePort {
  /** Health probe. */
  String ping();

  /** Create a user. */
  User create(User user);

  /** Get user by id. */
  Optional<User> getById(String id);

  /** Update a user. */
  User update(User user);

  /** Delete a user. */
  void delete(String id);

  /**
   * List users with pagination.
   *
   * @param page zero-based page index
   * @param size page size
   * @return list of users in current page
   */
  List<User> list(int page, int size);

  /**
   * List users with advanced query criteria.
   *
   * @param criteria query criteria including search, filters, sorting
   * @return list of users matching the criteria
   */
  List<User> listByCriteria(UserQueryCriteria criteria);

  /**
   * Count users matching the query criteria.
   *
   * @param criteria query criteria including search, filters
   * @return number of users matching the criteria
   */
  long countByCriteria(UserQueryCriteria criteria);

  /** Total users count. */
  long count();
}
