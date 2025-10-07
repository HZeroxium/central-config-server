package com.example.control.application;

import com.example.control.api.exception.ConfigurationException;
import com.example.control.api.exception.ValidationException;
import com.example.control.domain.DriftEvent;
import com.example.control.domain.ServiceInstance;
import com.example.control.application.service.DriftEventService;
import com.example.control.application.service.ServiceInstanceService;
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

  private final ServiceInstanceService serviceInstanceService;
  private final DriftEventService driftEventService;
  private final ConfigProxyService configProxyService;

  @Transactional
  @CacheEvict(value = { "service-instances", "drift-events" }, allEntries = true)
  public ServiceInstance processHeartbeat(HeartbeatPayload payload) {
    log.debug("Processing heartbeat from {}:{}", payload.getServiceName(), payload.getInstanceId());

    // Validate payload
    validateHeartbeatPayload(payload);

    String id = payload.getServiceName() + ":" + payload.getInstanceId();
    ServiceInstance instance = serviceInstanceService
        .findByServiceAndInstance(payload.getServiceName(), payload.getInstanceId())
        .orElse(ServiceInstance.builder()
            .serviceName(payload.getServiceName())
            .instanceId(payload.getInstanceId())
            .status(ServiceInstance.InstanceStatus.HEALTHY)
            .build());

    LocalDateTime now = LocalDateTime.now();

    // First heartbeat - create new instance
    if (instance.getCreatedAt() == null) {
      instance.setCreatedAt(now);
      log.info("New service instance registered: {}", id);
    }

    // Update instance metadata
    instance.setHost(payload.getHost());
    instance.setPort(payload.getPort());
    instance.setEnvironment(payload.getEnvironment());
    instance.setVersion(payload.getVersion());
    instance.setLastAppliedHash(payload.getConfigHash());
    instance.setLastSeenAt(now);
    instance.setUpdatedAt(now);
    instance.setMetadata(payload.getMetadata());

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

    if (hasDrift && !Boolean.TRUE.equals(instance.getHasDrift())) {
      // New drift detected
      log.warn("Configuration drift detected for {}: expected={}, applied={}",
          id, expectedHash, payload.getConfigHash());

      instance.setHasDrift(true);
      instance.setDriftDetectedAt(now);
      instance.setConfigHash(expectedHash);
      instance.setStatus(ServiceInstance.InstanceStatus.DRIFT);

      // Create drift event
      createDriftEvent(payload, expectedHash);

      // Auto-trigger refresh for drifted instance
      triggerRefreshForInstance(payload.getServiceName(), payload.getInstanceId());

    } else if (!hasDrift && Boolean.TRUE.equals(instance.getHasDrift())) {
      // Drift resolved
      log.info("Configuration drift resolved for {}", id);
      instance.setHasDrift(false);
      instance.setDriftDetectedAt(null);
      instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);

      // Resolve drift events
      driftEventService.resolveForInstance(payload.getServiceName(), payload.getInstanceId());
    }

    return serviceInstanceService.saveOrUpdate(instance);
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

    driftEventService.save(event);
  }

  /**
   * Trigger refresh for a specific drifted instance via Config Server's /busrefresh.
   */
  private void triggerRefreshForInstance(String serviceName, String instanceId) {
    try {
      String destination = serviceName + ":" + instanceId;
      String response = configProxyService.triggerBusRefresh(destination);
      log.info("Triggered refresh for drifted instance: {} - response: {}", destination, response);
    } catch (Exception e) {
      log.error("Failed to trigger refresh for {}:{}", serviceName, instanceId, e);
    }
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
