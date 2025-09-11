package com.example.user.service.port;

import com.example.user.domain.User;

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

  /** List all users. */
  List<User> list();
}
