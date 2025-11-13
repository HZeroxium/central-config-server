package com.example.control.infrastructure.adapter.persistence.mongo.adapter;

import com.example.control.domain.model.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.example.control.domain.port.repository.ServiceInstanceRepositoryPort;
import com.example.control.infrastructure.adapter.persistence.mongo.repository.ServiceInstanceMongoRepository;
import com.example.control.infrastructure.adapter.persistence.mongo.documents.ServiceInstanceDocument;
import com.mongodb.bulk.BulkWriteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MongoDB adapter implementing {@link ServiceInstanceRepositoryPort} using
 * Spring Data and MongoTemplate.
 */
@Slf4j
@Component
public class ServiceInstanceMongoAdapter
        extends
        AbstractMongoAdapter<ServiceInstance, ServiceInstanceDocument, ServiceInstanceId, ServiceInstanceCriteria, ServiceInstanceMongoRepository>
        implements ServiceInstanceRepositoryPort {

    public ServiceInstanceMongoAdapter(ServiceInstanceMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate, ServiceInstanceId::instanceId);
    }

    @Override
    protected ServiceInstanceDocument toDocument(ServiceInstance domain) {
        return ServiceInstanceDocument.fromDomain(domain);
    }

    @Override
    protected ServiceInstance toDomain(ServiceInstanceDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(ServiceInstanceCriteria criteria) {
        Query query = new Query();

        if (criteria == null)
            return query;

        // Apply filters
        if (criteria.serviceId() != null && !criteria.serviceId().isBlank()) {
            query.addCriteria(Criteria.where("serviceId").is(criteria.serviceId()));
        }
        if (criteria.instanceId() != null && !criteria.instanceId().isBlank()) {
            query.addCriteria(Criteria.where("_id").is(criteria.instanceId()));
        }
        if (criteria.status() != null)
            query.addCriteria(Criteria.where("status").is(criteria.status().name()));
        if (criteria.hasDrift() != null) {
            query.addCriteria(Criteria.where("hasDrift").is(criteria.hasDrift()));
        }
        if (criteria.environment() != null && !criteria.environment().isBlank()) {
            query.addCriteria(Criteria.where("environment").is(criteria.environment()));
        }
        if (criteria.version() != null && !criteria.version().isBlank()) {
            query.addCriteria(Criteria.where("version").is(criteria.version()));
        }
        if (criteria.lastSeenAtFrom() != null) {
            query.addCriteria(Criteria.where("lastSeenAt").gte(criteria.lastSeenAtFrom()));
        }
        if (criteria.lastSeenAtTo() != null) {
            query.addCriteria(Criteria.where("lastSeenAt").lte(criteria.lastSeenAtTo()));
        }

        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("teamId").in(criteria.userTeamIds()));
        }

        return query;
    }

    @Override
    protected String getCollectionName() {
        return "service_instances";
    }

    @Override
    public long countByServiceId(String serviceId) {
        return repository.countByServiceId(serviceId);
    }

    @Override
    public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
        return super.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
    }

    @Override
    protected Class<ServiceInstanceDocument> getDocumentClass() {
        return ServiceInstanceDocument.class;
    }

    @Override
    public BulkWriteResult bulkUpsert(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            log.debug("Empty instances list, skipping bulk upsert");
            return null; // Return null for empty list (caller should handle)
        }

        log.debug("Bulk upserting {} service instances", instances.size());

        BulkOperations bulkOps = mongoTemplate.bulkOps(
                BulkOperations.BulkMode.UNORDERED,
                ServiceInstanceDocument.class);

        Instant now = Instant.now();
        for (ServiceInstance instance : instances) {
            ServiceInstanceDocument doc = toDocument(instance);
            Query query = Query.query(Criteria.where("_id").is(instance.getId().instanceId()));
            Update update = new Update()
                    .set("serviceId", doc.getServiceId())
                    .set("teamId", doc.getTeamId())
                    .set("host", doc.getHost())
                    .set("port", doc.getPort())
                    .set("environment", doc.getEnvironment())
                    .set("version", doc.getVersion())
                    .set("configHash", doc.getConfigHash())
                    .set("lastAppliedHash", doc.getLastAppliedHash())
                    .set("expectedHash", doc.getExpectedHash())
                    .set("status", doc.getStatus())
                    .set("hasDrift", doc.getHasDrift())
                    .set("driftDetectedAt", doc.getDriftDetectedAt())
                    .set("lastSeenAt", doc.getLastSeenAt())
                    .set("updatedAt", now)
                    .set("metadata", doc.getMetadata())
                    .setOnInsert("createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt() : now);

            bulkOps.upsert(query, update);
        }

        BulkWriteResult result = bulkOps.execute();
        log.debug("Bulk upsert completed: {} inserted, {} modified, {} matched",
                result.getInsertedCount(), result.getModifiedCount(), result.getMatchedCount());

        return result;
    }

    @Override
    public List<ServiceInstance> findAllByIds(Set<ServiceInstanceId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        log.debug("Finding {} service instances by IDs", ids.size());

        // Extract instanceId strings from ServiceInstanceId set
        List<String> instanceIdStrings = ids.stream()
                .map(ServiceInstanceId::instanceId)
                .collect(Collectors.toList());

        Query query = Query.query(Criteria.where("_id").in(instanceIdStrings));
        List<ServiceInstanceDocument> documents = mongoTemplate.find(query, ServiceInstanceDocument.class);

        List<ServiceInstance> instances = documents.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        log.debug("Found {} service instances out of {} requested", instances.size(), ids.size());
        return instances;
    }
}
