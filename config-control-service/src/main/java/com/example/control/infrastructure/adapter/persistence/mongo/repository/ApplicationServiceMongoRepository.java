package com.example.control.infrastructure.adapter.persistence.mongo.repository;

import com.example.control.infrastructure.adapter.persistence.mongo.documents.ApplicationServiceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link ApplicationServiceDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for application services.
 * </p>
 */
@Repository
public interface ApplicationServiceMongoRepository extends MongoRepository<ApplicationServiceDocument, String> {
    /**
     * Find application service by exact display name.
     * <p>
     * This method provides O(1) lookup for service name resolution during heartbeat
     * processing.
     * Should be indexed for optimal performance.
     *
     * @param displayName the exact display name to search for
     * @return the application service if found, empty otherwise
     */
    Optional<ApplicationServiceDocument> findByDisplayName(String displayName);
}
