package com.example.control.application.service.infra;

import com.example.control.api.http.exception.exceptions.ConfigurationException;
import com.example.control.api.http.exception.exceptions.ValidationException;
import com.example.control.infrastructure.external.configserver.ConfigProxyService;
import com.example.control.application.command.ApplicationServiceCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.ServiceCredentialQueryService;
import com.example.control.application.service.DriftEventService;
import com.example.control.application.service.ServiceInstanceService;
import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.valueobject.id.DriftEventId;
import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.domain.model.ServiceInstance;
import com.example.control.infrastructure.cache.ServiceInstanceCacheEvictionService;
import com.example.control.infrastructure.config.messaging.HeartbeatProperties;
import com.example.control.infrastructure.observability.MetricsNames;
import com.example.control.infrastructure.security.JwtExtractor;
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
    private final ServiceInstanceCacheEvictionService cacheEvictionService;
    private final HeartbeatProperties heartbeatProperties;

    // Command/Query services for ApplicationService
    private final ApplicationServiceCommandService applicationServiceCommandService;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final ServiceCredentialQueryService serviceCredentialQueryService;

    /**
     * Maintains retry count per instance for drift backoff algorithm.
     */
    private final ConcurrentHashMap<String, Integer> driftRetryCount = new ConcurrentHashMap<>();

    /**
     * Maintains exponential backoff power per instance (1, 2, 4, 8, 16 cycles).
     */
    private final ConcurrentHashMap<String, Integer> driftBackoffPow = new ConcurrentHashMap<>();

    /**
     * Maintains drift event count per instance for threshold-based event creation.
     * Separate from driftRetryCount which is used for refresh backoff.
     */
    private final ConcurrentHashMap<String, Integer> driftEventCount = new ConcurrentHashMap<>();

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
    @Observed(name = MetricsNames.Heartbeat.PROCESS, contextualName = "process-heartbeat")
    public ServiceInstance processHeartbeat(
            @SpanTag("service.name") HeartbeatPayload payload) {
        log.debug("Processing heartbeat from {}:{}", payload.getServiceName(), payload.getInstanceId());

        // 1️⃣ Validate payload (basic sanity checks)
        validateHeartbeatPayload(payload);

        // 1.5️⃣ Validate authentication and service credentials
        validateAuthenticationAndCredentials(payload);

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
                // ApplicationService not found - throw exception (no auto-create)
                throw new IllegalStateException(
                        String.format("ApplicationService not found for displayName: %s. " +
                                "Please register the service via Admin Dashboard first, then obtain service credentials.",
                                payload.getServiceName()));
            }
        } catch (IllegalStateException e) {
            // Re-throw IllegalStateException (service not found)
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync serviceId and teamId for instance {}: {}", id, e.getMessage(), e);
            throw new IllegalStateException(
                    String.format("Failed to resolve ApplicationService for displayName: %s. Error: %s",
                            payload.getServiceName(), e.getMessage()), e);
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
            driftEventCount.remove(id);
            return serviceInstanceService.save(instance);
        }

        boolean hasDrift = !expectedHash.equals(payload.getConfigHash());

        // 7️⃣ Handle drift detection & resolution cases
        if (hasDrift && !Boolean.TRUE.equals(instance.getHasDrift())) {
            /** Case A: Drift newly detected - track internally but keep status HEALTHY until threshold */
            log.warn("Configuration drift detected for {}: expected={}, applied={}",
                    id, expectedHash, payload.getConfigHash());

            // Keep status HEALTHY and hasDrift false until threshold is reached
            // Only track drift internally via driftEventCount
            instance.setExpectedHash(expectedHash); // Store for future reference
            instance.setConfigHash(expectedHash);
            // Do NOT set hasDrift=true or status=DRIFT yet

            // Initialize drift event counter (start at 1 for first detection)
            driftEventCount.put(id, 1);
            
            // Don't create event on first detection - wait for threshold to be reached
            int threshold = heartbeatProperties.getDriftDetection().getEventThreshold();
            if (threshold <= 1) {
                // If threshold is 1 or less, set DRIFT status and create event immediately
                instance.setHasDrift(true);
                instance.setDriftDetectedAt(now);
                instance.setStatus(ServiceInstance.InstanceStatus.DRIFT);
                createDriftEvent(payload, expectedHash, instance);
            } else {
                log.debug("Drift detected for {} but threshold not reached ({}/{}) - keeping status HEALTHY",
                        id, driftEventCount.get(id), threshold);
            }

            // Trigger /busrefresh to resync configuration (always trigger immediately for auto-correction)
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
            driftEventCount.remove(id); // Reset drift event counter

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
            driftEventCount.remove(id); // Reset drift event counter

        } else if (hasDrift) {
            /** Case D: Persistent drift — increment counter and set DRIFT status when threshold reached */
            int count = driftRetryCount.merge(id, 1, Integer::sum);
            int pow = driftBackoffPow.compute(id, (k, v) -> v == null ? 0 : Math.min(v, 4)); // limit to 16 cycles
            int refreshThreshold = 1 << pow; // 1, 2, 4, 8, 16
            
            // Increment drift event counter and check if threshold reached
            int eventCount = driftEventCount.merge(id, 1, Integer::sum);
            int eventThreshold = heartbeatProperties.getDriftDetection().getEventThreshold();
            
            // Check if this is the first time threshold is reached
            if (eventCount >= eventThreshold && !Boolean.TRUE.equals(instance.getHasDrift())) {
                // Threshold reached - now set DRIFT status and create event
                log.warn("Drift threshold reached for {} after {} consecutive detections. Setting status DRIFT and creating DriftEvent.",
                        id, eventCount);
                instance.setHasDrift(true);
                instance.setDriftDetectedAt(now);
                instance.setStatus(ServiceInstance.InstanceStatus.DRIFT);
                createDriftEvent(payload, expectedHash, instance);
            } else if (eventCount >= eventThreshold && Boolean.TRUE.equals(instance.getHasDrift())) {
                // Already marked as DRIFT, just log
                log.debug("Drift persists for {} (count: {})", id, eventCount);
            }
            
            if (count >= refreshThreshold) {
                log.warn("Persistent drift for {} after {} heartbeats (threshold {}). Re-triggering refresh.",
                        id, count, refreshThreshold);
                triggerRefreshForInstance(payload.getServiceName(), payload.getInstanceId());

                driftRetryCount.put(id, 0);
                driftBackoffPow.put(id, Math.min(pow + 1, 4));
            }
        }

        // 8️⃣ Persist instance to MongoDB and return updated state
        ServiceInstance savedInstance = serviceInstanceService.save(instance);

        // 9️⃣ Evict cache entries for findAll and count caches
        // Status changes affect findAll queries with status filters, so we need to evict them
        if (instance.getId() != null) {
            cacheEvictionService.evictForStatusChange(instance.getId());
        }

        return savedInstance;
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
     * Validate authentication and service credentials for heartbeat.
     * <p>
     * Validates that:
     * <ul>
     * <li>JWT token is present in SecurityContext</li>
     * <li>Client ID can be extracted from JWT (azp or aud claim)</li>
     * <li>ServiceCredential exists for the client ID</li>
     * <li>Credential status is ACTIVE</li>
     * <li>Service name from payload matches ApplicationService.displayName</li>
     * </ul>
     * </p>
     *
     * @param payload the heartbeat payload
     * @throws IllegalStateException if authentication or credential validation fails
     */
    private void validateAuthenticationAndCredentials(HeartbeatPayload payload) {
        // Extract clientId from JWT
        String clientId;
        try {
            clientId = JwtExtractor.extractClientIdFromContext();
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    "Authentication required for heartbeat. No JWT token found in SecurityContext.", e);
        }

        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalStateException(
                    "Unable to extract client ID from JWT token. Missing 'azp' or 'aud' claim.");
        }

        log.debug("Extracted clientId from JWT: {} for service: {}", clientId, payload.getServiceName());

        // Lookup ServiceCredential by keycloakClientId
        Optional<ServiceCredential> credentialOpt = serviceCredentialQueryService
                .findByKeycloakClientId(clientId);

        if (credentialOpt.isEmpty()) {
            throw new IllegalStateException(
                    String.format("ServiceCredential not found for clientId: %s. " +
                            "Please ensure service credentials are created and activated.", clientId));
        }

        ServiceCredential credential = credentialOpt.get();

        // Validate credential status is ACTIVE (PENDING credentials cannot be used)
        if (credential.getStatus() != ServiceCredential.CredentialStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("ServiceCredential for clientId: %s is not ACTIVE. Current status: %s. " +
                            "Please activate credentials via POST /api/services/%s/credentials/activate " +
                            "after config files are ready.", clientId, credential.getStatus(), 
                            credential.getServiceId().id()));
        }

        // Validate serviceName matches ApplicationService.displayName
        Optional<ApplicationService> appServiceOpt = applicationServiceQueryService
                .findById(credential.getServiceId());

        if (appServiceOpt.isEmpty()) {
            throw new IllegalStateException(
                    String.format("ApplicationService not found for credential serviceId: %s. " +
                            "Credential may be orphaned.", credential.getServiceId()));
        }

        ApplicationService appService = appServiceOpt.get();
        if (!appService.getDisplayName().equals(payload.getServiceName())) {
            throw new IllegalStateException(
                    String.format("Service name mismatch. JWT clientId '%s' is associated with service '%s', " +
                            "but heartbeat payload contains serviceName '%s'. " +
                            "Please ensure SDK is configured with correct client credentials.",
                            clientId, appService.getDisplayName(), payload.getServiceName()));
        }

        log.debug("Authentication and credential validation passed for service: {} (clientId: {})",
                payload.getServiceName(), clientId);
    }

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
