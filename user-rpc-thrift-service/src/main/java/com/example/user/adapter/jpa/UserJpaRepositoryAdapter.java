package com.example.user.adapter.jpa;

import com.example.user.adapter.jpa.mapper.UserPersistenceMapper;
import com.example.user.domain.SortCriterion;
import com.example.user.domain.User;
import com.example.user.domain.UserQueryCriteria;
import com.example.user.service.port.UserRepositoryPort;
import com.example.user.metrics.ApplicationMetrics;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * JPA-based adapter implementing {@link UserRepositoryPort}.
 * <p>
 * Active when property {@code app.persistence.type=h2}. Maps the domain model {@link com.example.user.domain.User}
 * to the persistence entity {@link UserEntity} and delegates operations to {@link UserJpaRepository}.
 * 
 * Enhanced with comprehensive profiling and metrics collection.
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "app.persistence.type", havingValue = "h2")
public class UserJpaRepositoryAdapter implements UserRepositoryPort {
  private final UserJpaRepository repository;
  private final ApplicationMetrics metrics;

  /**
   * Create adapter with the injected Spring Data JPA repository and metrics.
   *
   * @param repository concrete JPA repository
   * @param metrics application metrics for profiling
   */
  public UserJpaRepositoryAdapter(UserJpaRepository repository, ApplicationMetrics metrics) {
    this.repository = repository;
    this.metrics = metrics;
  }

  // Delegate entity<->domain conversion to mapper

  /**
   * Save or update a domain user into the database.
   * If the identifier is missing, a random UUID is generated.
   *
   * @param user domain user to persist
   * @return persisted user reloaded from the database
   */
  @Override
  @Timed(value = "database.jpa.save", description = "Time taken to save user via JPA")
  public User save(User user) {
    log.debug("Saving user to H2 database: {}", user);
    
    var timer = metrics.startDatabaseTimer();
    try {
      metrics.incrementDatabaseOperations("save", "users");
      
      // Check if this is a new user (no ID) or existing user
      boolean isNewUser = user.getId() == null || user.getId().isBlank();
      
      if (isNewUser) {
        log.debug("Creating new user in H2 database");
        // Generate new ID for new user and ensure no version is set
        String newId = UUID.randomUUID().toString();
        user = user.toBuilder()
            .id(newId)
            .version(null) // Ensure no version for new entities
            .build();
      } else {
        log.debug("Updating existing user in H2 database with ID: {}", user.getId());
        // Check if user exists
        Optional<UserEntity> existing = repository.findById(user.getId());
        if (existing.isPresent()) {
          log.debug("User exists, preserving version: {}", existing.get().getVersion());
          // Preserve existing version for update
          user = user.toBuilder().version(existing.get().getVersion()).build();
        }
      }
      
      UserEntity entity = UserPersistenceMapper.toEntity(user);
      log.debug("Mapped domain user to JPA entity: {}", entity);
      
      UserEntity saved = repository.save(entity);
      log.debug("User saved to H2 database: {}", saved);
      
      User domainUser = UserPersistenceMapper.toDomain(saved);
      log.debug("Mapped JPA entity to domain user: {}", domainUser);
      return domainUser;
    } catch (Exception e) {
      metrics.incrementDatabaseErrors("save", "users", e.getClass().getSimpleName());
      log.error("Failed to save user to H2 database: {}", user, e);
      throw e;
    } finally {
      metrics.recordDatabaseDuration(timer, "save", "users");
    }
  }

  /**
   * Find a user by id.
   *
   * @param id user identifier
   * @return optional domain user if present
   */
  @Override
  @Timed(value = "database.jpa.find.by.id", description = "Time taken to find user by ID via JPA")
  public Optional<User> findById(String id) {
    log.debug("Finding user by ID in H2 database: {}", id);
    
    var timer = metrics.startDatabaseTimer();
    try {
      metrics.incrementDatabaseOperations("findById", "users");
      
      return repository.findByIdAndNotDeleted(id)
          .map(entity -> UserPersistenceMapper.toDomain(entity));
    } catch (Exception e) {
      metrics.incrementDatabaseErrors("findById", "users", e.getClass().getSimpleName());
      log.error("Failed to find user by ID in H2 database: {}", id, e);
      throw e;
    } finally {
      metrics.recordDatabaseDuration(timer, "findById", "users");
    }
  }

  /**
   * Find all users with pagination.
   *
   * @param page zero-based page index
   * @param size page size
   * @return list of domain users
   */
  @Override
  @Timed(value = "database.jpa.find.all.paged", description = "Time taken to find all users with pagination via JPA")
  public List<User> findPage(int page, int size) {
    log.debug("Finding all users with pagination in H2 database - page: {}, size: {}", page, size);
    
    var timer = metrics.startDatabaseTimer();
    try {
      metrics.incrementDatabaseOperations("findAllPaged", "users");
      
      PageRequest pageRequest = PageRequest.of(page, size);
      Page<UserEntity> entityPage = repository.findAllNotDeleted(pageRequest);
      log.debug("Retrieved {} users from H2 database for page: {}, size: {}", entityPage.getContent().size(), page, size);
      
      List<User> domainUsers = entityPage.getContent().stream()
          .map(UserPersistenceMapper::toDomain)
          .collect(Collectors.toList());
      
      log.debug("Mapped {} JPA entities to domain users", domainUsers.size());
      return domainUsers;
    } catch (Exception e) {
      metrics.incrementDatabaseErrors("findAllPaged", "users", e.getClass().getSimpleName());
      log.error("Failed to find all users with pagination in H2 database - page: {}, size: {}", page, size, e);
      throw e;
    } finally {
      metrics.recordDatabaseDuration(timer, "findAllPaged", "users");
    }
  }

