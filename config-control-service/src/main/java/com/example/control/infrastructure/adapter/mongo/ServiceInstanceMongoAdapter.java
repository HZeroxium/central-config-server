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
  public ServiceInstance save(ServiceInstance instance) {
    ServiceInstanceDocument doc = ServiceInstanceDocument.fromDomain(instance);
    repository.save(doc);
    return doc.toDomain();
  }

  @Override
  public Optional<ServiceInstance> findById(String id) {
    return repository.findById(id).map(ServiceInstanceDocument::toDomain);
  }

  @Override
  public boolean existsById(String id) {
    return repository.existsById(id);
  }

  @Override
  public void deleteById(String id) {
    repository.deleteById(id);
  }

  // Legacy methods for composite key support
  public Optional<ServiceInstance> findById(String serviceName, String instanceId) {
    String id = serviceName + ":" + instanceId;
    return findById(id);
  }

  public void delete(String serviceName, String instanceId) {
    String id = serviceName + ":" + instanceId;
    deleteById(id);
  }


  @Override
  public long countByServiceName(String serviceName) {
    return repository.countByServiceName(serviceName);
  }

  @Override
  public Page<ServiceInstance> findAll(Object filter, Pageable pageable) {
    ServiceInstanceFilter instanceFilter = (ServiceInstanceFilter) filter;
    Query query = new Query();

    if (instanceFilter != null) {
      if (instanceFilter.serviceName() != null && !instanceFilter.serviceName().isBlank()) {
        query.addCriteria(Criteria.where("serviceName").is(instanceFilter.serviceName()));
      }
      if (instanceFilter.instanceId() != null && !instanceFilter.instanceId().isBlank()) {
        query.addCriteria(Criteria.where("instanceId").is(instanceFilter.instanceId()));
      }
      if (instanceFilter.status() != null) {
        query.addCriteria(Criteria.where("status").is(instanceFilter.status().name()));
      }
      if (instanceFilter.hasDrift() != null) {
        query.addCriteria(Criteria.where("hasDrift").is(instanceFilter.hasDrift()));
      }
      if (instanceFilter.environment() != null && !instanceFilter.environment().isBlank()) {
        query.addCriteria(Criteria.where("environment").is(instanceFilter.environment()));
      }
      if (instanceFilter.version() != null && !instanceFilter.version().isBlank()) {
        query.addCriteria(Criteria.where("version").is(instanceFilter.version()));
      }
      if (instanceFilter.lastSeenAtFrom() != null) {
        query.addCriteria(Criteria.where("lastSeenAt").gte(instanceFilter.lastSeenAtFrom()));
      }
      if (instanceFilter.lastSeenAtTo() != null) {
        query.addCriteria(Criteria.where("lastSeenAt").lte(instanceFilter.lastSeenAtTo()));
      }
      
      // Team-based access control: filter by team IDs (ABAC enforcement)
      if (instanceFilter.userTeamIds() != null && !instanceFilter.userTeamIds().isEmpty()) {
        query.addCriteria(Criteria.where("teamId").in(instanceFilter.userTeamIds()));
      }
    }

    long total = mongoTemplate.count(query, ServiceInstanceDocument.class);
    query.with(pageable);
    List<ServiceInstance> content = mongoTemplate.find(query, ServiceInstanceDocument.class)
        .stream().map(ServiceInstanceDocument::toDomain).collect(Collectors.toList());

    return new PageImpl<>(content, pageable, total);
  }

  @Override
  public long count(Object filter) {
    ServiceInstanceFilter instanceFilter = (ServiceInstanceFilter) filter;
    Query query = new Query();

    if (instanceFilter != null) {
      if (instanceFilter.serviceName() != null && !instanceFilter.serviceName().isBlank()) {
        query.addCriteria(Criteria.where("serviceName").is(instanceFilter.serviceName()));
      }
      if (instanceFilter.instanceId() != null && !instanceFilter.instanceId().isBlank()) {
        query.addCriteria(Criteria.where("instanceId").is(instanceFilter.instanceId()));
      }
      if (instanceFilter.status() != null) {
        query.addCriteria(Criteria.where("status").is(instanceFilter.status().name()));
      }
      if (instanceFilter.hasDrift() != null) {
        query.addCriteria(Criteria.where("hasDrift").is(instanceFilter.hasDrift()));
      }
      if (instanceFilter.environment() != null && !instanceFilter.environment().isBlank()) {
        query.addCriteria(Criteria.where("environment").is(instanceFilter.environment()));
      }
      if (instanceFilter.version() != null && !instanceFilter.version().isBlank()) {
        query.addCriteria(Criteria.where("version").is(instanceFilter.version()));
      }
      if (instanceFilter.lastSeenAtFrom() != null) {
        query.addCriteria(Criteria.where("lastSeenAt").gte(instanceFilter.lastSeenAtFrom()));
      }
      if (instanceFilter.lastSeenAtTo() != null) {
        query.addCriteria(Criteria.where("lastSeenAt").lte(instanceFilter.lastSeenAtTo()));
      }
      
      // Team-based access control: filter by team IDs (ABAC enforcement)
      if (instanceFilter.userTeamIds() != null && !instanceFilter.userTeamIds().isEmpty()) {
        query.addCriteria(Criteria.where("teamId").in(instanceFilter.userTeamIds()));
      }
    }

    return mongoTemplate.count(query, ServiceInstanceDocument.class);
  }
}


