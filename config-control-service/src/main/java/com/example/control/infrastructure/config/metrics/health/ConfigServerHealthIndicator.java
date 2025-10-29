package com.example.control.infrastructure.config.metrics.health;

import com.example.control.infrastructure.config.ConfigServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Health indicator for Config Server connectivity.
 * <p>
 * This indicator checks if the Config Server is reachable and responding
 * to health check requests.
 */
@Component
@RequiredArgsConstructor
public class ConfigServerHealthIndicator implements HealthIndicator {

    private final ConfigServerProperties configServerProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Health health() {
        try {
            String healthUrl = configServerProperties.getUrl() + "/actuator/health";

            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                        .withDetail("service", "config-server")
                        .withDetail("url", configServerProperties.getUrl())
                        .withDetail("status", "reachable")
                        .withDetail("responseCode", response.getStatusCode().value())
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "config-server")
                        .withDetail("url", configServerProperties.getUrl())
                        .withDetail("status", "unreachable")
                        .withDetail("responseCode", response.getStatusCode().value())
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "config-server")
                    .withDetail("url", configServerProperties.getUrl())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