  /**
   * Count total number of users.
   *
   * @return total count
   */
  @Override
  @Timed(value = "database.jpa.count", description = "Time taken to count users via JPA")
  public long count() {
    log.debug("Counting users in H2 database");
    
    var timer = metrics.startDatabaseTimer();
    try {
      metrics.incrementDatabaseOperations("count", "users");
      
      long totalCount = repository.countNotDeleted();
      log.debug("Total user count in H2 database: {}", totalCount);
      return totalCount;
    } catch (Exception e) {
      metrics.incrementDatabaseErrors("count", "users", e.getClass().getSimpleName());
      log.error("Failed to count users in H2 database", e);
      throw e;
    } finally {
      metrics.recordDatabaseDuration(timer, "count", "users");
    }
  }

  /**
   * Soft delete a user by id.
   *
   * @param id user identifier
   */
  @Override
  @Timed(value = "database.jpa.delete.by.id", description = "Time taken to soft delete user by ID via JPA")
  public void deleteById(String id) {
    log.debug("Soft deleting user by ID in H2 database: {}", id);
    
    var timer = metrics.startDatabaseTimer();
    try {
      metrics.incrementDatabaseOperations("deleteById", "users");
      
      // Find the user first
      Optional<UserEntity> userEntity = repository.findByIdAndNotDeleted(id);
      if (userEntity.isPresent()) {
        UserEntity entity = userEntity.get();
        entity.setDeleted(true);
        entity.setDeletedAt(java.time.LocalDateTime.now());
        entity.setDeletedBy("admin");
        repository.save(entity);
        log.debug("User soft deleted from H2 database: {}", id);
      } else {
        log.warn("User not found for soft deletion: {}", id);
      }
    } catch (Exception e) {
      metrics.incrementDatabaseErrors("deleteById", "users", e.getClass().getSimpleName());
      log.error("Failed to soft delete user by ID in H2 database: {}", id, e);
      throw e;
    } finally {
      metrics.recordDatabaseDuration(timer, "deleteById", "users");
    }
  }

  @Override
  @Timed(value = "database.jpa.find.by.criteria", description = "Time taken to find users by criteria via JPA")
  public List<User> findByCriteria(UserQueryCriteria criteria) {
    log.debug("Finding users by criteria in H2 database: {}", criteria);
    
    var timer = metrics.startDatabaseTimer();
    try {
      metrics.incrementDatabaseOperations("findByCriteria", "users");
      
      // Build pageable with sorting
      Pageable pageable = buildPageable(criteria);
      
      // Prepare search term
      String searchTerm = criteria.hasSearch() ? criteria.getSearch() : "";
      
      // Call repository method
      List<UserEntity> entities = repository.findByAdvancedCriteria(
          criteria.getIncludeDeleted(),
          searchTerm,
          criteria.getStatus(),
          criteria.getRole(),
          criteria.getCreatedAfter(),
          criteria.getCreatedBefore(),
          pageable
      );
      
      log.debug("Found {} users matching criteria in H2 database", entities.size());
      
      List<User> users = entities.stream()
          .map(UserPersistenceMapper::toDomain)
          .collect(java.util.stream.Collectors.toList());
      
      log.debug("Mapped {} JPA entities to domain users", users.size());
      return users;
    } catch (Exception e) {
      metrics.incrementDatabaseErrors("findByCriteria", "users", e.getClass().getSimpleName());
      log.error("Failed to find users by criteria in H2 database: {}", criteria, e);
      throw e;
    } finally {
      metrics.recordDatabaseDuration(timer, "findByCriteria", "users");
    }
  }

  @Override
  @Timed(value = "database.jpa.count.by.criteria", description = "Time taken to count users by criteria via JPA")
  public long countByCriteria(UserQueryCriteria criteria) {
    log.debug("Counting users by criteria in H2 database: {}", criteria);
    
    var timer = metrics.startDatabaseTimer();
    try {
      metrics.incrementDatabaseOperations("countByCriteria", "users");
      
      // Prepare search term
      String searchTerm = criteria.hasSearch() ? criteria.getSearch() : "";
      
      // Call repository method
      long count = repository.countByAdvancedCriteria(
          criteria.getIncludeDeleted(),
          searchTerm,
          criteria.getStatus(),
          criteria.getRole(),
          criteria.getCreatedAfter(),
          criteria.getCreatedBefore()
      );
      
      log.debug("User count by criteria in H2 database: {}", count);
      return count;
    } catch (Exception e) {
      metrics.incrementDatabaseErrors("countByCriteria", "users", e.getClass().getSimpleName());
      log.error("Failed to count users by criteria in H2 database: {}", criteria, e);
      throw e;
    } finally {
      metrics.recordDatabaseDuration(timer, "countByCriteria", "users");
    }
  }

  /**
   * Build Pageable with sorting support for JPA.
   */
  private Pageable buildPageable(UserQueryCriteria criteria) {
    PageRequest pageRequest = PageRequest.of(
        criteria.getPage(),
        criteria.getSize()
    );
    
    List<SortCriterion> sortCriteria = criteria.getSortCriteria();
    if (sortCriteria != null && !sortCriteria.isEmpty()) {
      List<Sort.Order> orders = sortCriteria.stream()
          .filter(SortCriterion::isValid)
          .map(sc -> {
            Sort.Direction sortDirection = 
                "desc".equalsIgnoreCase(sc.getDirection()) ? 
                Sort.Direction.DESC : 
                Sort.Direction.ASC;
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