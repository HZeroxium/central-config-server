package com.example.control.infrastructure.resilience.persistence;

import com.example.control.infrastructure.resilience.ResilienceDecoratorsFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Wrapper service for protecting MongoDB write operations with resilience
 * patterns.
 * <p>
 * Provides Circuit Breaker + Bulkhead + TimeLimiter protection for critical
 * MongoDB write operations (save, saveAll, delete, deleteAll).
 * </p>
 * <p>
 * Note: No retry for write operations (idempotency concern).
 * </p>
 * <p>
 * Usage:
 * 
 * <pre>
 * ApplicationServiceDocument saved = resilientMongoOperation.save(
 *     repository, document, null);
 * </pre>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientMongoRepository {

  private static final String SERVICE_NAME = "mongodb";

  private final ResilienceDecoratorsFactory resilienceFactory;

  /**
   * Save entity with resilience protection.
   *
   * @param repository MongoRepository instance
   * @param entity     Entity to save
   * @param fallback   Fallback value (null if operation should fail)
   * @param <T>        Entity type
   * @param <ID>       ID type
   * @return Saved entity
   */
  public <T, ID> T save(MongoRepository<T, ID> repository, T entity, T fallback) {
    Supplier<T> saveOperation = () -> repository.save(entity);
    return resilienceFactory.decorateSupplierWithoutRetry(SERVICE_NAME, saveOperation, fallback)
        .get();
  }

  /**
   * Save all entities with resilience protection.
   *
   * @param repository MongoRepository instance
   * @param entities   Entities to save
   * @param fallback   Fallback value (null if operation should fail)
   * @param <T>        Entity type
   * @param <ID>       ID type
   * @return Saved entities
   */
  public <T, ID> List<T> saveAll(
      MongoRepository<T, ID> repository,
      Collection<T> entities,
      List<T> fallback) {
    Supplier<List<T>> saveAllOperation = () -> repository.saveAll(entities);
    return resilienceFactory.decorateSupplierWithoutRetry(SERVICE_NAME, saveAllOperation, fallback)
        .get();
  }

  /**
   * Delete entity with resilience protection.
   *
   * @param repository MongoRepository instance
   * @param entity     Entity to delete
   * @param <T>        Entity type
   * @param <ID>       ID type
   */
  public <T, ID> void delete(MongoRepository<T, ID> repository, T entity) {
    Supplier<Void> deleteOperation = () -> {
      repository.delete(entity);
      return null;
    };
    resilienceFactory.decorateSupplierWithoutRetry(SERVICE_NAME, deleteOperation, null).get();
  }

  /**
   * Delete entity by ID with resilience protection.
   *
   * @param repository MongoRepository instance
   * @param id         Entity ID
   * @param <T>        Entity type
   * @param <ID>       ID type
   */
  public <T, ID> void deleteById(MongoRepository<T, ID> repository, ID id) {
    Supplier<Void> deleteOperation = () -> {
      repository.deleteById(id);
      return null;
    };
    resilienceFactory.decorateSupplierWithoutRetry(SERVICE_NAME, deleteOperation, null).get();
  }

  /**
   * Delete all entities with resilience protection.
   *
   * @param repository MongoRepository instance
   * @param entities   Entities to delete
   * @param <T>        Entity type
   * @param <ID>       ID type
   */
  public <T, ID> void deleteAll(MongoRepository<T, ID> repository, Collection<T> entities) {
    Supplier<Void> deleteAllOperation = () -> {
      repository.deleteAll(entities);
      return null;
    };
    resilienceFactory.decorateSupplierWithoutRetry(SERVICE_NAME, deleteAllOperation, null).get();
  }
}
