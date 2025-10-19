package com.example.control.application;

import com.example.control.api.exception.ConfigurationException;
import com.example.control.api.exception.ValidationException;
import com.example.control.domain.ApplicationService;
import com.example.control.domain.DriftEvent;
import com.example.control.domain.ServiceInstance;
import com.example.control.domain.id.DriftEventId;
import com.example.control.domain.id.ServiceInstanceId;
import com.example.control.application.service.DriftEventService;
import com.example.control.application.service.ServiceInstanceService;
import com.example.control.application.service.ApplicationServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.annotation.SpanTag;
import io.micrometer.tracing.annotation.NewSpan;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core service responsible for processing incoming heartbeat signals.
 * <p>
 * Its responsibilities include:
 * <ul>
 *   <li>Validating incoming heartbeat payloads</li>
 *   <li>Tracking instance liveness and metadata</li>
 *   <li>Detecting configuration drift via config hash comparison</li>
 *   <li>Auto-resolving drift once hashes realign</li>
 *   <li>Triggering /busrefresh on persistent drift</li>
 * </ul>
 * <p>
 * The service also uses in-memory exponential backoff logic to avoid
 * frequent re-triggering of refresh for persistent drift cases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

  private final ServiceInstanceService serviceInstanceService;
  private final DriftEventService driftEventService;
  private final ConfigProxyService configProxyService;
  private final ApplicationServiceService applicationServiceService;

  /** Maintains retry count per instance for drift backoff algorithm. */
  private final ConcurrentHashMap<String, Integer> driftRetryCount = new ConcurrentHashMap<>();

  /** Maintains exponential backoff power per instance (1, 2, 4, 8, 16 cycles). */
  private final ConcurrentHashMap<String, Integer> driftBackoffPow = new ConcurrentHashMap<>();

  /**
   * Main entry point for heartbeat processing.
   * <p>
   * The method performs multiple responsibilities:
   * <ol>
   *   <li>Validate input payload</li>
   *   <li>Retrieve or create instance record</li>
   *   <li>Update metadata and timestamps</li>
   *   <li>Compare applied config hash (from instance) with expected hash (from Config Server)</li>
   *   <li>Detect drift, create events, and trigger refresh with exponential backoff</li>
   *   <li>Persist the final state to MongoDB</li>
   * </ol>
   * <p>
   * Caching for `service-instances` and `drift-events` is evicted on each heartbeat
   * to ensure fresh state for monitoring dashboards.
   *
   * @param payload validated heartbeat payload
   * @return updated {@link ServiceInstance} representing the current state
   */
  @Transactional
  @CacheEvict(value = {"service-instances", "drift-events"}, allEntries = true)
  @Timed("config_control.heartbeat.process")
  @Observed(name = "heartbeat.process", contextualName = "process-heartbeat")
  public ServiceInstance processHeartbeat(
      @SpanTag("service.name") HeartbeatPayload payload) {
    log.debug("Processing heartbeat from {}:{}", payload.getServiceName(), payload.getInstanceId());
    

    // 1️⃣ Validate payload (basic sanity checks)
    validateHeartbeatPayload(payload);

    String id = payload.getServiceName() + ":" + payload.getInstanceId();

    // 2️⃣ Load or initialize ServiceInstance domain object
           ServiceInstance instance = serviceInstanceService
               .findByServiceAndInstance(payload.getServiceName(), payload.getInstanceId())
               .orElse(ServiceInstance.builder()
                   .id(ServiceInstanceId.of(payload.getServiceName(), payload.getInstanceId()))
                   .status(ServiceInstance.InstanceStatus.HEALTHY)
                   .build());

    Instant now = Instant.now();

    // 3️⃣ Handle first-time heartbeat (registration)
    if (instance.getCreatedAt() == null) {
      instance.setCreatedAt(now);
      log.info("New service instance registered: {}", id);
      
      // Auto-populate serviceId and teamId by looking up ApplicationService
      try {
        Optional<ApplicationService> appService = applicationServiceService.findAll()
            .stream()
            .filter(svc -> svc.getDisplayName().equals(payload.getServiceName()))
            .findFirst();
            
        if (appService.isPresent()) {
          instance.setServiceId(appService.get().getId().id());
          instance.setTeamId(appService.get().getOwnerTeamId());
          log.debug("Auto-populated serviceId={} and teamId={} for instance {}", 
                   appService.get().getId().id(), appService.get().getOwnerTeamId(), id);
        }
      } catch (Exception e) {
        log.warn("Failed to auto-populate serviceId and teamId for instance {}: {}", id, e.getMessage());
      }
    }

    // 4️⃣ Update runtime metadata (host, port, version, hashes)
    instance.setHost(payload.getHost());
    instance.setPort(payload.getPort());
    instance.setEnvironment(payload.getEnvironment());
    instance.setVersion(payload.getVersion());
    instance.setLastAppliedHash(payload.getConfigHash());
    instance.setLastSeenAt(now);
    instance.setUpdatedAt(now);
    instance.setMetadata(payload.getMetadata());

    // 5️⃣ Retrieve expected config hash from Config Server
    String expectedHash;
    try {
      expectedHash = getExpectedConfigHashWithSpan(payload.getServiceName(), payload.getEnvironment());
    } catch (Exception e) {
      log.error("Failed to get effective config hash for {}:{}", payload.getServiceName(), payload.getEnvironment(), e);
      throw new ConfigurationException(payload.getServiceName(), payload.getEnvironment(),
          "Failed to retrieve effective configuration: " + e.getMessage());
    }

    // 6️⃣ Guard: skip drift detection if missing hashes
    if (expectedHash == null || payload.getConfigHash() == null) {
      instance.setStatus(ServiceInstance.InstanceStatus.UNKNOWN);
      instance.setHasDrift(false);
      driftRetryCount.remove(id);
      driftBackoffPow.remove(id);
      return serviceInstanceService.saveOrUpdate(instance);
    }

    boolean hasDrift = !expectedHash.equals(payload.getConfigHash());

    // 7️⃣ Handle drift detection & resolution cases
    if (hasDrift && !Boolean.TRUE.equals(instance.getHasDrift())) {
      /** Case A: Drift newly detected */
      log.warn("Configuration drift detected for {}: expected={}, applied={}",
          id, expectedHash, payload.getConfigHash());

      instance.setHasDrift(true);
      instance.setDriftDetectedAt(now);
      instance.setConfigHash(expectedHash);
      instance.setStatus(ServiceInstance.InstanceStatus.DRIFT);


      // Create a drift event record for observability
      createDriftEvent(payload, expectedHash);

      // Trigger /busrefresh to resync configuration
      triggerRefreshForInstance(payload.getServiceName(), payload.getInstanceId());
      

      // Initialize retry counters for exponential backoff
      driftRetryCount.put(id, 1);
      driftBackoffPow.put(id, 0); // 2^0 = 1 cycle delay

    } else if (!hasDrift && Boolean.TRUE.equals(instance.getHasDrift())) {
      /** Case B: Drift resolved */
      log.info("Configuration drift resolved for {}", id);
      instance.setHasDrift(false);
      instance.setDriftDetectedAt(null);
      instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);


      // Resolve any open drift events in persistence
      driftEventService.resolveForInstance(payload.getServiceName(), payload.getInstanceId());
      driftRetryCount.remove(id);

    } else if (!hasDrift && !Boolean.TRUE.equals(instance.getHasDrift())) {
      /** Case C: Normal steady-state heartbeat */
      if (instance.getStatus() != ServiceInstance.InstanceStatus.HEALTHY) {
        instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);
      }
      driftRetryCount.remove(id);

    } else if (hasDrift && Boolean.TRUE.equals(instance.getHasDrift())) {
      /** Case D: Persistent drift — apply exponential backoff strategy */
      int count = driftRetryCount.merge(id, 1, Integer::sum);
      int pow = driftBackoffPow.compute(id, (k, v) -> v == null ? 0 : Math.min(v, 4)); // limit to 16 cycles
      int threshold = 1 << pow; // 1, 2, 4, 8, 16
      if (count >= threshold) {
        log.warn("Persistent drift for {} after {} heartbeats (threshold {}). Re-triggering refresh.",
            id, count, threshold);
        triggerRefreshForInstance(payload.getServiceName(), payload.getInstanceId());
        
        
        driftRetryCount.put(id, 0);
        driftBackoffPow.put(id, Math.min(pow + 1, 4));
      }
    }

    // 8️⃣ Persist instance to MongoDB and return updated state
    return serviceInstanceService.saveOrUpdate(instance);
  }

  /**
   * Creates and saves a {@link DriftEvent} record to log drift detection.
   *
   * @param payload      source heartbeat payload
   * @param expectedHash expected configuration hash
   */
  private void createDriftEvent(HeartbeatPayload payload, String expectedHash) {
    DriftEvent event = DriftEvent.builder()
        .id(DriftEventId.of(UUID.randomUUID().toString()))
        .serviceName(payload.getServiceName())
        .instanceId(payload.getInstanceId())
        .expectedHash(expectedHash)
        .appliedHash(payload.getConfigHash())
        .severity(DriftEvent.DriftSeverity.MEDIUM)
        .status(DriftEvent.DriftStatus.DETECTED)
        .detectedAt(Instant.now())
        .detectedBy("heartbeat-service")
        .notes("Drift detected via heartbeat")
        .build();

    driftEventService.save(event);
  }

  /**
   * Invokes Config Server’s /busrefresh endpoint to trigger a refresh event for a given instance.
   *
   * @param serviceName service identifier
   * @param instanceId  instance identifier
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
   * Retrieves expected config hash from Config Server with tracing span.
   */
  @NewSpan("config.get_effective_hash")
  private String getExpectedConfigHashWithSpan(
      @SpanTag("service.name") String serviceName,
      @SpanTag("environment") String environment) {
    return configProxyService.getEffectiveConfigHash(serviceName, environment);
  }

  /**
   * Validates the structure and fields of a heartbeat payload.
   * <p>
   * Throws {@link ValidationException} if required fields are missing or invalid.
   *
   * @param payload incoming heartbeat data
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
