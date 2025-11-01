package com.example.control.infrastructure.config.metrics;

import com.example.control.infrastructure.resilience.RetryBudgetTracker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Custom metrics for resilience patterns.
 * <p>
 * Registers metrics for:
 * - Retry budget utilization per service
 * <p>
 * Note: Circuit breaker metrics are provided by Resilience4j's native
 * Micrometer
 * integration ({@code resilience4j_circuitbreaker_*} metrics). No custom
 * circuit
 * breaker gauges are needed.
 */
@Slf4j
@Component
public class ResilienceMetrics {

  private final MeterRegistry meterRegistry;
  private final RetryBudgetTracker retryBudgetTracker;

  public ResilienceMetrics(
      MeterRegistry meterRegistry,
      RetryBudgetTracker retryBudgetTracker,
      CircuitBreakerRegistry circuitBreakerRegistry) {
    this.meterRegistry = meterRegistry;
    this.retryBudgetTracker = retryBudgetTracker;
    // CircuitBreakerRegistry parameter kept for backward compatibility but not used
    // Circuit breaker metrics are provided by Resilience4j Micrometer integration
  }

  @PostConstruct
  public void registerCustomMetrics() {
    log.info("Registering custom resilience metrics (retry budget utilization)");

    // Register retry budget utilization gauges for each service
    registerRetryBudgetGauges();
  }

  /**
   * Register retry budget utilization gauges for key services.
   * <p>
   * This is custom business logic not provided by Resilience4j's native metrics.
   */
  private void registerRetryBudgetGauges() {
    String[] services = { "configserver", "consul", "keycloak" };

    for (String service : services) {
      Gauge.builder("retry.budget.utilization", retryBudgetTracker, tracker -> tracker.getRetryUtilization(service))
          .tag("service", service)
          .description("Retry budget utilization percentage for " + service)
          .baseUnit("percent")
          .register(meterRegistry);

      log.debug("Registered retry budget utilization gauge for service: {}", service);
    }
  }
}
