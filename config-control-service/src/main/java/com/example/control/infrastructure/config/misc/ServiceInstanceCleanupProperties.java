package com.example.control.infrastructure.config.misc;

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
   * Threshold in minutes for marking instances as UNHEALTHY (no heartbeat).
   * Default: 5 minutes
   */
  private int unhealthyThresholdMinutes = 5;

  /**
   * Threshold in days for cleaning up old UNHEALTHY instances.
   * Default: 30 days
   */
  private int unhealthyCleanupThresholdDays = 30;

  /**
   * Cron expression for scheduling cleanup jobs.
   */
  private String scheduleCron = "0 */5 * * * *"; // Every 5 minutes

  /**
   * Threshold in minutes for marking instances as STALE (no heartbeat).
   * 
   * @deprecated Use {@link #unhealthyThresholdMinutes} instead. This property is kept for backward compatibility.
   */
  @Deprecated
  private int staleThresholdMinutes = 10;

  /**
   * Threshold in days for cleaning up old stale instances.
   * 
   * @deprecated Use {@link #unhealthyCleanupThresholdDays} instead. This property is kept for backward compatibility.
   */
  @Deprecated
  private int cleanupThresholdDays = 30;
}
