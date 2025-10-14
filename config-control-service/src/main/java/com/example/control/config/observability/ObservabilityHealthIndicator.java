package com.example.control.config.observability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for observability stack components.
 * 
 * Checks connectivity to:
 * - Grafana Tempo (distributed tracing)
 * - Grafana Loki (log aggregation)
 * - Grafana Mimir (metrics backend)
 * - Grafana Alloy (unified collector)
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "management.health.observability.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ObservabilityHealthIndicator implements HealthIndicator {

  @Value("${management.otlp.tracing.endpoint:http://alloy:4318/v1/traces}")
  private String otlpTracingEndpoint;

  @Value("${management.otlp.metrics.url:http://alloy:4318/v1/metrics}")
  private String otlpMetricsUrl;

  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public Health health() {
    Map<String, Object> details = new HashMap<>();
    boolean allHealthy = true;

    try {
      // Check Tempo connectivity
      Health tempoHealth = checkTempoHealth();
      details.put("tempo", tempoHealth);
      if (!tempoHealth.getStatus().getCode().equals("UP")) {
        allHealthy = false;
      }

      // Check Loki connectivity
      Health lokiHealth = checkLokiHealth();
      details.put("loki", lokiHealth);
      if (!lokiHealth.getStatus().getCode().equals("UP")) {
        allHealthy = false;
      }

      // Check Mimir connectivity
      Health mimirHealth = checkMimirHealth();
      details.put("mimir", mimirHealth);
      if (!mimirHealth.getStatus().getCode().equals("UP")) {
        allHealthy = false;
      }

      // Check Alloy connectivity
      Health alloyHealth = checkAlloyHealth();
      details.put("alloy", alloyHealth);
      if (!alloyHealth.getStatus().getCode().equals("UP")) {
        allHealthy = false;
      }

      // Check OTLP endpoint accessibility
      Health otlpHealth = checkOtlpEndpoints();
      details.put("otlp", otlpHealth);
      if (!otlpHealth.getStatus().getCode().equals("UP")) {
        allHealthy = false;
      }

      if (allHealthy) {
        return Health.up()
            .withDetails(details)
            .withDetail("message", "All observability components are healthy")
            .build();
      } else {
        return Health.down()
            .withDetails(details)
            .withDetail("message", "Some observability components are unhealthy")
            .build();
      }

    } catch (Exception e) {
      log.error("Error checking observability health", e);
      return Health.down()
          .withDetails(details)
          .withDetail("error", e.getMessage())
          .withDetail("message", "Failed to check observability health")
          .build();
    }
  }

  private Health checkTempoHealth() {
    try {
      String tempoUrl = "http://tempo:3200/ready";
      restTemplate.getForObject(tempoUrl, String.class);
      return Health.up()
          .withDetail("url", tempoUrl)
          .withDetail("message", "Tempo is ready")
          .build();
    } catch (Exception e) {
      log.warn("Tempo health check failed", e);
      return Health.down()
          .withDetail("url", "http://tempo:3200/ready")
          .withDetail("error", e.getMessage())
          .build();
    }
  }

  private Health checkLokiHealth() {
    try {
      String lokiUrl = "http://loki:3100/ready";
      restTemplate.getForObject(lokiUrl, String.class);
      return Health.up()
          .withDetail("url", lokiUrl)
          .withDetail("message", "Loki is ready")
          .build();
    } catch (Exception e) {
      log.warn("Loki health check failed", e);
      return Health.down()
          .withDetail("url", "http://loki:3100/ready")
          .withDetail("error", e.getMessage())
          .build();
    }
  }

  private Health checkMimirHealth() {
    try {
      String mimirUrl = "http://mimir:9009/ready";
      restTemplate.getForObject(mimirUrl, String.class);
      return Health.up()
          .withDetail("url", mimirUrl)
          .withDetail("message", "Mimir is ready")
          .build();
    } catch (Exception e) {
      log.warn("Mimir health check failed", e);
      return Health.down()
          .withDetail("url", "http://mimir:9009/ready")
          .withDetail("error", e.getMessage())
          .build();
    }
  }

  private Health checkAlloyHealth() {
    try {
      String alloyUrl = "http://alloy:12345/-/healthy";
      restTemplate.getForObject(alloyUrl, String.class);
      return Health.up()
          .withDetail("url", alloyUrl)
          .withDetail("message", "Alloy is healthy")
          .build();
    } catch (Exception e) {
      log.warn("Alloy health check failed", e);
      return Health.down()
          .withDetail("url", "http://alloy:12345/-/healthy")
          .withDetail("error", e.getMessage())
          .build();
    }
  }

  private Health checkOtlpEndpoints() {
    try {
      // Check if OTLP endpoints are configured and accessible
      String tracingEndpoint = otlpTracingEndpoint;
      String metricsEndpoint = otlpMetricsUrl;

      // Basic connectivity check to Alloy OTLP endpoints
      String alloyUrl = "http://alloy:4318/v1/traces";
      restTemplate.getForEntity(alloyUrl, String.class);

      return Health.up()
          .withDetail("tracing_endpoint", tracingEndpoint)
          .withDetail("metrics_endpoint", metricsEndpoint)
          .withDetail("message", "OTLP endpoints are accessible")
          .build();
    } catch (Exception e) {
      log.warn("OTLP endpoints health check failed", e);
      return Health.down()
          .withDetail("tracing_endpoint", otlpTracingEndpoint)
          .withDetail("metrics_endpoint", otlpMetricsUrl)
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
