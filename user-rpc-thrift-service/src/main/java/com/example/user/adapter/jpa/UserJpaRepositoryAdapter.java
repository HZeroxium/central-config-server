package com.example.user.adapter.jpa;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    log.debug("Saving user to H2 database: {}", user);
    try {
      String id = user.getId();
      if (id == null || id.isBlank()) {
        id = UUID.randomUUID().toString();
        user.setId(id);
        log.debug("Generated new UUID for user: {}", id);
      }
      
      UserEntity entity = toEntity(user);
      log.debug("Mapped domain user to JPA entity: {}", entity);
      
      UserEntity saved = repository.save(entity);
      log.debug("User saved to H2 database: {}", saved);
      
      User domainUser = toDomain(saved);
      log.debug("Mapped JPA entity to domain user: {}", domainUser);
      return domainUser;
    } catch (Exception e) {
      log.error("Failed to save user to H2 database: {}", user, e);
      throw e;
    }
  }

  /**
   * Find a user by id.
   *
   * @param id user identifier
   * @return optional domain user if present
   */
  @Override
  public Optional<User> findById(String id) {
    log.debug("Finding user by ID in H2 database: {}", id);
    try {
      return repository.findById(id)
          .map(entity -> {
            log.debug("User found in H2 database: {}", entity);
            User domainUser = toDomain(entity);
            log.debug("Mapped JPA entity to domain user: {}", domainUser);
            return domainUser;
          })
          .or(() -> {
            log.debug("User not found in H2 database for ID: {}", id);
            return Optional.empty();
          });
    } catch (Exception e) {
      log.error("Failed to find user in H2 database for ID: {}", id, e);
      throw e;
    }
  }

  /**
   * Delete a user by id.
   *
   * @param id user identifier
   */
  @Override
  public void deleteById(String id) {
    log.debug("Deleting user by ID from H2 database: {}", id);
    try {
      repository.deleteById(id);
      log.debug("User deleted from H2 database: {}", id);
    } catch (Exception e) {
      log.error("Failed to delete user from H2 database for ID: {}", id, e);
      throw e;
    }
  }

  /**
   * List all users.
   *
   * @return list of all domain users
   */
  @Override
  public List<User> findAll() {
    log.debug("Finding all users in H2 database");
    try {
      List<UserEntity> entities = repository.findAll();
      log.debug("Retrieved {} users from H2 database", entities.size());
      
      List<User> users = entities.stream()
          .map(entity -> {
            log.debug("Mapping JPA entity to domain user: {}", entity);
            return toDomain(entity);
          })
          .collect(Collectors.toList());
      
      log.debug("Mapped {} JPA entities to domain users", users.size());
      return users;
    } catch (Exception e) {
      log.error("Failed to find all users in H2 database", e);
      throw e;
    }
  }

  @Override
  public List<User> findPage(int page, int size) {
    log.debug("Finding users with pagination in H2 database - page: {}, size: {}", page, size);
    try {
      Page<UserEntity> pageResult = repository.findAll(PageRequest.of(page, size));
      log.debug("Retrieved {} users from H2 database for page: {}, size: {}", 
                pageResult.getContent().size(), page, size);
      
      List<User> users = pageResult.getContent().stream()
          .map(entity -> {
            log.debug("Mapping JPA entity to domain user: {}", entity);
            return toDomain(entity);
          })
          .collect(Collectors.toList());
      
      log.debug("Mapped {} JPA entities to domain users for page: {}, size: {}", 
                users.size(), page, size);
      return users;
    } catch (Exception e) {
      log.error("Failed to find users with pagination in H2 database - page: {}, size: {}", page, size, e);
      throw e;
    }
  }

  @Override
  public long count() {
    log.debug("Counting users in H2 database");
    try {
      long count = repository.count();
      log.debug("User count in H2 database: {}", count);
      return count;
    } catch (Exception e) {
      log.error("Failed to count users in H2 database", e);
      throw e;
    }
  }
}


