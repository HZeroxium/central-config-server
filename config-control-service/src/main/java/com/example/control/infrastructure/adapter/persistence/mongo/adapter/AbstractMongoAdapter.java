package com.example.control.infrastructure.adapter.persistence.mongo.adapter;

import com.example.control.domain.port.RepositoryPort;
import com.mongodb.client.result.UpdateResult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract base adapter providing common MongoDB operations with type safety.
 * <p>
 * This class implements the standard CRUD operations and delegates
 * domain-specific
 * operations to subclasses. It handles the common pattern of converting between
 * domain entities and MongoDB documents.
 * </p>
 *
 * @param <T>  the domain entity type
 * @param <D>  the MongoDB document type
 * @param <ID> the entity identifier type
 * @param <F>  the filter criteria type
 * @param <R>  the MongoDB repository type that extends MongoRepository<D,
 *             String>
 */
@Slf4j
public abstract class AbstractMongoAdapter<T, D, ID, F, R extends MongoRepository<D, String>>
        implements RepositoryPort<T, ID, F> {

    protected final R repository;
    protected final MongoTemplate mongoTemplate;
    private final Function<ID, String> idMapper;

    /**
     * Constructor for the abstract adapter.
     *
     * @param repository    the MongoDB repository
     * @param mongoTemplate the MongoDB template for complex queries
     * @param idMapper      the function to convert the domain ID to a MongoDB
     *                      document ID
     */
    protected AbstractMongoAdapter(R repository, MongoTemplate mongoTemplate, Function<ID, String> idMapper) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.idMapper = idMapper;
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
        log.debug("Saving entity: {} to collection: {}", entity, getCollectionName());

        // Convert domain entity to MongoDB document
        D document = toDocument(entity);

        // Save MongoDB document
        D savedDocument = repository.save(document);

        // Convert MongoDB document to domain entity
        T result = toDomain(savedDocument);

        log.debug("Saved entity: {} to collection: {}", result, getCollectionName());

        return result;
    }

    @Override
    public Optional<T> findById(ID id) {
        log.debug("Finding entity by ID: {} in collection: {}", id, getCollectionName());

        // Convert domain entity ID to MongoDB document ID
        String documentId = convertIdToDocumentId(id);

        // Find MongoDB document by ID
        Optional<D> document = repository.findById(documentId);

        // Convert MongoDB document to domain entity
        Optional<T> result = document.map(this::toDomain);

        log.debug("Found entity: {} in collection: {}", result.isPresent(), getCollectionName());
        return result;
    }

    @Override
    public boolean existsById(ID id) {
        log.debug("Checking existence of entity with ID: {} in collection: {}", id, getCollectionName());

        // Convert domain entity ID to MongoDB document ID
        String documentId = convertIdToDocumentId(id);

        // Check if MongoDB document exists by ID
        boolean exists = repository.existsById(documentId);

        log.debug("Entity exists: {} in collection: {}", exists, getCollectionName());
        return exists;
    }

    @Override
    public void deleteById(ID id) {
        log.debug("Deleting entity with ID: {} in collection: {}", id, getCollectionName());

        // Convert domain entity ID to MongoDB document ID
        String documentId = convertIdToDocumentId(id);

        // Delete MongoDB document by ID
        repository.deleteById(documentId);

        log.debug("Deleted entity with ID: {} in collection: {}", id, getCollectionName());
    }

    @Override
    public Page<T> findAll(F filter, Pageable pageable) {
        log.debug("Finding entities with filter: {}, pageable: {} in collection: {}", filter, pageable, getCollectionName());

        // Build MongoDB query from filter criteria
        Query query = buildQuery(filter);

        // Apply pagination
        query.with(pageable);

        // Execute query
        List<D> documents = mongoTemplate.find(query, getDocumentClass(), getCollectionName());

        // Count total for pagination
        Query countQuery = buildQuery(filter);

        // Count total for pagination
        long total = mongoTemplate.count(countQuery, getDocumentClass(), getCollectionName());

        // Convert to domain entities
        List<T> entities = documents.stream()
                .map(this::toDomain)
                .toList();

        // Build page from documents and pagination info
        Page<T> result = new PageImpl<>(entities, pageable, total);

        log.debug("Found {} entities out of {} total in collection: {}", entities.size(), total, getCollectionName());
        return result;
    }

    @Override
    public long count(F filter) {
        log.debug("Counting entities with filter: {} in collection: {}", filter, getCollectionName());

        // Build MongoDB query from filter criteria
        Query query = buildQuery(filter);

        // Count total for pagination
        long count = mongoTemplate.count(query, getDocumentClass(), getCollectionName());

        log.debug("Counted {} entities in collection: {}", count, getCollectionName());
        return count;
    }

    @Override
    public long deleteAll() {
        log.warn("Deleting ALL entities in collection: {}", getCollectionName());

        long count = repository.count();
        repository.deleteAll();

        log.warn("Deleted {} entities in collection: {}", count, getCollectionName());
        return count;
    }

    /**
     * Convert entity ID to MongoDB document ID.
     * <p>
     * This method handles the conversion from domain ID value objects to
     * the String format used by MongoDB. All IDs are now String type for
     * consistency.
     * Subclasses can override this for custom ID conversion logic.
     *
     * @param id the domain entity ID
     * @return the MongoDB document ID
     */
    protected String convertIdToDocumentId(ID id) {
        if (id == null) {
            return null;
        }

        // Use the provided mapper to convert domain ID to String
        return idMapper.apply(id);
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
     * @param pageable  the pagination info
     * @param total     the total count
     * @return a page of domain entities
     */
    protected Page<T> buildPage(List<D> documents, Pageable pageable, long total) {
        List<T> entities = documents.stream()
                .map(this::toDomain)
                .toList();
        return new PageImpl<>(entities, pageable, total);
    }

    /**
     * Bulk update teamId for all documents matching the given serviceId.
     * <p>
     * This method is used for ownership transfer scenarios where we need to
     * update teamId across related collections efficiently.
     *
     * @param serviceId the service ID to match
     * @param newTeamId the new team ID to set
     * @return number of documents updated
     */
    protected long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
        log.debug("Bulk updating teamId to {} for serviceId: {} in collection: {}", newTeamId, serviceId, getCollectionName());

        Query query = new Query(org.springframework.data.mongodb.core.query.Criteria.where("serviceId").is(serviceId));
        Update update = new Update()
                .set("teamId", newTeamId)
                .set("updatedAt", Instant.now());

        UpdateResult result = mongoTemplate.updateMulti(
                query, update, getDocumentClass());

        log.debug("Bulk updated {} documents for serviceId: {} in collection: {}", result.getModifiedCount(), serviceId, getCollectionName());
        return result.getModifiedCount();
    }
}
