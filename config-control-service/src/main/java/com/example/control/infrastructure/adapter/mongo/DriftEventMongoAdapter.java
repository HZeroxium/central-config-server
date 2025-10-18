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
import java.time.LocalDateTime;
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
  public Page<DriftEvent> list(DriftEventFilter filter, Pageable pageable) {
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
      if (filter.severity() != null) {
        query.addCriteria(Criteria.where("severity").is(filter.severity().name()));
      }
      Instant from = filter.detectedAtFrom();
      Instant to = filter.detectedAtTo();
      if (from != null || to != null) {
        Criteria time = Criteria.where("detectedAt");
        if (from != null) time = time.gte(from);
        if (to != null) time = time.lte(to);
        query.addCriteria(time);
      }
      if (Boolean.TRUE.equals(filter.unresolvedOnly())) {
        query.addCriteria(Criteria.where("status").in("DETECTED", "ACKNOWLEDGED", "RESOLVING"));
      }
      
      // Team-based access control: filter by team IDs (ABAC enforcement)
      if (filter.userTeamIds() != null && !filter.userTeamIds().isEmpty()) {
        query.addCriteria(Criteria.where("teamId").in(filter.userTeamIds()));
      }
    }

    long total = mongoTemplate.count(query, DriftEventDocument.class);
    query.with(pageable);
    List<DriftEvent> content = mongoTemplate.find(query, DriftEventDocument.class)
        .stream().map(DriftEventDocument::toDomain).collect(Collectors.toList());
    return new PageImpl<>(content, pageable, total);
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


