package com.example.user.service.port;

import com.example.user.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
  User save(User user);

  Optional<User> findById(String id);

  void deleteById(String id);

  List<User> findAll();
}
