package com.example.user.adapter.mongo;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.persistence.type", havingValue = "mongo")
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final UserMongoRepository repository;

  private static UserDocument toDocument(User user) {
    return UserDocument.builder()
        .id(user.getId())
        .name(user.getName())
        .phone(user.getPhone())
        .address(user.getAddress())
        .build();
  }

  private static User toDomain(UserDocument doc) {
    return User.builder()
        .id(doc.getId())
        .name(doc.getName())
        .phone(doc.getPhone())
        .address(doc.getAddress())
        .build();
  }

  @Override
  public User save(User user) {
    UserDocument saved = repository.save(toDocument(user));
    return toDomain(saved);
  }

  @Override
  public Optional<User> findById(String id) {
    return repository.findById(id).map(UserRepositoryAdapter::toDomain);
  }

  @Override
  public void deleteById(String id) {
    repository.deleteById(id);
  }

  @Override
  public List<User> findAll() {
    return repository.findAll().stream().map(UserRepositoryAdapter::toDomain).collect(Collectors.toList());
  }
}
