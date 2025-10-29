package com.example.control.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for ServiceInstance cleanup scheduling.
 */
@Data
@Component
@ConfigurationProperties(prefix = "service-instance.cleanup")
public class ServiceInstanceCleanupProperties {

  /**
   * Whether cleanup is enabled.
   */
  private boolean enabled = true;

  /**
   * Threshold in minutes for marking instances as STALE (no heartbeat).
   */
  private int staleThresholdMinutes = 10;

  /**
   * Threshold in days for cleaning up old stale instances.
   */
  private int cleanupThresholdDays = 30;

  /**
   * Cron expression for scheduling cleanup jobs.
   */
  private String scheduleCron = "0 */5 * * * *"; // Every 5 minutes
}
