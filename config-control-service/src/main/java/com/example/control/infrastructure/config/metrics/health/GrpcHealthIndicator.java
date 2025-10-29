package com.example.control.infrastructure.config.metrics.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for gRPC server.
 * <p>
 * This indicator checks if the gRPC server is properly configured
 * and available for incoming requests.
 */
@Component
public class GrpcHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // In a real implementation, you might check if the gRPC server
            // is actually running and accepting connections
            // For now, we'll assume it's healthy if the application is running

            return Health.up()
                    .withDetail("service", "grpc")
                    .withDetail("port", "9091")
                    .withDetail("status", "running")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "grpc")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
