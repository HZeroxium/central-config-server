package com.example.user.service;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
import com.example.user.service.port.UserServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementing {@link UserServicePort} to orchestrate domain operations.
 * Delegates persistence to {@link com.example.user.service.port.UserRepositoryPort}.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserServicePort {

  private final UserRepositoryPort userRepository;

  /** {@inheritDoc} */
  @Override
  public String ping() {
    return "pong";
  }

  /** {@inheritDoc} */
  @Override
  public User create(User user) {
    return userRepository.save(user);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<User> getById(String id) {
    return userRepository.findById(id);
  }

  /** {@inheritDoc} */
  @Override
  public User update(User user) {
    return userRepository.save(user);
  }

  /** {@inheritDoc} */
  @Override
  public void delete(String id) {
    userRepository.deleteById(id);
  }

  /** {@inheritDoc} */
  @Override
  public List<User> list() {
    return userRepository.findAll();
  }
}
