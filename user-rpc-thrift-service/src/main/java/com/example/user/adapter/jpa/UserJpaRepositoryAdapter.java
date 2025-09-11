package com.example.user.adapter.jpa;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * JPA-based adapter implementing {@link UserRepositoryPort}.
 * <p>
 * Active when property {@code app.persistence.type=h2}. Maps the domain model {@link com.example.user.domain.User}
 * to the persistence entity {@link UserEntity} and delegates operations to {@link UserJpaRepository}.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.persistence.type", havingValue = "h2")
public class UserJpaRepositoryAdapter implements UserRepositoryPort {
  private final UserJpaRepository repository;

  /**
   * Create adapter with the injected Spring Data JPA repository.
   *
   * @param repository concrete JPA repository
   */
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

  /**
   * Save or update a domain user into the database.
   * If the identifier is missing, a random UUID is generated.
   *
   * @param user domain user to persist
   * @return persisted user reloaded from the database
   */
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

  /**
   * Find a user by id.
   *
   * @param id user identifier
   * @return optional domain user if present
   */
  @Override
  public Optional<User> findById(String id) {
    return repository.findById(id).map(UserJpaRepositoryAdapter::toDomain);
  }

  /**
   * Delete a user by id.
   *
   * @param id user identifier
   */
  @Override
  public void deleteById(String id) {
    repository.deleteById(id);
  }

  /**
   * List all users.
   *
   * @return list of all domain users
   */
  @Override
  public List<User> findAll() {
    return repository.findAll().stream().map(UserJpaRepositoryAdapter::toDomain).collect(Collectors.toList());
  }

  @Override
  public List<User> findPage(int page, int size) {
    Page<UserEntity> p = repository.findAll(PageRequest.of(page, size));
    return p.getContent().stream().map(UserJpaRepositoryAdapter::toDomain).collect(Collectors.toList());
  }

  @Override
  public long count() {
    return repository.count();
  }
}


