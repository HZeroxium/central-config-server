package com.example.control.infrastructure.mongo.adapter;

import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.id.ServiceInstanceId;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import com.example.control.infrastructure.mongo.repository.ServiceInstanceMongoRepository;
import com.example.control.infrastructure.mongo.documents.ServiceInstanceDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;


/**
 * MongoDB adapter implementing {@link ServiceInstanceRepositoryPort} using Spring Data and MongoTemplate.
 */
@Component
public class ServiceInstanceMongoAdapter 
    extends AbstractMongoAdapter<ServiceInstance, ServiceInstanceDocument, ServiceInstanceId, ServiceInstanceCriteria>
    implements ServiceInstanceRepositoryPort {

  private final ServiceInstanceMongoRepository repository;

  public ServiceInstanceMongoAdapter(ServiceInstanceMongoRepository repository, MongoTemplate mongoTemplate) {
    super(repository, mongoTemplate);
    this.repository = repository;
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
  public long countByServiceName(String serviceName) {
    return repository.countByServiceName(serviceName);
  }

  @Override
  public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
    return super.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
  }

  @Override
  protected Class<ServiceInstanceDocument> getDocumentClass() {
    return ServiceInstanceDocument.class;
  }
}


