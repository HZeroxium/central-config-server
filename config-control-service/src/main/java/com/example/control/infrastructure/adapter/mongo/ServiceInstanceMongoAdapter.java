package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.ServiceInstance;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import com.example.control.infrastructure.repository.ServiceInstanceMongoRepository;
import com.example.control.infrastructure.repository.documents.ServiceInstanceDocument;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB adapter implementing {@link ServiceInstanceRepositoryPort} using Spring Data and MongoTemplate.
 */
@Component
@RequiredArgsConstructor
public class ServiceInstanceMongoAdapter implements ServiceInstanceRepositoryPort {

  private final ServiceInstanceMongoRepository repository;
  private final MongoTemplate mongoTemplate;

  @Override
  public ServiceInstance saveOrUpdate(ServiceInstance instance) {
    ServiceInstanceDocument doc = ServiceInstanceDocument.fromDomain(instance);
    repository.save(doc);
    return doc.toDomain();
  }

  @Override
  public Optional<ServiceInstance> findById(String serviceName, String instanceId) {
    String id = serviceName + ":" + instanceId;
    return repository.findById(id).map(ServiceInstanceDocument::toDomain);
  }

  @Override
  public void delete(String serviceName, String instanceId) {
    String id = serviceName + ":" + instanceId;
    repository.deleteById(id);
  }

  @Override
  public long countByServiceName(String serviceName) {
    return repository.countByServiceName(serviceName);
  }

  @Override
  public Page<ServiceInstance> list(ServiceInstanceFilter filter, Pageable pageable) {
    Query query = new Query();

    if (filter != null) {
      if (filter.serviceName() != null && !filter.serviceName().isBlank()) {
        query.addCriteria(Criteria.where("serviceName").is(filter.serviceName()));
      }
      if (filter.instanceId() != null && !filter.instanceId().isBlank()) {
        query.addCriteria(Criteria.where("instanceId").is(filter.instanceId()));
      }
      if (filter.status() != null) {
        query.addCriteria(Criteria.where("status").is(filter.status().name()));
      }
      if (filter.hasDrift() != null) {
        query.addCriteria(Criteria.where("hasDrift").is(filter.hasDrift()));
      }
      if (filter.environment() != null && !filter.environment().isBlank()) {
        query.addCriteria(Criteria.where("environment").is(filter.environment()));
      }
      if (filter.version() != null && !filter.version().isBlank()) {
        query.addCriteria(Criteria.where("version").is(filter.version()));
      }
      if (filter.lastSeenAtFrom() != null) {
        query.addCriteria(Criteria.where("lastSeenAt").gte(filter.lastSeenAtFrom()));
      }
      if (filter.lastSeenAtTo() != null) {
        query.addCriteria(Criteria.where("lastSeenAt").lte(filter.lastSeenAtTo()));
      }
      
      // Team-based access control: filter by team IDs (ABAC enforcement)
      if (filter.userTeamIds() != null && !filter.userTeamIds().isEmpty()) {
        query.addCriteria(Criteria.where("teamId").in(filter.userTeamIds()));
      }
    }

    long total = mongoTemplate.count(query, ServiceInstanceDocument.class);
    query.with(pageable);
    List<ServiceInstance> content = mongoTemplate.find(query, ServiceInstanceDocument.class)
        .stream().map(ServiceInstanceDocument::toDomain).collect(Collectors.toList());

    return new PageImpl<>(content, pageable, total);
  }
}


