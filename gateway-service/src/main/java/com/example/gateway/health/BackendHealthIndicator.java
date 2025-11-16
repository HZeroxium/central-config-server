package com.example.gateway.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Health indicator that checks backend service availability.
 * <p>
 * Verifies that at least one instance of config-control-service is available
 * via service discovery. This ensures gateway readiness depends on backend availability.
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackendHealthIndicator implements HealthIndicator {

    private static final String BACKEND_SERVICE_NAME = "config-control-service";
    
    private final DiscoveryClient discoveryClient;

    @Override
    public Health health() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(BACKEND_SERVICE_NAME);
            
            if (instances == null || instances.isEmpty()) {
                log.warn("No backend instances available for service: {}", BACKEND_SERVICE_NAME);
                return Health.down()
                        .withDetail("service", BACKEND_SERVICE_NAME)
                        .withDetail("availableInstances", 0)
                        .withDetail("message", "No backend instances available")
                        .build();
            }
            
            log.debug("Backend health check: {} instances available for service: {}", 
                    instances.size(), BACKEND_SERVICE_NAME);
            
            return Health.up()
                    .withDetail("service", BACKEND_SERVICE_NAME)
                    .withDetail("availableInstances", instances.size())
                    .withDetail("instances", instances.stream()
                            .map(instance -> String.format("%s:%s", 
                                    instance.getHost(), instance.getPort()))
                            .toList())
                    .build();
        } catch (Exception e) {
            log.error("Error checking backend health", e);
            return Health.down()
                    .withDetail("service", BACKEND_SERVICE_NAME)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

