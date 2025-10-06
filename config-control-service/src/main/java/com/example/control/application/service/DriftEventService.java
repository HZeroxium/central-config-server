package com.example.control.application.service;

import com.example.control.domain.DriftEvent;
import com.example.control.infrastructure.repository.DriftEventDocument;
import com.example.control.infrastructure.repository.DriftEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriftEventService {

  private final DriftEventRepository repository;

  public DriftEvent save(DriftEvent event) {
    DriftEventDocument document = DriftEventDocument.fromDomain(event);
    repository.save(document);
    return document.toDomain();
  }

  public List<DriftEvent> findUnresolved() {
    return repository.findUnresolvedEvents().stream()
        .map(DriftEventDocument::toDomain)
        .collect(Collectors.toList());
  }

  public List<DriftEvent> findUnresolvedByService(String serviceName) {
    return repository.findUnresolvedEventsByService(serviceName).stream()
        .map(DriftEventDocument::toDomain)
        .collect(Collectors.toList());
  }

  public List<DriftEvent> findByService(String serviceName) {
    return repository.findByServiceName(serviceName).stream()
        .map(DriftEventDocument::toDomain)
        .collect(Collectors.toList());
  }

  public List<DriftEvent> findByServiceAndInstance(String serviceName, String instanceId) {
    return repository.findByServiceNameAndInstanceId(serviceName, instanceId).stream()
        .map(DriftEventDocument::toDomain)
        .collect(Collectors.toList());
  }

  public long countAll() {
    return repository.count();
  }

  public long countByStatus(DriftEvent.DriftStatus status) {
    return repository.countByStatus(status.name());
  }

  public void resolveForInstance(String serviceName, String instanceId) {
    repository.findByServiceNameAndInstanceId(serviceName, instanceId).forEach(doc -> {
      if (!DriftEvent.DriftStatus.RESOLVED.name().equals(doc.getStatus())) {
        doc.setStatus(DriftEvent.DriftStatus.RESOLVED.name());
        doc.setResolvedAt(LocalDateTime.now());
        doc.setResolvedBy("heartbeat-service");
        doc.setNotes((doc.getNotes() != null ? doc.getNotes() : "") + " | Auto-resolved via heartbeat");
        repository.save(doc);
      }
    });
  }
}


