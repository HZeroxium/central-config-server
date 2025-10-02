package com.example.control.api;

import com.example.control.application.ConfigProxyService;
import com.example.control.infrastructure.repository.ServiceInstanceDocument;
import com.example.control.infrastructure.repository.ServiceInstanceRepository;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for service discovery operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Service discovery and registry operations")
public class ServiceDiscoveryController {

    private final ConfigProxyService configProxyService;
    private final ServiceInstanceRepository instanceRepository;

    @GetMapping
    @Operation(summary = "List all services", description = "Get list of all registered services from Consul")
    @Timed(value = "api.services.list", description = "Time taken to list services")
    public ResponseEntity<Map<String, Object>> listServices() {
        List<String> services = configProxyService.getAllServices();

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "count", services.size(),
                "services", services));
    }

    @GetMapping("/{serviceName}")
    @Operation(summary = "Get service details", description = "Get details about a specific service and its instances")
    @Timed(value = "api.services.get", description = "Time taken to get service details")
    public ResponseEntity<Map<String, Object>> getService(
            @PathVariable @Parameter(description = "Service name") String serviceName) {

        List<ServiceInstance> consulInstances = configProxyService.getServiceInstances(serviceName);
        List<ServiceInstanceDocument> trackedInstances = instanceRepository.findByServiceName(serviceName);
        long instanceCount = instanceRepository.countByServiceName(serviceName);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "serviceName", serviceName,
                "consulInstanceCount", consulInstances.size(),
                "trackedInstanceCount", instanceCount,
                "consulInstances", consulInstances.stream()
                        .map(inst -> Map.of(
                                "instanceId", inst.getInstanceId(),
                                "host", inst.getHost(),
                                "port", inst.getPort(),
                                "metadata", inst.getMetadata()))
                        .collect(Collectors.toList()),
                "trackedInstances", trackedInstances.stream()
                        .map(doc -> Map.of(
                                "instanceId", doc.getInstanceId(),
                                "host", doc.getHost() != null ? doc.getHost() : "unknown",
                                "status", doc.getStatus(),
                                "hasDrift", doc.getHasDrift() != null ? doc.getHasDrift() : false,
                                "lastSeenAt", doc.getLastSeenAt() != null ? doc.getLastSeenAt().toString() : "never"))
                        .collect(Collectors.toList())));
    }

    @GetMapping("/{serviceName}/instances")
    @Operation(summary = "List service instances", description = "Get list of healthy instances from Consul for a service")
    @Timed(value = "api.services.instances", description = "Time taken to list service instances")
    public ResponseEntity<Map<String, Object>> listInstances(
            @PathVariable @Parameter(description = "Service name") String serviceName,
            @RequestParam(defaultValue = "true") @Parameter(description = "Only passing instances") boolean passing) {

        List<ServiceInstance> instances = configProxyService.getServiceInstances(serviceName);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "serviceName", serviceName,
                "count", instances.size(),
                "instances", instances.stream()
                        .map(inst -> {
                            Map<String, Object> instanceMap = new HashMap<>();
                            instanceMap.put("instanceId",
                                    inst.getInstanceId() != null ? inst.getInstanceId() : "unknown");
                            instanceMap.put("host", inst.getHost() != null ? inst.getHost() : "unknown");
                            instanceMap.put("port", inst.getPort());
                            instanceMap.put("scheme", inst.getScheme() != null ? inst.getScheme() : "http");
                            instanceMap.put("uri", inst.getUri() != null ? inst.getUri().toString() : "unknown");
                            instanceMap.put("metadata", inst.getMetadata() != null ? inst.getMetadata() : Map.of());
                            return instanceMap;
                        })
                        .collect(Collectors.toList())));
    }
}
