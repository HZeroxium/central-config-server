package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.DriftEvent;
import com.example.control.domain.port.DriftEventRepositoryPort;
import com.example.control.infrastructure.repository.DriftEventMongoRepository;
import com.example.control.infrastructure.repository.documents.DriftEventDocument;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB adapter for {@link DriftEventRepositoryPort}.
 */
@Component
@RequiredArgsConstructor
public class DriftEventMongoAdapter implements DriftEventRepositoryPort {

  private final DriftEventMongoRepository repository;
  private final MongoTemplate mongoTemplate;

  @Override
  public DriftEvent save(DriftEvent event) {
    DriftEventDocument doc = DriftEventDocument.fromDomain(event);
    repository.save(doc);
    return doc.toDomain();
  }

  @Override
  public Optional<DriftEvent> findById(String id) {
    return repository.findById(id).map(DriftEventDocument::toDomain);
  }

  @Override
  public boolean existsById(String id) {
    return repository.existsById(id);
  }

  @Override
  public void deleteById(String id) {
    repository.deleteById(id);
  }

    @Override
    public Page<DriftEvent> findAll(Object filter, Pageable pageable) {
        DriftEventRepositoryPort.DriftEventFilter eventFilter = (DriftEventRepositoryPort.DriftEventFilter) filter;
    Query query = new Query();

    if (eventFilter != null) {
      if (eventFilter.serviceName() != null && !eventFilter.serviceName().isBlank()) {
        query.addCriteria(Criteria.where("serviceName").is(eventFilter.serviceName()));
      }
      if (eventFilter.instanceId() != null && !eventFilter.instanceId().isBlank()) {
        query.addCriteria(Criteria.where("instanceId").is(eventFilter.instanceId()));
      }
      if (eventFilter.status() != null) {
        query.addCriteria(Criteria.where("status").is(eventFilter.status().name()));
      }
      if (eventFilter.severity() != null) {
        query.addCriteria(Criteria.where("severity").is(eventFilter.severity().name()));
      }
      Instant from = eventFilter.detectedAtFrom();
      Instant to = eventFilter.detectedAtTo();
      if (from != null || to != null) {
        Criteria time = Criteria.where("detectedAt");
        if (from != null) time = time.gte(from);
        if (to != null) time = time.lte(to);
        query.addCriteria(time);
      }
      if (Boolean.TRUE.equals(eventFilter.unresolvedOnly())) {
        query.addCriteria(Criteria.where("status").in("DETECTED", "ACKNOWLEDGED", "RESOLVING"));
      }
      
      // Team-based access control: filter by team IDs (ABAC enforcement)
      if (eventFilter.userTeamIds() != null && !eventFilter.userTeamIds().isEmpty()) {
        query.addCriteria(Criteria.where("teamId").in(eventFilter.userTeamIds()));
      }
    }

    long total = mongoTemplate.count(query, DriftEventDocument.class);
    query.with(pageable);
    List<DriftEvent> content = mongoTemplate.find(query, DriftEventDocument.class)
        .stream().map(DriftEventDocument::toDomain).collect(Collectors.toList());
    return new PageImpl<>(content, pageable, total);
  }

  @Override
  public long count(Object filter) {
    DriftEventRepositoryPort.DriftEventFilter eventFilter = (DriftEventRepositoryPort.DriftEventFilter) filter;
    Query query = new Query();
    if (eventFilter != null) {
      if (eventFilter.serviceName() != null && !eventFilter.serviceName().isBlank()) {
        query.addCriteria(Criteria.where("serviceName").is(eventFilter.serviceName()));
      }
      if (eventFilter.instanceId() != null && !eventFilter.instanceId().isBlank()) {
        query.addCriteria(Criteria.where("instanceId").is(eventFilter.instanceId()));
      }
      if (eventFilter.status() != null) {
        query.addCriteria(Criteria.where("status").is(eventFilter.status().name()));
      }
      if (eventFilter.severity() != null) {
        query.addCriteria(Criteria.where("severity").is(eventFilter.severity().name()));
      }
      Instant from = eventFilter.detectedAtFrom();
      Instant to = eventFilter.detectedAtTo();
      if (from != null || to != null) {
        Criteria time = Criteria.where("detectedAt");
        if (from != null) time = time.gte(from);
        if (to != null) time = time.lte(to);
        query.addCriteria(time);
      }
      if (Boolean.TRUE.equals(eventFilter.unresolvedOnly())) {
        query.addCriteria(Criteria.where("status").in("DETECTED", "ACKNOWLEDGED", "RESOLVING"));
      }
      if (eventFilter.userTeamIds() != null && !eventFilter.userTeamIds().isEmpty()) {
        query.addCriteria(Criteria.where("teamId").in(eventFilter.userTeamIds()));
      }
    }
    return mongoTemplate.count(query, DriftEventDocument.class);
  }

  @Override
  public void resolveForInstance(String serviceName, String instanceId, String resolvedBy) {
    repository.findByServiceNameAndInstanceId(serviceName, instanceId).forEach(doc -> {
      if (!DriftEvent.DriftStatus.RESOLVED.name().equals(doc.getStatus())) {
        doc.setStatus(DriftEvent.DriftStatus.RESOLVED.name());
        doc.setResolvedAt(Instant.now());
        doc.setResolvedBy(resolvedBy);
        doc.setNotes((doc.getNotes() != null ? doc.getNotes() : "") + " | Resolved via adapter");
        repository.save(doc);
      }
    });
  }

  @Override
  public long countAll() {
    return repository.count();
  }

  @Override
  public long countByStatus(DriftEvent.DriftStatus status) {
    return repository.countByStatus(status.name());
  }
}


