package com.example.control.application.service.infra;

import com.example.control.application.command.ApplicationServiceCommandService;
import com.example.control.application.command.ServiceInstanceCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.service.DriftEventService;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.domain.model.ServiceInstance;
import com.example.control.domain.port.repository.ServiceInstanceRepositoryPort;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.DriftEventId;
import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.example.control.infrastructure.external.configserver.ConfigProxyService;
import com.example.control.infrastructure.observability.MetricsNames;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for batch processing of heartbeat payloads.
 * <p>
 * Processes multiple heartbeats in a single transaction to reduce database
 * write overhead. Implements the same business logic as HeartbeatService but
 * optimized for batch operations:
 * <ul>
 * <li>Batch loads ServiceInstances and ApplicationServices</li>
 * <li>Batch loads config hashes (with cache deduplication)</li>
 * <li>Processes all heartbeats in memory</li>
 * <li>Bulk upserts to MongoDB</li>
 * </ul>
 * <p>
 * This service maintains the same drift detection and refresh logic as the
 * synchronous HeartbeatService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatBatchService {

    private final ServiceInstanceCommandService serviceInstanceCommandService;
    private final ServiceInstanceRepositoryPort serviceInstanceRepository;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final ApplicationServiceCommandService applicationServiceCommandService;
    private final ConfigProxyService configProxyService;
    private final DriftEventService driftEventService;
    private final HeartbeatMetrics heartbeatMetrics;

    // In-memory state for drift backoff (shared with HeartbeatService if needed)
    private final ConcurrentHashMap<String, Integer> driftRetryCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> driftBackoffPow = new ConcurrentHashMap<>();

    /**
     * Processes a batch of heartbeat payloads.
     * <p>
     * Implements the same business logic as HeartbeatService.processHeartbeat()
     * but optimized for batch processing:
     * <ol>
     * <li>Batch load ServiceInstances by IDs</li>
     * <li>Batch load ApplicationServices by display names</li>
     * <li>Batch load config hashes (grouped by service:env)</li>
     * <li>Process each heartbeat in memory</li>
     * <li>Bulk upsert ServiceInstances</li>
     * <li>Save drift events if any</li>
     * </ol>
     *
     * @param payloads list of heartbeat payloads to process
     */
    @Transactional
    @Observed(name = MetricsNames.Heartbeat.BATCH_PROCESS, contextualName = "heartbeat-batch-process")
    public void processBatch(List<HeartbeatPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            log.debug("Empty payload list, skipping batch processing");
            return;
        }

        log.debug("Processing batch of {} heartbeats", payloads.size());
        Instant now = Instant.now();

        // 1. Batch load ServiceInstances
        Set<ServiceInstanceId> instanceIds = payloads.stream()
                .map(p -> ServiceInstanceId.of(p.getInstanceId()))
                .collect(Collectors.toSet());
        Map<String, ServiceInstance> instancesMap = loadInstancesBatch(instanceIds);

        // 2. Batch load ApplicationServices
        Set<String> serviceNames = payloads.stream()
                .map(HeartbeatPayload::getServiceName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, ApplicationService> appServicesMap = loadApplicationServicesBatch(serviceNames);

        // 3. Batch load config hashes (group by service:env for cache efficiency)
        Map<String, String> configHashesMap = loadConfigHashesBatch(payloads);

        // 4. Process each heartbeat in memory
        List<ServiceInstance> instancesToSave = new ArrayList<>();
        List<DriftEvent> driftEventsToSave = new ArrayList<>();
        Set<String> servicesToRefresh = new HashSet<>();

        for (HeartbeatPayload payload : payloads) {
            try {
                ServiceInstance instance = processHeartbeatInMemory(
                        payload, instancesMap, appServicesMap, configHashesMap, now);
                instancesToSave.add(instance);

                // Collect drift events and refresh triggers
                if (Boolean.TRUE.equals(instance.getHasDrift()) && instance.getDriftDetectedAt() != null) {
                    // New drift detected - create event
                    DriftEvent event = createDriftEvent(payload, instance);
                    driftEventsToSave.add(event);
                    servicesToRefresh.add(payload.getServiceName() + ":" + payload.getInstanceId());
                }
            } catch (Exception e) {
                log.error("Failed to process heartbeat for {}:{}", payload.getServiceName(),
                        payload.getInstanceId(), e);
                // Continue processing other heartbeats
            }
        }

        // 5. Bulk upsert ServiceInstances
        if (!instancesToSave.isEmpty()) {
            com.mongodb.bulk.BulkWriteResult result = serviceInstanceCommandService.bulkUpsert(instancesToSave);
            if (result != null) {
                heartbeatMetrics.recordMongodbWrites(result.getInsertedCount() + result.getModifiedCount());
            }
        }

        // 6. Save drift events in batch
        if (!driftEventsToSave.isEmpty()) {
            for (DriftEvent event : driftEventsToSave) {
                driftEventService.save(event);
            }
            heartbeatMetrics.recordDriftDetected();
        }

        // 7. Trigger refresh for drifted instances (async/batched if possible)
        for (String destination : servicesToRefresh) {
            try {
                configProxyService.triggerBusRefresh(destination);
                log.debug("Triggered refresh for drifted instance: {}", destination);
            } catch (Exception e) {
                log.error("Failed to trigger refresh for {}", destination, e);
            }
        }

        log.debug("Batch processing completed: {} instances processed, {} drift events created",
                instancesToSave.size(), driftEventsToSave.size());
    }

    /**
     * Batch loads ServiceInstances by their IDs.
     */
    private Map<String, ServiceInstance> loadInstancesBatch(Set<ServiceInstanceId> instanceIds) {
        if (instanceIds.isEmpty()) {
            return new HashMap<>();
        }

        // Use repository directly for batch query
        List<ServiceInstance> instances = serviceInstanceRepository.findAllByIds(instanceIds);

        return instances.stream()
                .collect(Collectors.toMap(
                        instance -> instance.getId().instanceId(),
                        instance -> instance));
    }

    /**
     * Batch loads ApplicationServices by display names.
     * <p>
     * Creates orphaned services for missing ones to maintain consistency.
     */
    private Map<String, ApplicationService> loadApplicationServicesBatch(Set<String> serviceNames) {
        if (serviceNames.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, ApplicationService> appServicesMap = applicationServiceQueryService
                .findByDisplayNamesMap(serviceNames);

        // Create orphaned services for missing ones
        Set<String> missingServices = new HashSet<>(serviceNames);
        missingServices.removeAll(appServicesMap.keySet());

        for (String displayName : missingServices) {
            try {
                ApplicationService orphanedService = ApplicationService.builder()
                        .id(ApplicationServiceId.of(UUID.randomUUID().toString()))
                        .displayName(displayName)
                        .ownerTeamId(null) // Orphaned
                        .environments(List.of("dev", "staging", "prod"))
                        .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                        .createdAt(Instant.now())
                        .createdBy("system")
                        .build();

                ApplicationService saved = applicationServiceCommandService.save(orphanedService);
                appServicesMap.put(displayName, saved);
                log.debug("Created orphaned ApplicationService: {} for displayName: {}", saved.getId(), displayName);
            } catch (Exception e) {
                log.error("Failed to create orphaned ApplicationService for displayName: {}", displayName, e);
            }
        }

        return appServicesMap;
    }

    /**
     * Batch loads config hashes grouped by service:environment.
     * <p>
     * Groups payloads by service:env to minimize cache misses and HTTP calls.
     */
    private Map<String, String> loadConfigHashesBatch(List<HeartbeatPayload> payloads) {
        // Group by serviceName:environment
        Map<String, List<HeartbeatPayload>> grouped = payloads.stream()
                .filter(p -> p.getServiceName() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getServiceName() + ":" + (p.getEnvironment() != null ? p.getEnvironment() : "default")));

        Map<String, String> hashes = new HashMap<>();
        for (Map.Entry<String, List<HeartbeatPayload>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split(":", 2);
            String serviceName = parts[0];
            String environment = parts.length > 1 ? parts[1] : "default";

            try {
                // Cache will handle deduplication
                String hash = configProxyService.getEffectiveConfigHash(serviceName, environment);
                hashes.put(key, hash);
            } catch (Exception e) {
                log.warn("Failed to load config hash for {}:{}", serviceName, environment, e);
                hashes.put(key, null);
            }
        }

        return hashes;
    }

    /**
     * Processes a single heartbeat in memory.
     * <p>
     * Replicates the logic from HeartbeatService.processHeartbeat() but operates
     * on in-memory objects without database writes.
     */
    private ServiceInstance processHeartbeatInMemory(
            HeartbeatPayload payload,
            Map<String, ServiceInstance> instancesMap,
            Map<String, ApplicationService> appServicesMap,
            Map<String, String> configHashesMap,
            Instant now) {

        String id = payload.getServiceName() + ":" + payload.getInstanceId();
        ServiceInstanceId instanceId = ServiceInstanceId.of(payload.getInstanceId());

        // Get or create instance
        ServiceInstance instance = instancesMap.getOrDefault(
                payload.getInstanceId(),
                ServiceInstance.builder()
                        .id(instanceId)
                        .status(ServiceInstance.InstanceStatus.HEALTHY)
                        .build());

        boolean isFirstHeartbeat = instance.getCreatedAt() == null;
        if (isFirstHeartbeat) {
            instance.setCreatedAt(now);
        }

        // Sync serviceId and teamId from ApplicationService
        ApplicationService appService = appServicesMap.get(payload.getServiceName());
        if (appService != null) {
            instance.setServiceId(appService.getId().id());
            instance.setTeamId(appService.getOwnerTeamId());

            // Merge environment if needed
            if (payload.getEnvironment() != null && !payload.getEnvironment().isEmpty()) {
                List<String> currentEnvironments = appService.getEnvironments();
                if (currentEnvironments == null || !currentEnvironments.contains(payload.getEnvironment())) {
                    List<String> merged = mergeEnvironments(currentEnvironments, payload.getEnvironment());
                    appService.setEnvironments(merged);
                    appService.setUpdatedAt(now);
                    // Note: We don't save here to avoid individual writes - could batch this too
                }
            }
        }

        // Update metadata
        instance.setHost(payload.getHost());
        instance.setPort(payload.getPort());
        instance.setEnvironment(payload.getEnvironment());
        instance.setVersion(payload.getVersion());
        instance.setLastAppliedHash(payload.getConfigHash());
        instance.setLastSeenAt(now);
        instance.setUpdatedAt(now);
        instance.setMetadata(payload.getMetadata());

        // Get expected hash
        String hashKey = payload.getServiceName() + ":" +
                (payload.getEnvironment() != null ? payload.getEnvironment() : "default");
        String expectedHash = configHashesMap.get(hashKey);

        // Drift detection
        if (expectedHash == null || payload.getConfigHash() == null) {
            instance.setStatus(ServiceInstance.InstanceStatus.UNKNOWN);
            instance.setHasDrift(false);
            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);
            return instance;
        }

        boolean hasDrift = !expectedHash.equals(payload.getConfigHash());

        // Handle drift cases (same logic as HeartbeatService)
        if (hasDrift && !Boolean.TRUE.equals(instance.getHasDrift())) {
            // Case A: New drift detected
            instance.setHasDrift(true);
            instance.setDriftDetectedAt(now);
            instance.setExpectedHash(expectedHash);
            instance.setConfigHash(expectedHash);
            instance.setStatus(ServiceInstance.InstanceStatus.DRIFT);
            driftRetryCount.put(id, 1);
            driftBackoffPow.put(id, 0);
        } else if (!hasDrift && Boolean.TRUE.equals(instance.getHasDrift())) {
            // Case B: Drift resolved
            instance.setHasDrift(false);
            instance.setDriftDetectedAt(null);
            instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);
            instance.setExpectedHash(expectedHash);
            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);
        } else if (!hasDrift && !Boolean.TRUE.equals(instance.getHasDrift())) {
            // Case C: Normal steady state
            if (instance.getStatus() != ServiceInstance.InstanceStatus.HEALTHY) {
                instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);
            }
            instance.setExpectedHash(expectedHash);
            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);
        } else if (hasDrift && Boolean.TRUE.equals(instance.getHasDrift())) {
            // Case D: Persistent drift
            int count = driftRetryCount.merge(id, 1, Integer::sum);
            int pow = driftBackoffPow.compute(id, (k, v) -> v == null ? 0 : Math.min(v, 4));
            int threshold = 1 << pow;
            if (count >= threshold) {
                driftRetryCount.put(id, 0);
                driftBackoffPow.put(id, Math.min(pow + 1, 4));
                // Refresh will be triggered after batch processing
            }
        }

        return instance;
    }

    /**
     * Creates a drift event for a heartbeat.
     */
    private DriftEvent createDriftEvent(HeartbeatPayload payload, ServiceInstance instance) {
        return DriftEvent.builder()
                .id(DriftEventId.of(UUID.randomUUID().toString()))
                .serviceName(payload.getServiceName())
                .instanceId(payload.getInstanceId())
                .serviceId(instance.getServiceId())
                .teamId(instance.getTeamId())
                .environment(instance.getEnvironment())
                .expectedHash(instance.getExpectedHash())
                .appliedHash(payload.getConfigHash())
                .severity(DriftEvent.DriftSeverity.MEDIUM)
                .status(DriftEvent.DriftStatus.DETECTED)
                .detectedAt(Instant.now())
                .detectedBy("heartbeat-batch-service")
                .notes("Drift detected via batch heartbeat processing")
                .build();
    }

    /**
     * Merges a new environment into the existing environments list.
     */
    private List<String> mergeEnvironments(List<String> currentEnvironments, String newEnvironment) {
        if (currentEnvironments == null || currentEnvironments.isEmpty()) {
            return List.of(newEnvironment);
        }
        if (currentEnvironments.contains(newEnvironment)) {
            return currentEnvironments;
        }
        return Stream.concat(currentEnvironments.stream(), Stream.of(newEnvironment))
                .sorted()
                .distinct()
                .toList();
    }
}

