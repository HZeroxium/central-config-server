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
 * - Deadline propagation (expired, propagated counts)
 * - Retry budget utilization per service
 * - Circuit breaker state gauges
 * </p>
 */
@Slf4j
@Component
public class ResilienceMetrics {

  private final MeterRegistry meterRegistry;
  private final RetryBudgetTracker retryBudgetTracker;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  public ResilienceMetrics(
      MeterRegistry meterRegistry,
      RetryBudgetTracker retryBudgetTracker,
      CircuitBreakerRegistry circuitBreakerRegistry) {
    this.meterRegistry = meterRegistry;
    this.retryBudgetTracker = retryBudgetTracker;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
  }

  @PostConstruct
  public void registerCustomMetrics() {
    log.info("Registering custom resilience metrics");

    // Register retry budget utilization gauges for each service
    registerRetryBudgetGauges();

    // Register circuit breaker listeners to track state
    registerCircuitBreakerListeners();
  }

  /**
   * Register retry budget utilization gauges for key services.
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

  /**
   * Register listeners for circuit breaker state changes.
   */
  private void registerCircuitBreakerListeners() {
    circuitBreakerRegistry.getEventPublisher()
        .onEntryAdded(event -> {
          String cbName = event.getAddedEntry().getName();
          log.info("Circuit breaker added: {}", cbName);
          registerCircuitBreakerStateGauge(cbName);
        });

    // Register gauges for existing circuit breakers
    circuitBreakerRegistry.getAllCircuitBreakers()
        .forEach(cb -> registerCircuitBreakerStateGauge(cb.getName()));
  }

  /**
   * Register a gauge for circuit breaker state.
   */
  private void registerCircuitBreakerStateGauge(String circuitBreakerName) {
    Gauge.builder("circuitbreaker.state", () -> {
      var cb = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
      return switch (cb.getState()) {
        case CLOSED -> 0;
        case HALF_OPEN -> 1;
        case OPEN -> 2;
        case DISABLED -> 3;
        case FORCED_OPEN -> 4;
        default -> -1;
      };
    })
        .tag("name", circuitBreakerName)
        .description("Circuit breaker state (0=CLOSED, 1=HALF_OPEN, 2=OPEN, 3=DISABLED, 4=FORCED_OPEN)")
        .register(meterRegistry);

    log.debug("Registered circuit breaker state gauge for: {}", circuitBreakerName);
  }
}
