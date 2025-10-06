package com.example.control.application.service;

import com.example.control.domain.ServiceInstance;
import com.example.control.infrastructure.repository.ServiceInstanceDocument;
import com.example.control.infrastructure.repository.ServiceInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceInstanceService {

  private final ServiceInstanceRepository repository;

  public ServiceInstance saveOrUpdate(ServiceInstance instance) {
    ServiceInstanceDocument document = ServiceInstanceDocument.fromDomain(instance);
    if (document.getCreatedAt() == null) {
      document.setCreatedAt(LocalDateTime.now());
    }
    document.setUpdatedAt(LocalDateTime.now());
    repository.save(document);
    return document.toDomain();
  }

  public List<ServiceInstance> findByServiceName(String serviceName) {
    return repository.findByServiceName(serviceName)
        .stream()
        .map(ServiceInstanceDocument::toDomain)
        .collect(Collectors.toList());
  }

  public Optional<ServiceInstance> findByServiceAndInstance(String serviceName, String instanceId) {
    return repository.findByServiceNameAndInstanceId(serviceName, instanceId)
        .map(ServiceInstanceDocument::toDomain);
  }

  public List<ServiceInstance> findAllWithDrift() {
    return repository.findAllWithDrift()
        .stream()
        .map(ServiceInstanceDocument::toDomain)
        .collect(Collectors.toList());
  }

  public List<ServiceInstance> findByServiceWithDrift(String serviceName) {
    return repository.findByServiceNameWithDrift(serviceName)
        .stream()
        .map(ServiceInstanceDocument::toDomain)
        .collect(Collectors.toList());
  }

  public List<ServiceInstance> findStaleInstances(LocalDateTime threshold) {
    return repository.findStaleInstances(threshold)
        .stream()
        .map(ServiceInstanceDocument::toDomain)
        .collect(Collectors.toList());
  }

  public long countByServiceName(String serviceName) {
    return repository.countByServiceName(serviceName);
  }

  public ServiceInstance updateStatusAndDrift(String serviceName,
                                              String instanceId,
                                              ServiceInstance.InstanceStatus status,
                                              boolean hasDrift,
                                              String expectedHash,
                                              String lastAppliedHash) {
    String id = serviceName + ":" + instanceId;
    ServiceInstanceDocument document = repository.findById(id)
        .orElseGet(() -> ServiceInstanceDocument.builder()
            .id(id)
            .serviceName(serviceName)
            .instanceId(instanceId)
            .createdAt(LocalDateTime.now())
            .build());

    document.setStatus(status != null ? status.name() : null);
    document.setHasDrift(hasDrift);
    document.setConfigHash(expectedHash);
    document.setLastAppliedHash(lastAppliedHash);
    document.setUpdatedAt(LocalDateTime.now());

    repository.save(document);
    return document.toDomain();
  }
}


