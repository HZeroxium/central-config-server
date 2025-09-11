package com.example.user.service;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
import com.example.user.service.port.UserServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserServicePort {

  private final UserRepositoryPort userRepository;

  @Override
  public String ping() {
    return "pong";
  }

  @Override
  public User create(User user) {
    return userRepository.save(user);
  }

  @Override
  public Optional<User> getById(String id) {
    return userRepository.findById(id);
  }

  @Override
  public User update(User user) {
    return userRepository.save(user);
  }

  @Override
  public void delete(String id) {
    userRepository.deleteById(id);
  }

  @Override
  public List<User> list() {
    return userRepository.findAll();
  }
}
