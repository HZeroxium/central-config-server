package com.example.user.adapter.mongo;

import com.example.user.adapter.mongo.mapper.UserMongoPersistenceMapper;
import com.example.common.domain.SortCriterion;
import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.user.service.port.UserRepositoryPort;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * MongoDB-based adapter implementing {@link UserRepositoryPort}.
 * Active when property app.persistence.type=mongo.
 * 
 * Enhanced with comprehensive profiling and metrics collection.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.persistence.type", havingValue = "mongo")
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final UserMongoRepository repository;

  // Delegate mapping to mapper

  @Override
  @Timed(value = "database.mongo.save", description = "Time taken to save user via MongoDB")
  public User save(User user) {
    log.debug("Saving user to MongoDB: {}", user);
    try {

      // Check if this is a new user (no ID) or existing user
      boolean isNewUser = user.getId() == null || user.getId().isBlank();

      if (isNewUser) {
        log.debug("Creating new user in MongoDB");
        // Generate new ID for new user and ensure no version is set
        String newId = java.util.UUID.randomUUID().toString();
        user = user.toBuilder()
            .id(newId)
            .version(null) // Ensure no version for new entities
            .build();
      } else {
        log.debug("Updating existing user in MongoDB with ID: {}", user.getId());
        // Check if user exists
        Optional<UserDocument> existing = repository.findById(user.getId());
        if (existing.isPresent()) {
          log.debug("User exists, preserving version: {}", existing.get().getVersion());
          // Preserve existing version for update
          user = user.toBuilder().version(existing.get().getVersion()).build();
        }
      }

      UserDocument document = UserMongoPersistenceMapper.toDocument(user);
      log.debug("Mapped domain user to MongoDB document: {}", document);

      UserDocument saved = repository.save(document);
      log.debug("User saved to MongoDB: {}", saved);

      User domainUser = UserMongoPersistenceMapper.toDomain(saved);
      log.debug("Mapped MongoDB document to domain user: {}", domainUser);
      return domainUser;
    } catch (Exception e) {
      log.error("Failed to save user to MongoDB: {}", user, e);
      throw e;
    }
  }

  @Override
  @Timed(value = "database.mongo.find.by.id", description = "Time taken to find user by ID via MongoDB")
  public Optional<User> findById(String id) {
    log.debug("Finding user by ID in MongoDB: {}", id);
    try {

      return repository.findByIdAndNotDeleted(id)
          .map(document -> UserMongoPersistenceMapper.toDomain(document));
    } catch (Exception e) {
      log.error("Failed to find user by ID in MongoDB: {}", id, e);
      throw e;
    }
  }

  @Override
  @Timed(value = "database.mongo.delete.by.id", description = "Time taken to soft delete user by ID via MongoDB")
  public void deleteById(String id) {
    log.debug("Soft deleting user by ID in MongoDB: {}", id);
    try {

      // Find the user first
      Optional<UserDocument> userDoc = repository.findByIdAndNotDeleted(id);
      if (userDoc.isPresent()) {
        UserDocument doc = userDoc.get();
        doc.setDeleted(true);
        doc.setDeletedAt(java.time.LocalDateTime.now());
        doc.setDeletedBy("admin");
        repository.save(doc);
        log.debug("User soft deleted from MongoDB: {}", id);
      } else {
        log.warn("User not found for soft deletion: {}", id);
      }
    } catch (Exception e) {
      log.error("Failed to soft delete user by ID in MongoDB: {}", id, e);
      throw e;
    }
  }

  @Override
  @Timed(value = "database.mongo.find.by.criteria", description = "Time taken to find users by criteria via MongoDB")
  public List<User> findByCriteria(UserQueryCriteria criteria) {
    log.debug("Finding users by criteria in MongoDB: {}", criteria);
    try {

      // Build pageable with sorting
      Pageable pageable = buildPageable(criteria);

      // Prepare search term
      String searchTerm = criteria.hasSearch() ? criteria.getSearch() : "";

      // Call repository method
      List<UserDocument> documents = repository.findByAdvancedCriteria(
          criteria.getIncludeDeleted(),
          searchTerm,
          criteria.getStatus(),
          criteria.getRole(),
          criteria.getCreatedAfter(),
          criteria.getCreatedBefore(),
          pageable);

      log.debug("Found {} users matching criteria in MongoDB", documents.size());

      List<User> users = documents.stream()
          .map(UserMongoPersistenceMapper::toDomain)
          .collect(java.util.stream.Collectors.toList());

      log.debug("Mapped {} MongoDB documents to domain users", users.size());
      return users;
    } catch (Exception e) {
      log.error("Failed to find users by criteria in MongoDB: {}", criteria, e);
      throw e;
    }
  }

  @Override
  @Timed(value = "database.mongo.count.by.criteria", description = "Time taken to count users by criteria via MongoDB")
  public long countByCriteria(UserQueryCriteria criteria) {
    log.debug("Counting users by criteria in MongoDB: {}", criteria);
    try {

      // Prepare search term
      String searchTerm = criteria.hasSearch() ? criteria.getSearch() : "";

      // Call repository method
      long count = repository.countByAdvancedCriteria(
          criteria.getIncludeDeleted(),
          searchTerm,
          criteria.getStatus(),
          criteria.getRole(),
          criteria.getCreatedAfter(),
          criteria.getCreatedBefore());

      log.debug("User count by criteria in MongoDB: {}", count);
      return count;
    } catch (Exception e) {
      log.error("Failed to count users by criteria in MongoDB: {}", criteria, e);
      throw e;
    }
  }

  /**
   * Build Pageable with sorting support for MongoDB.
   */
  private Pageable buildPageable(UserQueryCriteria criteria) {
    PageRequest pageRequest = PageRequest.of(
        criteria.getPage(),
        criteria.getSize());

    List<SortCriterion> sortCriteria = criteria.getSortCriteria();
    if (sortCriteria != null && !sortCriteria.isEmpty()) {
      List<Sort.Order> orders = sortCriteria.stream()
          .filter(SortCriterion::isValid)
          .map(sc -> {
            org.springframework.data.domain.Sort.Direction sortDirection = "desc".equalsIgnoreCase(sc.getDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
            return new Sort.Order(sortDirection, sc.getFieldName());
          })
          .collect(java.util.stream.Collectors.toList());

      if (!orders.isEmpty()) {
        return pageRequest.withSort(Sort.by(orders));
      }
    }

    // Default sorting if no valid criteria provided
    return pageRequest.withSort(Sort.by(
        Sort.Direction.DESC, "createdAt"));
  }
}