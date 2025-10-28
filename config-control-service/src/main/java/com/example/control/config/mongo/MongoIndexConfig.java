package com.example.control.config.mongo;

import com.example.control.infrastructure.mongo.documents.ApprovalRequestDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * MongoDB index configuration for performance optimization.
 * <p>
 * Creates compound indexes for approval requests to support:
 * - Duplicate PENDING request validation
 * - Cascade approve/reject operations
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    /**
     * Ensures required indexes exist on application startup.
     * <p>
     * This method is idempotent - indexes are created only if they don't exist.
     * </p>
     */
    @PostConstruct
    public void ensureIndexes() {
        log.info("Creating MongoDB indexes for approval requests...");
        
        try {
            // Index 1: Duplicate PENDING request check
            // Unique partial index on (requesterUserId, targetServiceId) where status = PENDING
            // This prevents duplicate PENDING requests from same user for same service
            Index duplicateCheckIndex = new Index()
                    .on("requesterUserId", Sort.Direction.ASC)
                    .on("targetServiceId", Sort.Direction.ASC)
                    .unique()
                    .named("idx_duplicate_pending_check")
                    .partial(PartialIndexFilter.of(Criteria.where("status").is("PENDING")));
            
            mongoTemplate.indexOps(ApprovalRequestDocument.class)
                    .ensureIndex(duplicateCheckIndex);
            
            log.info("Created duplicate check index: idx_duplicate_pending_check");
            
            // Index 2: Cascade operations
            // Compound index on (targetServiceId, targetTeamId, status)
            // Supports cascade approve (same team) and cascade reject (different teams)
            Index cascadeIndex = new Index()
                    .on("targetServiceId", Sort.Direction.ASC)
                    .on("targetTeamId", Sort.Direction.ASC)
                    .on("status", Sort.Direction.ASC)
                    .named("idx_cascade_operations");
            
            mongoTemplate.indexOps(ApprovalRequestDocument.class)
                    .ensureIndex(cascadeIndex);
            
            log.info("Created cascade operations index: idx_cascade_operations");
            
            log.info("MongoDB indexes created successfully");
            
        } catch (Exception e) {
            log.error("Failed to create MongoDB indexes", e);
            // Don't fail application startup if index creation fails
            // Indexes can be created manually if needed
        }
    }
}

