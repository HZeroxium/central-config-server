package com.example.user.service.port;

import com.example.user.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserServicePort {
  String ping();

  User create(User user);

  Optional<User> getById(String id);

  User update(User user);

  void delete(String id);

  List<User> list();
}
