package com.example.control.infrastructure.adapter.persistence.mongo.adapter;

import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.criteria.FailedHeartbeatCriteria;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import com.example.control.domain.port.repository.FailedHeartbeatRepositoryPort;
import com.example.control.infrastructure.adapter.persistence.mongo.repository.FailedHeartbeatMongoRepository;
import com.example.control.infrastructure.adapter.persistence.mongo.documents.FailedHeartbeatDocument;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB adapter for {@link FailedHeartbeatRepositoryPort}.
 */
@Slf4j
@Component
public class FailedHeartbeatMongoAdapter
        extends AbstractMongoAdapter<FailedHeartbeat, FailedHeartbeatDocument, FailedHeartbeatId, FailedHeartbeatCriteria, FailedHeartbeatMongoRepository>
        implements FailedHeartbeatRepositoryPort {

    public FailedHeartbeatMongoAdapter(FailedHeartbeatMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate, FailedHeartbeatId::id);
    }

    @Override
    protected FailedHeartbeatDocument toDocument(FailedHeartbeat domain) {
        return FailedHeartbeatDocument.fromDomain(domain);
    }

    @Override
    protected FailedHeartbeat toDomain(FailedHeartbeatDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(FailedHeartbeatCriteria criteria) {
        Query query = new Query();
        if (criteria == null) {
            return query;
        }

        // Apply filters
        if (criteria.serviceName() != null && !criteria.serviceName().isBlank()) {
            query.addCriteria(Criteria.where("serviceName").is(criteria.serviceName()));
        }
        if (criteria.instanceId() != null && !criteria.instanceId().isBlank()) {
            query.addCriteria(Criteria.where("instanceId").is(criteria.instanceId()));
        }
        if (criteria.status() != null) {
            query.addCriteria(Criteria.where("status").is(criteria.status().name()));
        }
        if (criteria.teamId() != null && !criteria.teamId().isBlank()) {
            query.addCriteria(Criteria.where("teamId").is(criteria.teamId()));
        }
        if (criteria.firstSeenAtFrom() != null) {
            query.addCriteria(Criteria.where("firstSeenAt").gte(criteria.firstSeenAtFrom()));
        }
        if (criteria.firstSeenAtTo() != null) {
            query.addCriteria(Criteria.where("firstSeenAt").lte(criteria.firstSeenAtTo()));
        }

        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("teamId").in(criteria.userTeamIds()));
        }

        return query;
    }

    @Override
    protected String getCollectionName() {
        return "failed_heartbeats";
    }

    @Override
    protected Class<FailedHeartbeatDocument> getDocumentClass() {
        return FailedHeartbeatDocument.class;
    }

    @Override
    public Optional<FailedHeartbeat> findByServiceNameAndInstanceId(String serviceName, String instanceId) {
        log.debug("Finding failed heartbeat by serviceName: {} and instanceId: {}", serviceName, instanceId);
        
        Optional<FailedHeartbeatDocument> document = repository.findByServiceNameAndInstanceId(serviceName, instanceId);
        Optional<FailedHeartbeat> result = document.map(this::toDomain);
        
        log.debug("Found failed heartbeat: {}", result.isPresent());
        return result;
    }

    @Override
    public long countByStatus(FailedHeartbeat.FailedHeartbeatStatus status) {
        log.debug("Counting failed heartbeats by status: {}", status);
        long count = repository.countByStatus(status.name());
        log.debug("Count: {}", count);
        return count;
    }

    @Override
    public long bulkUpdateStatus(List<FailedHeartbeatId> ids, FailedHeartbeat.FailedHeartbeatStatus status, String resolvedBy) {
        if (ids == null || ids.isEmpty()) {
            log.debug("Empty IDs list, skipping bulk update");
            return 0;
        }

        log.debug("Bulk updating status for {} failed heartbeats to {}", ids.size(), status);

        List<String> documentIds = ids.stream()
                .map(FailedHeartbeatId::id)
                .toList();

        Query query = Query.query(Criteria.where("_id").in(documentIds));
        
        Update update = new Update()
                .set("status", status.name())
                .set("resolvedAt", Instant.now())
                .set("resolvedBy", resolvedBy);

        UpdateResult result = mongoTemplate.updateMulti(
                query, update, FailedHeartbeatDocument.class, getCollectionName());

        log.info("Bulk updated {} failed heartbeats to status {}", result.getModifiedCount(), status);
        return result.getModifiedCount();
    }
}

