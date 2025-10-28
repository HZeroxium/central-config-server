package com.example.control.infrastructure.mongo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

import jakarta.annotation.PostConstruct;

/**
 * Creates MongoDB indexes for approval workflow collections.
 * Ensures data integrity and enforces business rules via partial unique indexes.
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
        IndexOperations ops = mongoTemplate.indexOps("approval_requests");

        // Supportive compound index for queries: (targetServiceId, status)
        try {
            ops.ensureIndex(new Index()
                    .on("targetServiceId", org.springframework.data.domain.Sort.Direction.ASC)
                    .on("status", org.springframework.data.domain.Sort.Direction.ASC)
            );
            log.info("Ensured index on approval_requests(targetServiceId, status)");
        } catch (Exception e) {
            log.warn("Failed ensuring non-unique index for approval_requests: {}", e.getMessage());
        }

        // Unique partial: (requesterUserId, targetServiceId, status='PENDING')
        try {
            Index pendingUnique = new Index()
                    .on("requesterUserId", org.springframework.data.domain.Sort.Direction.ASC)
                    .on("targetServiceId", org.springframework.data.domain.Sort.Direction.ASC)
                    .on("status", org.springframework.data.domain.Sort.Direction.ASC)
                    .unique()
                    .partial(PartialIndexFilter.of(Criteria.where("status").is("PENDING")))
                    .named("uniq_user_service_pending");
            ops.ensureIndex(pendingUnique);
            log.info("Ensured unique partial index uniq_user_service_pending");
        } catch (Exception e) {
            log.warn("Failed ensuring unique partial index (PENDING): {}", e.getMessage());
        }

        // Unique partial: (targetServiceId, status='APPROVED')
        try {
            Index approvedUnique = new Index()
                    .on("targetServiceId", org.springframework.data.domain.Sort.Direction.ASC)
                    .on("status", org.springframework.data.domain.Sort.Direction.ASC)
                    .unique()
                    .partial(PartialIndexFilter.of(Criteria.where("status").is("APPROVED")))
                    .named("uniq_service_approved");
            ops.ensureIndex(approvedUnique);
            log.info("Ensured unique partial index uniq_service_approved");
        } catch (Exception e) {
            log.warn("Failed ensuring unique partial index (APPROVED): {}", e.getMessage());
        }
    }
}


