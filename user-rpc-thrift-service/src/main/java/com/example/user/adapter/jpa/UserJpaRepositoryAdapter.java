package com.example.user.adapter.jpa;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(name = "app.persistence.type", havingValue = "h2")
public class UserJpaRepositoryAdapter implements UserRepositoryPort {
  private final UserJpaRepository repository;

  public UserJpaRepositoryAdapter(UserJpaRepository repository) {
    this.repository = repository;
  }

  private static UserEntity toEntity(User user) {
    return UserEntity.builder()
        .id(user.getId())
        .name(user.getName())
        .phone(user.getPhone())
        .address(user.getAddress())
        .build();
  }

  private static User toDomain(UserEntity e) {
    return User.builder()
        .id(e.getId())
        .name(e.getName())
        .phone(e.getPhone())
        .address(e.getAddress())
        .build();
  }

  @Override
  public User save(User user) {
    String id = user.getId();
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
      user.setId(id);
    }
    UserEntity saved = repository.save(toEntity(user));
    return toDomain(saved);
  }

  @Override
  public Optional<User> findById(String id) {
    return repository.findById(id).map(UserJpaRepositoryAdapter::toDomain);
  }

  @Override
  public void deleteById(String id) {
    repository.deleteById(id);
  }

  @Override
  public List<User> findAll() {
    return repository.findAll().stream().map(UserJpaRepositoryAdapter::toDomain).collect(Collectors.toList());
  }
}


