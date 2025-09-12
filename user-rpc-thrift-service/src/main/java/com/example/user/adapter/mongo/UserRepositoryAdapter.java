package com.example.user.adapter.mongo;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

/**
 * MongoDB-based adapter implementing {@link UserRepositoryPort}.
 * Active when property app.persistence.type=mongo.
 */
@Slf4j
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
    log.debug("Saving user to MongoDB: {}", user);
    try {
      UserDocument document = toDocument(user);
      log.debug("Mapped domain user to MongoDB document: {}", document);
      
      UserDocument saved = repository.save(document);
      log.debug("User saved to MongoDB: {}", saved);
      
      User domainUser = toDomain(saved);
      log.debug("Mapped MongoDB document to domain user: {}", domainUser);
      return domainUser;
    } catch (Exception e) {
      log.error("Failed to save user to MongoDB: {}", user, e);
      throw e;
    }
  }

  @Override
  public Optional<User> findById(String id) {
    log.debug("Finding user by ID in MongoDB: {}", id);
    try {
      return repository.findById(id)
          .map(document -> {
            log.debug("User found in MongoDB: {}", document);
            User domainUser = toDomain(document);
            log.debug("Mapped MongoDB document to domain user: {}", domainUser);
            return domainUser;
          })
          .or(() -> {
            log.debug("User not found in MongoDB for ID: {}", id);
            return Optional.empty();
          });
    } catch (Exception e) {
      log.error("Failed to find user in MongoDB for ID: {}", id, e);
      throw e;
    }
  }

  @Override
  public void deleteById(String id) {
    log.debug("Deleting user by ID from MongoDB: {}", id);
    try {
      repository.deleteById(id);
      log.debug("User deleted from MongoDB: {}", id);
    } catch (Exception e) {
      log.error("Failed to delete user from MongoDB for ID: {}", id, e);
      throw e;
    }
  }

  @Override
  public List<User> findAll() {
    log.debug("Finding all users in MongoDB");
    try {
      List<UserDocument> documents = repository.findAll();
      log.debug("Retrieved {} users from MongoDB", documents.size());
      
      List<User> users = documents.stream()
          .map(document -> {
            log.debug("Mapping MongoDB document to domain user: {}", document);
            return toDomain(document);
          })
          .collect(Collectors.toList());
      
      log.debug("Mapped {} MongoDB documents to domain users", users.size());
      return users;
    } catch (Exception e) {
      log.error("Failed to find all users in MongoDB", e);
      throw e;
    }
  }

  @Override
  public List<User> findPage(int page, int size) {
    log.debug("Finding users with pagination in MongoDB - page: {}, size: {}", page, size);
    try {
      Page<UserDocument> pageResult = repository.findAll(PageRequest.of(page, size));
      List<UserDocument> documents = pageResult.getContent();
      log.debug("Retrieved {} users from MongoDB for page: {}, size: {}", documents.size(), page, size);
      
      List<User> users = documents.stream()
          .map(document -> {
            log.debug("Mapping MongoDB document to domain user: {}", document);
            return toDomain(document);
          })
          .collect(Collectors.toList());
      
      log.debug("Mapped {} MongoDB documents to domain users for page: {}, size: {}", 
                users.size(), page, size);
      return users;
    } catch (Exception e) {
      log.error("Failed to find users with pagination in MongoDB - page: {}, size: {}", page, size, e);
      throw e;
    }
  }

  @Override
  public long count() {
    log.debug("Counting users in MongoDB");
    try {
      long count = repository.count();
      log.debug("User count in MongoDB: {}", count);
      return count;
    } catch (Exception e) {
      log.error("Failed to count users in MongoDB", e);
      throw e;
    }
  }
}
