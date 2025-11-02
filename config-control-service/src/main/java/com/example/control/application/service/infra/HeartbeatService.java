package com.example.control.application.service.infra;

import com.example.control.api.http.exception.exceptions.ConfigurationException;
import com.example.control.api.http.exception.exceptions.ValidationException;
import com.example.control.infrastructure.external.configserver.ConfigProxyService;
import com.example.control.application.command.ApplicationServiceCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.service.DriftEventService;
import com.example.control.application.service.ServiceInstanceService;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.DriftEventId;
import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.domain.model.ServiceInstance;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core service responsible for processing incoming heartbeat signals.
 * <p>
 * Its responsibilities include:
 * <ul>
 * <li>Validating incoming heartbeat payloads</li>
 * <li>Tracking instance liveness and metadata</li>
 * <li>Detecting configuration drift via config hash comparison</li>
 * <li>Auto-resolving drift once hashes realign</li>
 * <li>Triggering /busrefresh on persistent drift</li>
 * </ul>
 * <p>
 * The service also uses in-memory exponential backoff logic to avoid
 * frequent re-triggering of refresh for persistent drift cases.
 * <p>
 * This orchestrator service ONLY calls Command/Query services, NOT other
 * orchestrators.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final ServiceInstanceService serviceInstanceService;
    private final DriftEventService driftEventService;
    private final ConfigProxyService configProxyService;

    // Command/Query services for ApplicationService
    private final ApplicationServiceCommandService applicationServiceCommandService;
    private final ApplicationServiceQueryService applicationServiceQueryService;

    /**
     * Maintains retry count per instance for drift backoff algorithm.
     */
    private final ConcurrentHashMap<String, Integer> driftRetryCount = new ConcurrentHashMap<>();

    /**
     * Maintains exponential backoff power per instance (1, 2, 4, 8, 16 cycles).
     */
    private final ConcurrentHashMap<String, Integer> driftBackoffPow = new ConcurrentHashMap<>();

    /**
     * Main entry point for heartbeat processing.
     * <p>
     * The method performs multiple responsibilities:
     * <ol>
     * <li>Validate input payload</li>
     * <li>Retrieve or create instance record</li>
     * <li>Update metadata and timestamps</li>
     * <li>Compare applied config hash (from instance) with expected hash (from
     * Config Server)</li>
     * <li>Detect drift, create events, and trigger refresh with exponential
     * backoff</li>
     * <li>Persist the final state to MongoDB</li>
     * </ol>
     * <p>
     * Cache eviction: Evicts specific instance and related drift events cache
     * entries
     * to ensure fresh state for monitoring dashboards.
     *
     * @param payload validated heartbeat payload
     * @return updated {@link ServiceInstance} representing the current state
     */
    @Transactional
    @CacheEvict(value = { "service-instances", "drift-events" }, key = "#payload.instanceId")
    // Using @Observed for both metrics and traces (when enabled)
    // @Timed removed to avoid double-recording latency metrics
    @Observed(name = "heartbeat.process", contextualName = "process-heartbeat")
    public ServiceInstance processHeartbeat(
            @SpanTag("service.name") HeartbeatPayload payload) {
        log.debug("Processing heartbeat from {}:{}", payload.getServiceName(), payload.getInstanceId());

        // 1️⃣ Validate payload (basic sanity checks)
        validateHeartbeatPayload(payload);

        String id = payload.getServiceName() + ":" + payload.getInstanceId();

        // 2️⃣ Load or initialize ServiceInstance domain object
        // Ensure instance always has a valid ID to prevent NPE in getInstanceId()
        ServiceInstanceId instanceId = ServiceInstanceId.of(payload.getInstanceId());
        ServiceInstance instance = serviceInstanceService
                .findById(instanceId)
                .orElse(ServiceInstance.builder()
                        .id(instanceId)
                        .status(ServiceInstance.InstanceStatus.HEALTHY)
                        .build());

        // Validate instance ID is set (should never be null after above)
        if (instance.getId() == null) {
            log.error("ServiceInstance ID is null for instanceId: {}, setting it", payload.getInstanceId());
            instance.setId(instanceId);
        }

        Instant now = Instant.now();

        // 3️⃣ Handle first-time heartbeat (registration)
        boolean isFirstHeartbeat = instance.getCreatedAt() == null;
        if (isFirstHeartbeat) {
            instance.setCreatedAt(now);
            log.info("New service instance registered: {}", id);
        }

        // Always sync serviceId and teamId from ApplicationService to ensure
        // consistency
        // This handles cases where ApplicationService ownership changes after instance
        // creation
        ApplicationService appService = null;
        try {
            // Business logic: Find ApplicationService by exact display name match
            // Use exact match lookup for better performance and accuracy
            Optional<ApplicationService> existing = applicationServiceQueryService
                    .findByDisplayName(payload.getServiceName());

            if (existing.isPresent()) {
                appService = existing.get();
                if (isFirstHeartbeat) {
                    log.debug("Found existing ApplicationService: {} for display name: {}",
                            appService.getId(), payload.getServiceName());
                }

                // Business logic: Merge environments when new instance has different
                // environment
                if (payload.getEnvironment() != null && !payload.getEnvironment().isEmpty()) {
                    List<String> currentEnvironments = appService.getEnvironments();
                    if (currentEnvironments == null || !currentEnvironments.contains(payload.getEnvironment())) {
                        List<String> mergedEnvironments = mergeEnvironments(currentEnvironments,
                                payload.getEnvironment());
                        appService.setEnvironments(mergedEnvironments);
                        appService.setUpdatedAt(Instant.now());
                        appService = applicationServiceCommandService.save(appService);
                        log.info("Merged environment {} into ApplicationService {} environments: {}",
                                payload.getEnvironment(), appService.getId(), mergedEnvironments);
                    }
                }
            } else {
                // Business logic: Recreate orphaned ApplicationService if instance exists but
                // service is missing
                // This handles edge case where ApplicationService was deleted but instance
                // still exists
                if (isFirstHeartbeat || instance.getServiceId() == null) {
                    // Create orphaned service with environment from payload
                    List<String> initialEnvironments;
                    if (payload.getEnvironment() != null && !payload.getEnvironment().isEmpty()) {
                        initialEnvironments = List.of(payload.getEnvironment());
                    } else {
                        initialEnvironments = List.of("dev", "staging", "prod"); // Default if no environment
                    }

                    ApplicationService orphanedService = ApplicationService.builder()
                            .id(ApplicationServiceId.of(UUID.randomUUID().toString()))
                            .displayName(payload.getServiceName())
                            .ownerTeamId(null) // Orphaned - requires approval workflow
                            .environments(initialEnvironments)
                            .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                            .createdAt(Instant.now())
                            .createdBy("system") // System-created
                            .build();

                    appService = applicationServiceCommandService.save(orphanedService);
                    if (isFirstHeartbeat) {
                        log.warn(
                                "Auto-created orphaned ApplicationService: {} (displayName: {}, environment: {}) - requires approval workflow for team assignment",
                                appService.getId(), payload.getServiceName(), payload.getEnvironment());
                    } else {
                        log.warn(
                                "Recreated orphaned ApplicationService: {} (displayName: {}) - ApplicationService was missing but instance exists",
                                appService.getId(), payload.getServiceName());
                    }
                } else {
                    // Instance exists but ApplicationService not found - this shouldn't happen
                    // normally
                    // But we'll recreate it to ensure consistency
                    log.warn(
                            "ApplicationService not found for displayName: {} but instance has serviceId: {}, recreating orphaned service",
                            payload.getServiceName(), instance.getServiceId());

                    List<String> initialEnvironments;
                    if (payload.getEnvironment() != null && !payload.getEnvironment().isEmpty()) {
                        initialEnvironments = List.of(payload.getEnvironment());
                    } else {
                        initialEnvironments = List.of("dev", "staging", "prod"); // Default if no environment
                    }

                    ApplicationService orphanedService = ApplicationService.builder()
                            .id(ApplicationServiceId.of(instance.getServiceId())) // Try to reuse same ID if possible
                            .displayName(payload.getServiceName())
                            .ownerTeamId(null) // Orphaned - requires approval workflow
                            .environments(initialEnvironments)
                            .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                            .createdAt(Instant.now())
                            .createdBy("system") // System-created
                            .build();

                    appService = applicationServiceCommandService.save(orphanedService);
                    log.warn(
                            "Recreated orphaned ApplicationService: {} (displayName: {}) - maintaining consistency",
                            appService.getId(), payload.getServiceName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to sync serviceId and teamId for instance {}: {}", id, e.getMessage(), e);
            // If lookup fails, ensure we still create an orphaned service to maintain
            // consistency
            // This ensures instance always has a valid serviceId (even if null) and
            // prevents NPE
            if (appService == null) {
                try {
                    List<String> initialEnvironments;
                    if (payload.getEnvironment() != null && !payload.getEnvironment().isEmpty()) {
                        initialEnvironments = List.of(payload.getEnvironment());
                    } else {
                        initialEnvironments = List.of("dev", "staging", "prod");
                    }

                    ApplicationService orphanedService = ApplicationService.builder()
                            .id(ApplicationServiceId.of(UUID.randomUUID().toString()))
                            .displayName(payload.getServiceName())
                            .ownerTeamId(null) // Orphaned - requires approval workflow
                            .environments(initialEnvironments)
                            .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                            .createdAt(Instant.now())
                            .createdBy("system") // System-created
                            .build();

                    appService = applicationServiceCommandService.save(orphanedService);
                    log.warn("Created fallback orphaned ApplicationService: {} (displayName: {}) due to lookup failure",
                            appService.getId(), payload.getServiceName());
                } catch (Exception fallbackException) {
                    log.error("Failed to create fallback orphaned ApplicationService for instance {}: {}", id,
                            fallbackException.getMessage(), fallbackException);
                    // If fallback also fails, instance will have null serviceId - this is
                    // acceptable
                    // for orphaned instances that will be linked later
                }
            }
        }

        // Sync serviceId and teamId if appService was successfully resolved (after
        // catch block)
        if (appService != null) {
            String previousServiceId = instance.getServiceId();
            String previousTeamId = instance.getTeamId();

            instance.setServiceId(appService.getId().id());
            instance.setTeamId(appService.getOwnerTeamId()); // May be null for orphaned services

            if (isFirstHeartbeat) {
                if (appService.getOwnerTeamId() == null) {
                    log.warn(
                            "Auto-linked instance {} to orphaned ApplicationService {} - requires approval workflow for team assignment",
                            id, appService.getId());
                } else {
                    log.info("Auto-populated serviceId={} and teamId={} for instance {}",
                            appService.getId().id(), appService.getOwnerTeamId(), id);
                }
            } else {
                // Subsequent heartbeat - log if teamId changed
                if (previousServiceId != null && !previousServiceId.equals(appService.getId().id())) {
                    log.info("ServiceId changed for instance {}: {} -> {}", id, previousServiceId,
                            appService.getId().id());
                }
                if (previousTeamId != null && !previousTeamId.equals(appService.getOwnerTeamId())) {
                    log.info("TeamId synced for instance {}: {} -> {} (ownership changed)",
                            id, previousTeamId, appService.getOwnerTeamId());
                } else if (previousTeamId == null && appService.getOwnerTeamId() != null) {
                    log.info("TeamId synced for instance {}: null -> {} (ownership assigned)",
                            id, appService.getOwnerTeamId());
                }
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
            log.error("Failed to get effective config hash for {}:{}", payload.getServiceName(),
                    payload.getEnvironment(), e);
            throw new ConfigurationException(payload.getServiceName(), payload.getEnvironment(),
                    "Failed to retrieve effective configuration: " + e.getMessage());
        }

        // 6️⃣ Guard: skip drift detection if missing hashes
        if (expectedHash == null || payload.getConfigHash() == null) {
            instance.setStatus(ServiceInstance.InstanceStatus.UNKNOWN);
            instance.setHasDrift(false);
            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);
            return serviceInstanceService.save(instance);
        }

        boolean hasDrift = !expectedHash.equals(payload.getConfigHash());

        // 7️⃣ Handle drift detection & resolution cases
        if (hasDrift && !Boolean.TRUE.equals(instance.getHasDrift())) {
            /** Case A: Drift newly detected */
            log.warn("Configuration drift detected for {}: expected={}, applied={}",
                    id, expectedHash, payload.getConfigHash());

            instance.setHasDrift(true);
            instance.setDriftDetectedAt(now);
            instance.setExpectedHash(expectedHash); // Store for future reference
            instance.setConfigHash(expectedHash);
            instance.setStatus(ServiceInstance.InstanceStatus.DRIFT);

            // Create a drift event record for observability
            createDriftEvent(payload, expectedHash, instance);

            // Trigger /busrefresh to resync configuration
            triggerRefreshForInstance(payload.getServiceName(), payload.getInstanceId());

            // Initialize retry counters for exponential backoff
            driftRetryCount.put(id, 1);
            driftBackoffPow.put(id, 0); // 2^0 = 1 cycle delay

        } else if (!hasDrift && Boolean.TRUE.equals(instance.getHasDrift())) {
            /** Case B: Drift resolved - config hash now matches expected */
            log.info("Configuration drift resolved for {}", id);

            instance.setHasDrift(false);
            instance.setDriftDetectedAt(null);
            instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);
            instance.setExpectedHash(expectedHash); // Update expectedHash for future comparisons

            // Auto-resolve all unresolved drift events for this instance
            // Resolution is scoped by serviceName + instanceId (environment-agnostic as per
            // policy)
            driftEventService.resolveForInstance(
                    payload.getServiceName(),
                    payload.getInstanceId(),
                    "heartbeat-service");

            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);

        } else if (!hasDrift && !Boolean.TRUE.equals(instance.getHasDrift())) {
            /**
             * Case C: Normal steady-state heartbeat - ensure any orphaned events are
             * resolved
             */
            if (instance.getStatus() != ServiceInstance.InstanceStatus.HEALTHY) {
                instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);
            }
            instance.setExpectedHash(expectedHash);

            // Resolve any orphaned DETECTED events from previous sessions
            driftEventService.resolveForInstance(
                    payload.getServiceName(),
                    payload.getInstanceId(),
                    "heartbeat-service");

            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);

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
        return serviceInstanceService.save(instance);
    }

    /**
     * Creates and saves a {@link DriftEvent} record to log drift detection.
     * <p>
     * Populates serviceId and teamId from the ServiceInstance to ensure proper
     * team-based access control and filtering.
     *
     * @param payload      source heartbeat payload
     * @param expectedHash expected configuration hash
     * @param instance     the service instance for context
     */
    private void createDriftEvent(HeartbeatPayload payload, String expectedHash, ServiceInstance instance) {
        DriftEvent event = DriftEvent.builder()
                .id(DriftEventId.of(UUID.randomUUID().toString()))
                .serviceName(payload.getServiceName())
                .instanceId(payload.getInstanceId())
                .serviceId(instance.getServiceId()) // Populate from instance
                .teamId(instance.getTeamId()) // Populate from instance
                .environment(instance.getEnvironment()) // Populate from instance
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
     * Invokes Config Server’s /busrefresh endpoint to trigger a refresh event for a
     * given instance.
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
     * Merges a new environment into the existing environments list.
     * <p>
     * If currentEnvironments is null or empty, returns a list with just the new
     * environment.
     * Otherwise, adds the new environment if it doesn't already exist.
     *
     * @param currentEnvironments the current environments list (may be null or
     *                            empty)
     * @param newEnvironment      the new environment to merge
     * @return merged list of environments
     */
    private List<String> mergeEnvironments(List<String> currentEnvironments, String newEnvironment) {
        if (currentEnvironments == null || currentEnvironments.isEmpty()) {
            return List.of(newEnvironment);
        }
        if (currentEnvironments.contains(newEnvironment)) {
            return currentEnvironments; // Already present, no change needed
        }
        // Create new list with merged environments
        return java.util.stream.Stream.concat(
                currentEnvironments.stream(),
                java.util.stream.Stream.of(newEnvironment))
                .sorted()
                .distinct()
                .toList();
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
