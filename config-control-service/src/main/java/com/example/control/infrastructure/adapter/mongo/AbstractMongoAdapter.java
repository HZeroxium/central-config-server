package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.port.RepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Abstract base adapter providing common MongoDB operations with type safety.
 * <p>
 * This class implements the standard CRUD operations and delegates domain-specific
 * operations to subclasses. It handles the common pattern of converting between
 * domain entities and MongoDB documents.
 * </p>
 *
 * @param <T> the domain entity type
 * @param <D> the MongoDB document type
 * @param <ID> the entity identifier type
 * @param <F> the filter criteria type
 */
@Slf4j
public abstract class AbstractMongoAdapter<T, D, ID, F> implements RepositoryPort<T, ID, F> {

    protected final MongoRepository<D, String> repository;
    protected final MongoTemplate mongoTemplate;

    /**
     * Constructor for the abstract adapter.
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for complex queries
     */
    protected AbstractMongoAdapter(MongoRepository<D, String> repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Convert domain entity to MongoDB document.
     *
     * @param domain the domain entity
     * @return the MongoDB document
     */
    protected abstract D toDocument(T domain);

    /**
     * Convert MongoDB document to domain entity.
     *
     * @param document the MongoDB document
     * @return the domain entity
     */
    protected abstract T toDomain(D document);

    /**
     * Build MongoDB query from filter criteria.
     *
     * @param filter the filter criteria
     * @return the MongoDB query
     */
    protected abstract Query buildQuery(F filter);

    /**
     * Get the collection name for this entity.
     *
     * @return the collection name
     */
    protected abstract String getCollectionName();

    @Override
    public T save(T entity) {
        log.debug("Saving entity: {}", entity);
        D document = toDocument(entity);
        D savedDocument = repository.save(document);
        T result = toDomain(savedDocument);
        log.debug("Saved entity: {}", result);
        return result;
    }

    @Override
    public Optional<T> findById(ID id) {
        log.debug("Finding entity by ID: {}", id);
        String documentId = convertIdToDocumentId(id);
        Optional<D> document = repository.findById(documentId);
        Optional<T> result = document.map(this::toDomain);
        log.debug("Found entity: {}", result.isPresent());
        return result;
    }

    @Override
    public boolean existsById(ID id) {
        log.debug("Checking existence of entity with ID: {}", id);
        String documentId = convertIdToDocumentId(id);
        boolean exists = repository.existsById(documentId);
        log.debug("Entity exists: {}", exists);
        return exists;
    }

    @Override
    public void deleteById(ID id) {
        log.debug("Deleting entity with ID: {}", id);
        String documentId = convertIdToDocumentId(id);
        repository.deleteById(documentId);
        log.debug("Deleted entity with ID: {}", id);
    }

    @Override
    public Page<T> findAll(F filter, Pageable pageable) {
        log.debug("Finding entities with filter: {}, pageable: {}", filter, pageable);
        
        Query query = buildQuery(filter);
        
        // Apply pagination
        query.with(pageable);
        
        // Execute query
        List<D> documents = mongoTemplate.find(query, getDocumentClass(), getCollectionName());
        
        // Count total for pagination
        Query countQuery = buildQuery(filter);
        long total = mongoTemplate.count(countQuery, getDocumentClass(), getCollectionName());
        
        // Convert to domain entities
        List<T> entities = documents.stream()
                .map(this::toDomain)
                .toList();
        
        Page<T> result = new PageImpl<>(entities, pageable, total);
        log.debug("Found {} entities out of {} total", entities.size(), total);
        return result;
    }

    @Override
    public long count(F filter) {
        log.debug("Counting entities with filter: {}", filter);
        
        Query query = buildQuery(filter);
        long count = mongoTemplate.count(query, getDocumentClass(), getCollectionName());
        
        log.debug("Counted {} entities", count);
        return count;
    }

    /**
     * Convert entity ID to MongoDB document ID.
     * <p>
     * This method handles the conversion from domain ID value objects to
     * the String format used by MongoDB. Subclasses can override this
     * for custom ID conversion logic.
     *
     * @param id the domain entity ID
     * @return the MongoDB document ID
     */
    protected String convertIdToDocumentId(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return id.toString();
    }

    /**
     * Get the document class for MongoDB operations.
     * <p>
     * This is used by MongoTemplate for type-safe operations.
     * Subclasses must return their specific document class.
     *
     * @return the document class
     */
    protected abstract Class<D> getDocumentClass();

    /**
     * Build a page from documents and pagination info.
     * <p>
     * Helper method for constructing Page objects from MongoDB results.
     *
     * @param documents the MongoDB documents
     * @param pageable the pagination info
     * @param total the total count
     * @return a page of domain entities
     */
    protected Page<T> buildPage(List<D> documents, Pageable pageable, long total) {
        List<T> entities = documents.stream()
                .map(this::toDomain)
                .toList();
        return new PageImpl<>(entities, pageable, total);
    }
}
