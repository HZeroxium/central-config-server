package com.example.control.infrastructure.config.resilience;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for custom resilience settings.
 * <p>
 * Maps properties from {@code resilience.*} in application.yml.
 * Includes retry budget and deadline propagation settings.
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "resilience")
public class ResilienceProperties {

  /**
   * Retry budget configuration for preventing cascading failures.
   */
  private RetryBudget retryBudget = new RetryBudget();

  /**
   * Deadline propagation configuration for coordinated timeouts.
   */
  private DeadlinePropagation deadlinePropagation = new DeadlinePropagation();

  @Data
  public static class RetryBudget {
    /**
     * Enable retry budget tracking.
     */
    private boolean enabled = true;

    /**
     * Sliding window size for retry budget calculation.
     */
    private Duration windowSize = Duration.ofSeconds(10);

    /**
     * Maximum percentage of requests that can be retries within the window.
     */
    private int maxRetryPercentage = 20;

    /**
     * Per-service retry budget overrides.
     */
    private Map<String, ServiceBudget> perService = new HashMap<>();

    @Data
    public static class ServiceBudget {
      private int maxRetryPercentage;
    }
  }

  @Data
  public static class DeadlinePropagation {
    /**
     * Enable deadline propagation across service calls.
     */
    private boolean enabled = true;

    /**
     * Default timeout for requests without explicit deadline.
     */
    private Duration defaultTimeout = Duration.ofSeconds(30);

    /**
     * HTTP header name for deadline propagation.
     */
    private String headerName = "X-Request-Deadline";
  }
}
