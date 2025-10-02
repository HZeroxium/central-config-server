package com.example.control.application;

import com.example.control.api.exception.ConfigurationException;
import com.example.control.api.exception.ValidationException;
import com.example.control.domain.DriftEvent;
import com.example.control.domain.ServiceInstance;
import com.example.control.infrastructure.repository.DriftEventDocument;
import com.example.control.infrastructure.repository.DriftEventRepository;
import com.example.control.infrastructure.repository.ServiceInstanceDocument;
import com.example.control.infrastructure.repository.ServiceInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for processing heartbeat from service instances
 * and detecting configuration drift.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

  private final ServiceInstanceRepository instanceRepository;
  private final DriftEventRepository driftEventRepository;
  private final ConfigProxyService configProxyService;

  @Transactional
  @CacheEvict(value = { "service-instances", "drift-events" }, allEntries = true)
  public ServiceInstance processHeartbeat(HeartbeatPayload payload) {
    log.debug("Processing heartbeat from {}:{}", payload.getServiceName(), payload.getInstanceId());

    // Validate payload
    validateHeartbeatPayload(payload);

    String id = payload.getServiceName() + ":" + payload.getInstanceId();
    ServiceInstanceDocument document = instanceRepository.findById(id)
        .orElse(new ServiceInstanceDocument());

    LocalDateTime now = LocalDateTime.now();

    // First heartbeat - create new instance
    if (document.getId() == null) {
      document.setId(id);
      document.setServiceName(payload.getServiceName());
      document.setInstanceId(payload.getInstanceId());
      document.setCreatedAt(now);
      document.setStatus(ServiceInstance.InstanceStatus.HEALTHY.name());
      log.info("New service instance registered: {}", id);
    }

    // Update instance metadata
    document.setHost(payload.getHost());
    document.setPort(payload.getPort());
    document.setEnvironment(payload.getEnvironment());
    document.setVersion(payload.getVersion());
    document.setLastAppliedHash(payload.getConfigHash());
    document.setLastSeenAt(now);
    document.setUpdatedAt(now);
    document.setMetadata(payload.getMetadata());

    // Detect drift
    String expectedHash;
    try {
      expectedHash = configProxyService.getEffectiveConfigHash(
          payload.getServiceName(),
          payload.getEnvironment());
    } catch (Exception e) {
      log.error("Failed to get effective config hash for {}:{}",
          payload.getServiceName(), payload.getEnvironment(), e);
      throw new ConfigurationException(
          payload.getServiceName(),
          payload.getEnvironment(),
          "Failed to retrieve effective configuration: " + e.getMessage());
    }

    boolean hasDrift = expectedHash != null &&
        payload.getConfigHash() != null &&
        !expectedHash.equals(payload.getConfigHash());

    if (hasDrift && !Boolean.TRUE.equals(document.getHasDrift())) {
      // New drift detected
      log.warn("Configuration drift detected for {}: expected={}, applied={}",
          id, expectedHash, payload.getConfigHash());

      document.setHasDrift(true);
      document.setDriftDetectedAt(now);
      document.setConfigHash(expectedHash);
      document.setStatus(ServiceInstance.InstanceStatus.DRIFT.name());

      // Create drift event
      createDriftEvent(payload, expectedHash);

    } else if (!hasDrift && Boolean.TRUE.equals(document.getHasDrift())) {
      // Drift resolved
      log.info("Configuration drift resolved for {}", id);
      document.setHasDrift(false);
      document.setDriftDetectedAt(null);
      document.setStatus(ServiceInstance.InstanceStatus.HEALTHY.name());

      // Resolve drift events
      resolveDriftEvents(payload.getServiceName(), payload.getInstanceId());
    }

    instanceRepository.save(document);
    return document.toDomain();
  }

  private void createDriftEvent(HeartbeatPayload payload, String expectedHash) {
    DriftEvent event = DriftEvent.builder()
        .id(UUID.randomUUID().toString())
        .serviceName(payload.getServiceName())
        .instanceId(payload.getInstanceId())
        .expectedHash(expectedHash)
        .appliedHash(payload.getConfigHash())
        .severity(DriftEvent.DriftSeverity.MEDIUM)
        .status(DriftEvent.DriftStatus.DETECTED)
        .detectedAt(LocalDateTime.now())
        .detectedBy("heartbeat-service")
        .notes("Drift detected via heartbeat")
        .build();

    driftEventRepository.save(DriftEventDocument.fromDomain(event));
  }

  private void resolveDriftEvents(String serviceName, String instanceId) {
    driftEventRepository.findByServiceNameAndInstanceId(serviceName, instanceId)
        .stream()
        .filter(doc -> !doc.getStatus().equals(DriftEvent.DriftStatus.RESOLVED.name()))
        .forEach(doc -> {
          doc.setStatus(DriftEvent.DriftStatus.RESOLVED.name());
          doc.setResolvedAt(LocalDateTime.now());
          doc.setResolvedBy("heartbeat-service");
          doc.setNotes(doc.getNotes() + " | Auto-resolved via heartbeat");
          driftEventRepository.save(doc);
        });
  }

  /**
   * Validate heartbeat payload.
   */
  private void validateHeartbeatPayload(HeartbeatPayload payload) {
    if (payload == null) {
      throw new ValidationException("Heartbeat payload cannot be null");
    }

    if (!StringUtils.hasText(payload.getServiceName())) {
      throw new ValidationException("Service name is required");
    }

    if (!StringUtils.hasText(payload.getInstanceId())) {
      throw new ValidationException("Instance ID is required");
    }

    if (payload.getHost() != null && !StringUtils.hasText(payload.getHost())) {
      throw new ValidationException("Host cannot be empty if provided");
    }

    if (payload.getPort() != null && (payload.getPort() < 1 || payload.getPort() > 65535)) {
      throw new ValidationException("Port must be between 1 and 65535");
    }
  }
}
