package com.example.control.infrastructure.config.persistence.mongo;

import com.example.control.infrastructure.adapter.persistence.mongo.documents.ApplicationServiceDocument;
import com.example.control.infrastructure.adapter.persistence.mongo.documents.ApprovalRequestDocument;
import com.example.control.infrastructure.adapter.persistence.mongo.documents.DriftEventDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;

import jakarta.annotation.PostConstruct;

/**
 * Consolidated MongoDB index configuration for all collections.
 * <p>
 * Ensures data integrity and enforces business rules via partial unique
 * indexes.
 * Creates compound indexes for optimal query performance.
 * </p>
 */
@Slf4j
@Configuration
public class MongoIndexesConfig {

    private final MongoTemplate mongoTemplate;

    public MongoIndexesConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void ensureIndexes() {
        ensureApprovalRequestIndexes();
        ensureApplicationServiceIndexes();
        ensureDriftEventIndexes();
    }

    /**
     * Ensures indexes for approval_requests collection.
     * <p>
     * Creates indexes to support:
     * - Duplicate PENDING request validation (unique partial index)
     * - Cascade approve/reject operations (compound index)
     * - Service-based queries (compound index)
     * - Gate queries (index on requiredGates array)
     * </p>
     */
    private void ensureApprovalRequestIndexes() {
        log.info("Creating MongoDB indexes for approval_requests collection...");
        IndexOperations ops = mongoTemplate.indexOps(ApprovalRequestDocument.class);

        try {
            // Index 1: Compound index for service-based queries
            // Supports queries filtering by serviceId and status
            Index serviceStatusIndex = new Index()
                    .on("targetServiceId", Sort.Direction.ASC)
                    .on("status", Sort.Direction.ASC)
                    .named("idx_service_status");
            ops.ensureIndex(serviceStatusIndex);
            log.info("Created compound index: idx_service_status");
        } catch (Exception e) {
            log.warn("Failed ensuring index idx_service_status: {}", e.getMessage());
        }

        try {
            // Index 2: Compound index for cascade operations
            // Supports cascade approve (same team) and cascade reject (different teams)
            Index cascadeIndex = new Index()
                    .on("targetServiceId", Sort.Direction.ASC)
                    .on("targetTeamId", Sort.Direction.ASC)
                    .on("status", Sort.Direction.ASC)
                    .named("idx_cascade_operations");
            ops.ensureIndex(cascadeIndex);
            log.info("Created compound index: idx_cascade_operations");
        } catch (Exception e) {
            log.warn("Failed ensuring index idx_cascade_operations: {}", e.getMessage());
        }

        try {
            // Index 3: Unique partial index for duplicate PENDING request check
            // Prevents duplicate PENDING requests from same user for same service
            // Note: status field not included in index as partial filter already restricts
            // to PENDING
            Index duplicateCheckIndex = new Index()
                    .on("requesterUserId", Sort.Direction.ASC)
                    .on("targetServiceId", Sort.Direction.ASC)
                    .unique()
                    .partial(PartialIndexFilter.of(Criteria.where("status").is("PENDING")))
                    .named("idx_duplicate_pending_check");
            ops.ensureIndex(duplicateCheckIndex);
            log.info("Created unique partial index: idx_duplicate_pending_check");
        } catch (Exception e) {
            log.warn("Failed ensuring unique partial index idx_duplicate_pending_check: {}", e.getMessage());
        }

        try {
            // Index 4: Index on requiredGates array for efficient gate queries
            // Supports queries filtering by gate names using $in operator
            Index gatesIndex = new Index()
                    .on("requiredGates", Sort.Direction.ASC)
                    .named("idx_required_gates");
            ops.ensureIndex(gatesIndex);
            log.info("Created index on requiredGates array: idx_required_gates");
        } catch (Exception e) {
            log.warn("Failed ensuring index idx_required_gates: {}", e.getMessage());
        }

        log.info("Completed creating indexes for approval_requests collection");
    }

    /**
     * Ensures indexes for application_services collection.
     * <p>
     * Creates indexes to support:
     * - Text search on displayName (full-text search)
     * - Other existing compound indexes are defined via annotations
     * </p>
     */
    private void ensureApplicationServiceIndexes() {
        log.info("Creating MongoDB indexes for application_services collection...");
        IndexOperations ops = mongoTemplate.indexOps(ApplicationServiceDocument.class);

        try {
            // Text index for full-text search on displayName
            // Supports efficient text search queries instead of regex
            TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("displayName")
                    .build();
            ops.ensureIndex(textIndex);
            log.info("Created text index on displayName for full-text search");
        } catch (Exception e) {
            log.warn("Failed ensuring text index on displayName: {}", e.getMessage());
        }

        log.info("Completed creating indexes for application_services collection");
    }

    /**
     * Ensures indexes for drift_events collection.
     * <p>
     * Creates a text index to allow efficient text search on the serviceName field.
     * </p>
     */
    private void ensureDriftEventIndexes() {
        log.info("Creating MongoDB indexes for drift_events collection...");
        IndexOperations ops = mongoTemplate.indexOps(DriftEventDocument.class);

        try {
            // Text index for full-text search on serviceName
            TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("serviceName")
                    .build();
            ops.ensureIndex(textIndex);
            log.info("Created text index on serviceName for full-text search in drift_events");
        } catch (Exception e) {
            log.warn("Failed ensuring text index on serviceName in drift_events: {}", e.getMessage());
        }

        log.info("Completed creating indexes for drift_events collection");
    }
}
