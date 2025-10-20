package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.id.DriftEventId;
import com.example.control.domain.port.DriftEventRepositoryPort;
import com.example.control.infrastructure.repository.DriftEventMongoRepository;
import com.example.control.infrastructure.repository.documents.DriftEventDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * MongoDB adapter for {@link DriftEventRepositoryPort}.
 */
@Slf4j
@Component
public class DriftEventMongoAdapter 
    extends AbstractMongoAdapter<DriftEvent, DriftEventDocument, DriftEventId, DriftEventCriteria> 
    implements DriftEventRepositoryPort {

  private final DriftEventMongoRepository repository;

  public DriftEventMongoAdapter(DriftEventMongoRepository repository, MongoTemplate mongoTemplate) {
    super(repository, mongoTemplate);
    this.repository = repository;
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
    if (criteria == null) return query;
    
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
    
    org.springframework.data.mongodb.core.query.Update update = 
        new org.springframework.data.mongodb.core.query.Update()
            .set("status", DriftEvent.DriftStatus.RESOLVED.name())
            .set("resolvedAt", Instant.now())
            .set("resolvedBy", resolvedBy);
    
    com.mongodb.client.result.UpdateResult result = mongoTemplate.updateMulti(
        query, update, DriftEventDocument.class, getCollectionName()
    );
    
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
}


