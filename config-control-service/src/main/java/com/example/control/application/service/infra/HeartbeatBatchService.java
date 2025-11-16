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
import com.mongodb.bulk.BulkWriteResult;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    @Qualifier("configHashFetchExecutor")
    private final AsyncTaskExecutor configHashFetchExecutor;

    // In-memory state for drift backoff (shared with HeartbeatService if needed)
    private final ConcurrentHashMap<String, Integer> driftRetryCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> driftBackoffPow = new ConcurrentHashMap<>();

    /**
     * Tracks drift state transitions during batch processing.
     * Used to determine when to create drift events and when to resolve them.
     */
    private enum DriftTransition {
        NEWLY_DETECTED,    // Case A: drift newly detected (create event)
        RESOLVED,          // Case B: drift resolved (resolve events)
        STEADY_NORMAL,     // Case C: normal state (may need to resolve orphaned events)
        PERSISTENT,        // Case D: persistent drift (no event, may refresh)
        NONE               // No state change
    }

    /**
     * Result of processing a single heartbeat in memory.
     * Contains the processed instance and the drift transition state.
     */
    private record ProcessHeartbeatResult(
            ServiceInstance instance,
            DriftTransition transition,
            boolean needsRefresh,  // For Case D: refresh when threshold reached
            String serviceName,    // Service name from payload (needed for resolveForInstance)
            String instanceId      // Instance ID from payload (needed for resolveForInstance)
    ) {}

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
        Set<ApplicationService> appServicesToSave = new HashSet<>();
        Map<String, ApplicationService> appServicesMap = loadApplicationServicesBatch(serviceNames, appServicesToSave);

        // 3. Batch load config hashes (group by service:env for cache efficiency)
        Map<String, String> configHashesMap = loadConfigHashesBatch(payloads);

        // 4. Process each heartbeat in memory
        List<ServiceInstance> instancesToSave = new ArrayList<>();
        List<DriftEvent> driftEventsToSave = new ArrayList<>();
        Set<String> servicesToRefresh = new HashSet<>();
        List<ProcessHeartbeatResult> resolutionNeeded = new ArrayList<>(); // For Case B & C

        for (HeartbeatPayload payload : payloads) {
            try {
                ProcessHeartbeatResult result = processHeartbeatInMemory(
                        payload, instancesMap, appServicesMap, configHashesMap, now, appServicesToSave);
                instancesToSave.add(result.instance());

                // Handle drift transitions
                if (result.transition() == DriftTransition.NEWLY_DETECTED) {
                    // Case A: New drift detected - create event
                    DriftEvent event = createDriftEvent(payload, result.instance());
                    driftEventsToSave.add(event);
                    servicesToRefresh.add(payload.getServiceName() + ":" + payload.getInstanceId());
                    log.debug("New drift detected for {}:{}", payload.getServiceName(), payload.getInstanceId());
                } else if (result.transition() == DriftTransition.RESOLVED) {
                    // Case B: Drift resolved - collect for batch resolution
                    resolutionNeeded.add(result);
                    log.debug("Drift resolved for {}:{}", payload.getServiceName(), payload.getInstanceId());
                } else if (result.transition() == DriftTransition.STEADY_NORMAL) {
                    // Case C: Normal steady state - may need to resolve orphaned events
                    resolutionNeeded.add(result);
                } else if (result.transition() == DriftTransition.PERSISTENT && result.needsRefresh()) {
                    // Case D: Persistent drift - refresh when threshold reached
                    servicesToRefresh.add(payload.getServiceName() + ":" + payload.getInstanceId());
                    log.debug("Persistent drift refresh triggered for {}:{}", payload.getServiceName(), payload.getInstanceId());
                }
                // NONE transition: no action needed
            } catch (Exception e) {
                log.error("Failed to process heartbeat for {}:{}", payload.getServiceName(),
                        payload.getInstanceId(), e);
                // Continue processing other heartbeats
            }
        }

        // 5. Bulk save ApplicationServices (orphaned services and environment merges)
        if (!appServicesToSave.isEmpty()) {
            List<ApplicationService> servicesList = new ArrayList<>(appServicesToSave);
            BulkWriteResult result = applicationServiceCommandService.bulkSave(servicesList);
            if (result != null) {
                log.debug("Bulk saved {} application services: {} inserted, {} modified",
                        servicesList.size(), result.getInsertedCount(), result.getModifiedCount());
                // Update appServicesMap with saved services for consistency
                for (ApplicationService service : servicesList) {
                    appServicesMap.put(service.getDisplayName(), service);
                }
            }
        }

        // 6. Bulk upsert ServiceInstances
        if (!instancesToSave.isEmpty()) {
            BulkWriteResult result = serviceInstanceCommandService.bulkUpsert(instancesToSave);
            if (result != null) {
                heartbeatMetrics.recordMongodbWrites(result.getInsertedCount() + result.getModifiedCount());
            }
        }

        // 7. Save drift events in batch (only for newly detected drift)
        if (!driftEventsToSave.isEmpty()) {
            driftEventService.bulkSave(driftEventsToSave);
            heartbeatMetrics.recordDriftDetected();
        }

        // 8. Batch resolve drift events for instances that recovered (Case B & C)
        if (!resolutionNeeded.isEmpty()) {
            for (ProcessHeartbeatResult result : resolutionNeeded) {
                try {
                    String serviceName = result.serviceName();
                    String instanceId = result.instanceId();
                    if (serviceName != null && instanceId != null) {
                        driftEventService.resolveForInstance(serviceName, instanceId, "heartbeat-batch-service");
                        log.debug("Resolved drift events for {}:{}", serviceName, instanceId);
                    }
                } catch (Exception e) {
                    log.error("Failed to resolve drift events for instance: {}:{}", 
                            result.serviceName(), result.instanceId(), e);
                    // Continue with other resolutions
                }
            }
            log.debug("Resolved drift events for {} instances", resolutionNeeded.size());
        }

        // 9. Trigger refresh for drifted instances (batched by service)
        if (!servicesToRefresh.isEmpty()) {
            triggerBatchBusRefresh(servicesToRefresh);
        }

        log.debug("Batch processing completed: {} instances processed, {} drift events created, {} app services saved",
                instancesToSave.size(), driftEventsToSave.size(), appServicesToSave.size());
    }

    /**
     * Triggers batch bus refresh grouped by service name with parallel execution.
     * <p>
     * Groups refresh destinations by service name and triggers one refresh per service
     * to reduce HTTP calls to Config Server. Executes refresh calls in parallel using
     * CompletableFuture for better performance.
     *
     * @param destinations set of destination strings in format "serviceName:instanceId"
     */
    private void triggerBatchBusRefresh(Set<String> destinations) {
        if (destinations.isEmpty()) {
            return;
        }

        // Extract unique service names from destinations
        Set<String> uniqueServiceNames = destinations.stream()
                .map(dest -> {
                    int colonIndex = dest.indexOf(':');
                    return colonIndex > 0 ? dest.substring(0, colonIndex) : dest;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.debug("Triggering batch bus refresh for {} unique services (from {} destinations)",
                uniqueServiceNames.size(), destinations.size());

        // Trigger refresh calls in parallel using CompletableFuture
        // Each service refresh runs independently with individual error handling
        List<CompletableFuture<Void>> refreshTasks = uniqueServiceNames.stream()
                .map(serviceName -> CompletableFuture.runAsync(() -> {
                    try {
                        // Use service name as destination (Config Server may support service:** pattern)
                        configProxyService.triggerBusRefresh(serviceName);
                        log.debug("Triggered refresh for service: {}", serviceName);
                    } catch (Exception e) {
                        log.error("Failed to trigger refresh for service: {}", serviceName, e);
                        // Don't propagate exception - allow other refreshes to continue
                    }
                }, configHashFetchExecutor))
                .collect(Collectors.toList());

        // Wait for all refresh tasks to complete (with timeout protection)
        try {
            CompletableFuture<Void> allRefreshes = CompletableFuture.allOf(
                    refreshTasks.toArray(new CompletableFuture[0]));
            allRefreshes.join(); // Wait for completion
            log.debug("Completed batch bus refresh for {} services", uniqueServiceNames.size());
        } catch (Exception e) {
            log.warn("Some bus refresh operations may have failed", e);
            // Continue - individual errors are already logged
        }
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
     * Orphaned services are collected and will be bulk saved later.
     *
     * @param serviceNames set of service display names to load
     * @param appServicesToSave set to collect ApplicationServices that need to be saved
     * @return map of display name to ApplicationService
     */
    private Map<String, ApplicationService> loadApplicationServicesBatch(
            Set<String> serviceNames, Set<ApplicationService> appServicesToSave) {
        if (serviceNames.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, ApplicationService> appServicesMap = applicationServiceQueryService
                .findByDisplayNamesMap(serviceNames);

        // Create orphaned services for missing ones (collect for bulk save)
        Set<String> missingServices = new HashSet<>(serviceNames);
        missingServices.removeAll(appServicesMap.keySet());

        Instant now = Instant.now();
        for (String displayName : missingServices) {
            try {
                ApplicationService orphanedService = ApplicationService.builder()
                        .id(ApplicationServiceId.of(UUID.randomUUID().toString()))
                        .displayName(displayName)
                        .ownerTeamId(null) // Orphaned
                        .environments(List.of("dev", "staging", "prod"))
                        .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                        .createdAt(now)
                        .createdBy("system")
                        .build();

                // Add to map for immediate use and to save set for bulk save
                appServicesMap.put(displayName, orphanedService);
                appServicesToSave.add(orphanedService);
                log.debug("Prepared orphaned ApplicationService: {} for displayName: {}", orphanedService.getId(), displayName);
            } catch (Exception e) {
                log.error("Failed to create orphaned ApplicationService for displayName: {}", displayName, e);
            }
        }

        return appServicesMap;
    }

    /**
     * Batch loads config hashes grouped by service:environment with parallel fetching.
     * <p>
     * Groups payloads by service:env to minimize cache misses and HTTP calls.
     * Fetches config hashes in parallel using CompletableFuture for better performance.
     */
    @Observed(name = "heartbeat.batch.config-hash-fetch", contextualName = "heartbeat-batch-config-hash-fetch")
    private Map<String, String> loadConfigHashesBatch(List<HeartbeatPayload> payloads) {
        // Group by serviceName:environment
        Map<String, List<HeartbeatPayload>> grouped = payloads.stream()
                .filter(p -> p.getServiceName() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getServiceName() + ":" + (p.getEnvironment() != null ? p.getEnvironment() : "default")));

        // Create parallel fetch tasks for each unique service:env combination
        // Use dedicated configHashFetchExecutor to avoid saturating common pool
        List<CompletableFuture<Map.Entry<String, String>>> fetchTasks = grouped.keySet().stream()
                .map(key -> {
                    String[] parts = key.split(":", 2);
                    String serviceName = parts[0];
                    String environment = parts.length > 1 ? parts[1] : "default";
                    
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            // Cache will handle deduplication
                            String hash = configProxyService.getEffectiveConfigHash(serviceName, environment);
                            return Map.entry(key, hash);
                        } catch (Exception e) {
                            log.warn("Failed to load config hash for {}:{}", serviceName, environment, e);
                            return Map.entry(key, (String) null);
                        }
                    }, configHashFetchExecutor);
                })
                .collect(Collectors.toList());

        // Wait for all fetches to complete
        CompletableFuture<Void> allFetches = CompletableFuture.allOf(
                fetchTasks.toArray(new CompletableFuture[0]));

        Map<String, String> hashes = new HashMap<>();
        try {
            allFetches.join(); // Wait for all parallel fetches to complete
            
            // Collect results
            for (CompletableFuture<Map.Entry<String, String>> task : fetchTasks) {
                Map.Entry<String, String> entry = task.join();
                hashes.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            log.error("Error during parallel config hash fetch", e);
            // Fallback: return empty map or partial results
        }

        return hashes;
    }

    /**
     * Processes a single heartbeat in memory.
     * <p>
     * Replicates the logic from HeartbeatService.processHeartbeat() but operates
     * on in-memory objects without database writes. Tracks drift transition state
     * to determine when to create/resolve drift events.
     *
     * @param payload heartbeat payload
     * @param instancesMap map of instance ID to ServiceInstance
     * @param appServicesMap map of service name to ApplicationService
     * @param configHashesMap map of service:env to config hash
     * @param now current timestamp
     * @param appServicesToSave set to collect ApplicationServices that need to be saved
     * @return ProcessHeartbeatResult containing processed instance and drift transition state
     */
    private ProcessHeartbeatResult processHeartbeatInMemory(
            HeartbeatPayload payload,
            Map<String, ServiceInstance> instancesMap,
            Map<String, ApplicationService> appServicesMap,
            Map<String, String> configHashesMap,
            Instant now,
            Set<ApplicationService> appServicesToSave) {

        String id = payload.getServiceName() + ":" + payload.getInstanceId();
        ServiceInstanceId instanceId = ServiceInstanceId.of(payload.getInstanceId());

        // Get or create instance
        ServiceInstance instance = instancesMap.getOrDefault(
                payload.getInstanceId(),
                ServiceInstance.builder()
                        .id(instanceId)
                        .status(ServiceInstance.InstanceStatus.HEALTHY)
                        .build());

        // Store previous drift state BEFORE processing to detect transitions
        Boolean previousHasDrift = instance.getHasDrift();
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
                    // Mark for bulk save (environment merge)
                    appServicesToSave.add(appService);
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
        DriftTransition transition = DriftTransition.NONE;
        boolean needsRefresh = false;

        if (expectedHash == null || payload.getConfigHash() == null) {
            instance.setStatus(ServiceInstance.InstanceStatus.UNKNOWN);
            instance.setHasDrift(false);
            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);
            return new ProcessHeartbeatResult(instance, DriftTransition.NONE, false, 
                    payload.getServiceName(), payload.getInstanceId());
        }

        boolean hasDrift = !expectedHash.equals(payload.getConfigHash());

        // Handle drift cases (same logic as HeartbeatService)
        if (hasDrift && !Boolean.TRUE.equals(previousHasDrift)) {
            // Case A: New drift detected
            instance.setHasDrift(true);
            instance.setDriftDetectedAt(now);
            instance.setExpectedHash(expectedHash);
            instance.setConfigHash(expectedHash);
            instance.setStatus(ServiceInstance.InstanceStatus.DRIFT);
            driftRetryCount.put(id, 1);
            driftBackoffPow.put(id, 0);
            transition = DriftTransition.NEWLY_DETECTED;
            needsRefresh = true; // Always refresh on new detection
        } else if (!hasDrift && Boolean.TRUE.equals(previousHasDrift)) {
            // Case B: Drift resolved - config hash now matches expected
            instance.setHasDrift(false);
            instance.setDriftDetectedAt(null);
            instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);
            instance.setExpectedHash(expectedHash);
            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);
            transition = DriftTransition.RESOLVED;
        } else if (!hasDrift && !Boolean.TRUE.equals(previousHasDrift)) {
            // Case C: Normal steady-state heartbeat - ensure any orphaned events are resolved
            if (instance.getStatus() != ServiceInstance.InstanceStatus.HEALTHY) {
                instance.setStatus(ServiceInstance.InstanceStatus.HEALTHY);
            }
            instance.setExpectedHash(expectedHash);
            driftRetryCount.remove(id);
            driftBackoffPow.remove(id);
            transition = DriftTransition.STEADY_NORMAL;
        } else if (hasDrift && Boolean.TRUE.equals(previousHasDrift)) {
            // Case D: Persistent drift — apply exponential backoff strategy
            int count = driftRetryCount.merge(id, 1, Integer::sum);
            int pow = driftBackoffPow.compute(id, (k, v) -> v == null ? 0 : Math.min(v, 4)); // limit to 16 cycles
            int threshold = 1 << pow; // 1, 2, 4, 8, 16
            if (count >= threshold) {
                log.debug("Persistent drift for {} after {} heartbeats (threshold {}). Re-triggering refresh.",
                        id, count, threshold);
                driftRetryCount.put(id, 0);
                driftBackoffPow.put(id, Math.min(pow + 1, 4));
                needsRefresh = true; // Only refresh when threshold reached
            }
            transition = DriftTransition.PERSISTENT;
        }

        return new ProcessHeartbeatResult(instance, transition, needsRefresh, 
                payload.getServiceName(), payload.getInstanceId());
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

