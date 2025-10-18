package com.example.control.domain.port;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Generic base repository port interface providing standard CRUD operations.
 * <p>
 * This interface defines the common contract for all repository ports in the system.
 * Each specific repository port should extend this interface with appropriate generic types.
 * </p>
 * 
 * @param <T> the domain entity type
 * @param <ID> the entity identifier type
 */
public interface RepositoryPort<T, ID> {

    /**
     * Save or update an entity.
     * <p>
     * If the entity has a null ID, it will be treated as a new entity and inserted.
     * If the entity has a non-null ID, it will be updated if it exists.
     *
     * @param entity the entity to save
     * @return the saved entity with updated fields (e.g., generated ID, timestamps)
     */
    T save(T entity);

    /**
     * Delete an entity by its identifier.
     *
     * @param id the identifier of the entity to delete
     */
    void deleteById(ID id);

    /**
     * Check if an entity exists by its identifier.
     *
     * @param id the identifier to check
     * @return true if the entity exists, false otherwise
     */
    boolean existsById(ID id);

    /**
     * Find an entity by its identifier.
     *
     * @param id the identifier to search for
     * @return an Optional containing the entity if found, empty otherwise
     */
    Optional<T> findById(ID id);

    /**
     * Find all entities matching the given filter criteria with pagination.
     * <p>
     * The filter object should contain the search criteria. Each repository implementation
     * should define its own filter record type that extends or contains common filtering
     * capabilities like team-based access control.
     *
     * @param filter the filter criteria (can be null for no filtering)
     * @param pageable pagination and sorting information
     * @return a page of matching entities
     */
    Page<T> findAll(Object filter, Pageable pageable);

    /**
     * Count entities matching the given filter criteria.
     * <p>
     * This method should use the same filtering logic as findAll() to ensure
     * consistent results.
     *
     * @param filter the filter criteria (can be null for no filtering)
     * @return the count of matching entities
     */
    long count(Object filter);
}
