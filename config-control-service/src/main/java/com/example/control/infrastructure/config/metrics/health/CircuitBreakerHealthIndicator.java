package com.example.control.infrastructure.config.metrics.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Custom health indicator for circuit breakers.
 * <p>
 * Shows the state of all circuit breakers in the /actuator/health endpoint.
 * Health is DOWN if any critical circuit breaker is OPEN.
 * </p>
 * <p>
 * Critical circuit breakers: configserver, consul (infrastructure dependencies)
 * Non-critical: keycloak, email (degraded mode acceptable)
 * </p>
 */
@Component("circuitBreakerHealth")
@RequiredArgsConstructor
public class CircuitBreakerHealthIndicator implements HealthIndicator {

  private final CircuitBreakerRegistry circuitBreakerRegistry;

  // Circuit breakers that cause health DOWN when OPEN
  private static final Set<String> CRITICAL_CIRCUIT_BREAKERS = Set.of(
      "configserver",
      "consul");

  @Override
  public Health health() {
    Map<String, Object> details = new HashMap<>();
    boolean anyCriticalOpen = false;
    int totalCircuitBreakers = 0;
    int openCircuitBreakers = 0;

    for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
      totalCircuitBreakers++;
      String name = circuitBreaker.getName();
      CircuitBreaker.State state = circuitBreaker.getState();

      Map<String, Object> cbDetails = new HashMap<>();
      cbDetails.put("state", state.name());
      cbDetails.put("failureRate", String.format("%.2f%%", circuitBreaker.getMetrics().getFailureRate()));
      cbDetails.put("slowCallRate", String.format("%.2f%%", circuitBreaker.getMetrics().getSlowCallRate()));
      cbDetails.put("bufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
      cbDetails.put("failedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
      cbDetails.put("slowCalls", circuitBreaker.getMetrics().getNumberOfSlowCalls());

      details.put(name, cbDetails);

      if (state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN) {
        openCircuitBreakers++;
        if (CRITICAL_CIRCUIT_BREAKERS.contains(name)) {
          anyCriticalOpen = true;
        }
      }
    }

    details.put("summary", Map.of(
        "total", totalCircuitBreakers,
        "open", openCircuitBreakers,
        "closed", totalCircuitBreakers - openCircuitBreakers));

    if (anyCriticalOpen) {
      return Health.down()
          .withDetail("reason", "One or more critical circuit breakers are OPEN")
          .withDetails(details)
          .build();
    } else if (openCircuitBreakers > 0) {
      return Health.up()
          .withDetail("warning", "Some non-critical circuit breakers are OPEN (degraded mode)")
          .withDetails(details)
          .build();
    } else {
      return Health.up()
          .withDetail("message", "All circuit breakers are healthy")
          .withDetails(details)
          .build();
    }
  }
}
