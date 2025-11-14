package com.example.control.infrastructure.adapter.persistence.mongo.adapter;

import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.valueobject.id.DriftEventId;
import com.example.control.domain.port.repository.DriftEventRepositoryPort;
import com.example.control.infrastructure.adapter.persistence.mongo.repository.DriftEventMongoRepository;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import com.example.control.infrastructure.adapter.persistence.mongo.documents.DriftEventDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB adapter for {@link DriftEventRepositoryPort}.
 */
@Slf4j
@Component
public class DriftEventMongoAdapter
        extends
        AbstractMongoAdapter<DriftEvent, DriftEventDocument, DriftEventId, DriftEventCriteria, DriftEventMongoRepository>
        implements DriftEventRepositoryPort {

    public DriftEventMongoAdapter(DriftEventMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate, DriftEventId::id);
    }

    @Override
    protected DriftEventDocument toDocument(DriftEvent domain) {
        return DriftEventDocument.fromDomain(domain);
    }

    @Override
    protected DriftEvent toDomain(DriftEventDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(DriftEventCriteria criteria) {
        Query query = new Query();
        if (criteria == null)
            return query;

        // Apply filters
        if (criteria.instanceId() != null && !criteria.instanceId().isBlank()) {
            query.addCriteria(Criteria.where("instanceId").is(criteria.instanceId()));
        }
        if (criteria.status() != null) {
            query.addCriteria(Criteria.where("status").is(criteria.status().name()));
        }
        if (criteria.severity() != null) {
            query.addCriteria(Criteria.where("severity").is(criteria.severity().name()));
        }
        if (criteria.detectedAtFrom() != null) {
            query.addCriteria(Criteria.where("detectedAt").gte(criteria.detectedAtFrom()));
        }
        if (criteria.detectedAtTo() != null) {
            query.addCriteria(Criteria.where("detectedAt").lte(criteria.detectedAtTo()));
        }
        if (Boolean.TRUE.equals(criteria.unresolvedOnly())) {
            query.addCriteria(Criteria.where("status").in("DETECTED", "ACKNOWLEDGED", "RESOLVING"));
        }

        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("teamId").in(criteria.userTeamIds()));
        }

        // Text search: use MongoDB text index for efficient full-text search
        if (criteria.serviceName() != null && !criteria.serviceName().trim().isEmpty()) {
            String searchTerm = criteria.serviceName().trim();
            // Use $text search for full-text matching (requires text index)
            // This is more efficient than regex for search queries
            query.addCriteria(new Criteria()
                    .orOperator(
                            Criteria.where("serviceName")
                                    .regex(".*" + searchTerm + ".*", "i"), // Fallback regex for partial matching
                            Criteria.where("serviceName")
                                    .is(searchTerm) // Exact match
                    ));
        }
        return query;
    }

    @Override
    protected String getCollectionName() {
        return "drift_events";
    }

    @Override
    public void resolveForInstance(String serviceName, String instanceId, String resolvedBy) {
        log.debug("Resolving drift events for instance: {}:{}", serviceName, instanceId);

        Query query = new Query(
                Criteria.where("serviceName").is(serviceName)
                        .and("instanceId").is(instanceId)
                        .and("status").in("DETECTED", "ACKNOWLEDGED", "RESOLVING") // Only unresolved events
        );

        Update update = new Update()
                .set("status", DriftEvent.DriftStatus.RESOLVED.name())
                .set("resolvedAt", Instant.now())
                .set("resolvedBy", resolvedBy);

        UpdateResult result = mongoTemplate.updateMulti(
                query, update, DriftEventDocument.class, getCollectionName());

        log.info("Resolved {} drift events for instance {}:{}",
                result.getModifiedCount(), serviceName, instanceId);
    }

    @Override
    public long countByStatus(DriftEvent.DriftStatus status) {
        return repository.countByStatus(status.name());
    }

    @Override
    public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
        return super.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
    }

    @Override
    protected Class<DriftEventDocument> getDocumentClass() {
        return DriftEventDocument.class;
    }

    @Override
    public BulkWriteResult bulkSave(List<DriftEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("Empty events list, skipping bulk save");
            return null; // Return null for empty list (caller should handle)
        }

        log.debug("Bulk saving {} drift events", events.size());

        // Deduplicate events by ID to avoid conflicts in bulk write
        Map<String, DriftEvent> uniqueEvents = new LinkedHashMap<>();
        for (DriftEvent event : events) {
            // Generate ID if null (new event)
            if (event.getId() == null) {
                event.setId(DriftEventId.of(java.util.UUID.randomUUID().toString()));
            }
            // Keep the last occurrence if duplicates exist
            uniqueEvents.put(event.getId().id(), event);
        }

        if (uniqueEvents.isEmpty()) {
            log.debug("No unique events after deduplication, skipping bulk save");
            return null;
        }

        if (uniqueEvents.size() < events.size()) {
            log.warn("Deduplicated {} drift events to {} unique events", 
                    events.size(), uniqueEvents.size());
        }

        BulkOperations bulkOps = mongoTemplate.bulkOps(
                BulkOperations.BulkMode.UNORDERED,
                DriftEventDocument.class);

        Instant now = Instant.now();
        for (DriftEvent event : uniqueEvents.values()) {
            DriftEventDocument doc = toDocument(event);
            Query query = Query.query(Criteria.where("_id").is(event.getId().id()));
            Update update = new Update()
                    .set("serviceName", doc.getServiceName())
                    .set("instanceId", doc.getInstanceId())
                    .set("serviceId", doc.getServiceId())
                    .set("teamId", doc.getTeamId())
                    .set("environment", doc.getEnvironment())
                    .set("expectedHash", doc.getExpectedHash())
                    .set("appliedHash", doc.getAppliedHash())
                    .set("severity", doc.getSeverity())
                    .set("status", doc.getStatus())
                    // Removed: .set("detectedAt", doc.getDetectedAt()) - causes conflict with setOnInsert
                    // detectedAt should be immutable after creation, only set on insert
                    .set("resolvedAt", doc.getResolvedAt())
                    .set("detectedBy", doc.getDetectedBy())
                    .set("resolvedBy", doc.getResolvedBy())
                    .set("notes", doc.getNotes())
                    // Only set detectedAt on insert (immutable field)
                    .setOnInsert("detectedAt", doc.getDetectedAt() != null ? doc.getDetectedAt() : now);

            bulkOps.upsert(query, update);
        }

        BulkWriteResult result = bulkOps.execute();
        log.debug("Bulk save completed: {} inserted, {} modified, {} matched",
                result.getInsertedCount(), result.getModifiedCount(), result.getMatchedCount());

        return result;
    }
}
