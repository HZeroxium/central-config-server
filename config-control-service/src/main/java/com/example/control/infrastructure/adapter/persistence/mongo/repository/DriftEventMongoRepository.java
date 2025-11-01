package com.example.control.infrastructure.adapter.persistence.mongo.repository;

import com.example.control.infrastructure.adapter.persistence.mongo.documents.DriftEventDocument;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link DriftEventDocument} entities.
 * <p>
 * Provides data access operations for drift detection, tracking, and reporting.
 */
@Repository
public interface DriftEventMongoRepository extends MongoRepository<DriftEventDocument, String> {

    /**
     * Counts all drift events matching a specific status.
     *
     * @param status drift event status (DETECTED, RESOLVED, etc.)
     * @return total count
     */
    long countByStatus(String status);
}
