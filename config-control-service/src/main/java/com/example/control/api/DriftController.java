package com.example.control.api;

import com.example.control.application.ConfigProxyService;
import com.example.control.infrastructure.repository.DriftEventDocument;
import com.example.control.infrastructure.repository.DriftEventMongoRepository;
import com.example.control.infrastructure.repository.ServiceInstanceDocument;
import com.example.control.infrastructure.repository.ServiceInstanceMongoRepository;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for configuration drift detection and management.
 *
 * @deprecated Use endpoints under {@code /api/drift-events} and
 * {@code /api/service-instances} which provide full pageable filtering
 * via Ports & Adapters. This controller remains for backward compatibility
 * and will be removed in a future release.
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("/api/drift")
@RequiredArgsConstructor
@Tag(name = "Drift", description = "Configuration drift detection and tracking")
public class DriftController {

    private final ServiceInstanceMongoRepository instanceRepository;
    private final DriftEventMongoRepository driftEventRepository;
    private final ConfigProxyService configProxyService;

    @GetMapping
    @Operation(summary = "List all drift events", description = "Get all configuration drift events")
    @Timed(value = "api.drift.list", description = "Time taken to list drift events")
    public ResponseEntity<Map<String, Object>> listDriftEvents(
            @RequestParam(required = false) @Parameter(description = "Filter by service name") String serviceName,
            @RequestParam(required = false) @Parameter(description = "Only unresolved") Boolean unresolvedOnly) {

        List<DriftEventDocument> events;

        if (Boolean.TRUE.equals(unresolvedOnly)) {
            events = serviceName != null
                    ? driftEventRepository.findUnresolvedEventsByService(serviceName)
                    : driftEventRepository.findUnresolvedEvents();
        } else {
            events = serviceName != null
                    ? driftEventRepository.findByServiceName(serviceName)
                    : driftEventRepository.findAll();
        }

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "count", events.size(),
                "events", events.stream()
                        .map(doc -> {
                            Map<String, Object> eventMap = new HashMap<>();
                            eventMap.put("id", doc.getId() != null ? doc.getId() : "unknown");
                            eventMap.put("serviceName",
                                    doc.getServiceName() != null ? doc.getServiceName() : "unknown");
                            eventMap.put("instanceId", doc.getInstanceId() != null ? doc.getInstanceId() : "unknown");
                            eventMap.put("severity", doc.getSeverity() != null ? doc.getSeverity() : "unknown");
                            eventMap.put("status", doc.getStatus() != null ? doc.getStatus() : "unknown");
                            eventMap.put("expectedHash",
                                    doc.getExpectedHash() != null ? doc.getExpectedHash() : "unknown");
                            eventMap.put("appliedHash",
                                    doc.getAppliedHash() != null ? doc.getAppliedHash() : "unknown");
                            eventMap.put("detectedAt",
                                    doc.getDetectedAt() != null ? doc.getDetectedAt().toString() : "unknown");
                            eventMap.put("resolvedAt",
                                    doc.getResolvedAt() != null ? doc.getResolvedAt().toString() : "unknown");
                            return eventMap;
                        })
                        .collect(Collectors.toList())));
    }

    @GetMapping("/instances")
    @Operation(summary = "List instances with drift", description = "Get all service instances that have configuration drift")
    @Timed(value = "api.drift.instances", description = "Time taken to list drifted instances")
    public ResponseEntity<Map<String, Object>> listDriftedInstances(
            @RequestParam(required = false) @Parameter(description = "Filter by service name") String serviceName) {

        List<ServiceInstanceDocument> instances = serviceName != null
                ? instanceRepository.findByServiceNameWithDrift(serviceName)
                : instanceRepository.findAllWithDrift();

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "count", instances.size(),
                "instances", instances.stream()
                        .map(doc -> Map.of(
                                "serviceName", doc.getServiceName(),
                                "instanceId", doc.getInstanceId(),
                                "host", doc.getHost() != null ? doc.getHost() : "unknown",
                                "status", doc.getStatus(),
                                "expectedHash", doc.getConfigHash() != null ? doc.getConfigHash() : "unknown",
                                "appliedHash", doc.getLastAppliedHash() != null ? doc.getLastAppliedHash() : "unknown",
                                "driftDetectedAt",
                                doc.getDriftDetectedAt() != null ? doc.getDriftDetectedAt().toString() : "unknown",
                                "lastSeenAt", doc.getLastSeenAt() != null ? doc.getLastSeenAt().toString() : "never"))
                        .collect(Collectors.toList())));
    }

    @GetMapping("/{serviceName}/diff")
    @Operation(summary = "Get config diff", description = "Compare expected config hash vs applied hash for a service")
    @Timed(value = "api.drift.diff", description = "Time taken to compute config diff")
    public ResponseEntity<Map<String, Object>> getConfigDiff(
            @PathVariable @Parameter(description = "Service name") String serviceName,
            @RequestParam(required = false) @Parameter(description = "Profile/environment") String profile,
            @RequestParam(required = false) @Parameter(description = "Applied config hash") String appliedHash) {

        Map<String, Object> diff = configProxyService.getConfigDiff(serviceName, profile, appliedHash);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "diff", diff));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get drift statistics", description = "Get statistics about drift events and affected instances")
    @Timed(value = "api.drift.statistics", description = "Time taken to compute drift statistics")
    public ResponseEntity<Map<String, Object>> getDriftStatistics() {
        long totalDriftEvents = driftEventRepository.count();
        long unresolvedEvents = driftEventRepository.findUnresolvedEvents().size();
        long driftedInstances = instanceRepository.findAllWithDrift().size();

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "statistics", Map.of(
                        "totalDriftEvents", totalDriftEvents,
                        "unresolvedEvents", unresolvedEvents,
                        "driftedInstances", driftedInstances,
                        "resolvedEvents", totalDriftEvents - unresolvedEvents)));
    }
}
