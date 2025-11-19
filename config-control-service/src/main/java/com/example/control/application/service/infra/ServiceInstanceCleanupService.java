package com.example.control.application.service.infra;

import com.example.control.application.command.ServiceInstanceCommandService;
import com.example.control.application.query.ServiceInstanceQueryService;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.model.ServiceInstance;
import com.example.control.infrastructure.cache.ServiceInstanceCacheEvictionService;
import com.example.control.infrastructure.config.misc.ServiceInstanceCleanupProperties;
import com.example.control.infrastructure.observability.MetricsNames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled service for cleaning up unhealthy service instances.
 * <p>
 * This service periodically:
 * <ul>
 * <li>Marks instances as UNHEALTHY if they haven't sent a heartbeat within the
 * threshold (default: 5 minutes)</li>
 * <li>Deletes instances that have been UNHEALTHY for longer than the cleanup
 * threshold (default: 30 days)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "service-instance.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class ServiceInstanceCleanupService {

  private final ServiceInstanceQueryService queryService;
  private final ServiceInstanceCommandService commandService;
  private final ServiceInstanceCleanupProperties properties;
  private final MeterRegistry meterRegistry;
  private final ServiceInstanceCacheEvictionService cacheEvictionService;

  private Counter unhealthyInstancesMarkedCounter;
  private Counter unhealthyInstancesDeletedCounter;

  /**
   * Initialize metrics counters.
   */
  private void initMetrics() {
    if (unhealthyInstancesMarkedCounter == null) {
      unhealthyInstancesMarkedCounter = Counter.builder(MetricsNames.Cleanup.UNHEALTHY_INSTANCES_MARKED)
          .description("Number of instances marked as unhealthy")
          .register(meterRegistry);
    }
    if (unhealthyInstancesDeletedCounter == null) {
      unhealthyInstancesDeletedCounter = Counter.builder(MetricsNames.Cleanup.UNHEALTHY_INSTANCES_DELETED)
          .description("Number of unhealthy instances deleted")
          .register(meterRegistry);
    }
  }

  /**
   * Mark instances as UNHEALTHY if they haven't sent a heartbeat within the
   * threshold.
   * <p>
   * Runs periodically based on configured schedule (default: every 5 minutes).
   * Instances that haven't sent a heartbeat within unhealthyThresholdMinutes
   * (default: 5 minutes) will be marked as UNHEALTHY.
   * <p>
   * Instances will automatically recover to HEALTHY status when they send a new
   * heartbeat (handled by HeartbeatService).
   */
  @Scheduled(cron = "${service-instance.cleanup.schedule-cron:0 */5 * * * *}")
  @Transactional
  public void markUnhealthyInstances() {
    if (!properties.isEnabled()) {
      log.debug("Cleanup is disabled, skipping unhealthy instance marking");
      return;
    }

    initMetrics();

    // Use unhealthyThresholdMinutes, fallback to staleThresholdMinutes for backward compatibility
    @SuppressWarnings("deprecation")
    int thresholdMinutes = properties.getUnhealthyThresholdMinutes() > 0
        ? properties.getUnhealthyThresholdMinutes()
        : properties.getStaleThresholdMinutes();
    Instant threshold = Instant.now().minusSeconds(thresholdMinutes * 60L);
    log.debug("Marking instances as UNHEALTHY if lastSeenAt < {} (threshold: {} minutes)", threshold,
        thresholdMinutes);

    ServiceInstanceCriteria criteria = ServiceInstanceCriteria.unhealthyInstances(threshold);
    List<ServiceInstance> unhealthyInstances = queryService
        .findAll(criteria, org.springframework.data.domain.Pageable.unpaged())
        .getContent()
        .stream()
        .filter(instance -> instance.getStatus() != ServiceInstance.InstanceStatus.UNHEALTHY)
        .toList();

    if (unhealthyInstances.isEmpty()) {
      log.debug("No instances to mark as UNHEALTHY");
      return;
    }

    int marked = 0;
    for (ServiceInstance instance : unhealthyInstances) {
      try {
        instance.setStatus(ServiceInstance.InstanceStatus.UNHEALTHY);
        instance.setUpdatedAt(Instant.now());
        commandService.save(instance);
        marked++;
        unhealthyInstancesMarkedCounter.increment();
        log.debug("Marked instance {} as UNHEALTHY (lastSeenAt: {})", instance.getId(), instance.getLastSeenAt());
      } catch (Exception e) {
        log.error("Failed to mark instance {} as UNHEALTHY", instance.getId(), e);
      }
    }

    // Evict cache entries for status-filtered findAll caches and count caches
    // Status changes from HEALTHY/DRIFT to UNHEALTHY affect findAll queries with status filters
    if (marked > 0) {
      cacheEvictionService.evictAll("Status change: marked " + marked + " instances as UNHEALTHY");
    }

    log.info("Marked {} instances as UNHEALTHY", marked);
  }

  /**
   * Cleanup old unhealthy instances that have been UNHEALTHY for longer than the
   * cleanup threshold.
   * <p>
   * Runs periodically based on configured schedule (default: every 5 minutes).
   * Instances that have been UNHEALTHY for longer than unhealthyCleanupThresholdDays
   * (default: 30 days) will be permanently deleted.
   */
  @Scheduled(cron = "${service-instance.cleanup.schedule-cron:0 */5 * * * *}")
  @Transactional
  public void cleanupOldUnhealthyInstances() {
    if (!properties.isEnabled()) {
      log.debug("Cleanup is disabled, skipping unhealthy instance cleanup");
      return;
    }

    initMetrics();

    // Use unhealthyCleanupThresholdDays, fallback to cleanupThresholdDays for backward compatibility
    @SuppressWarnings("deprecation")
    int thresholdDays = properties.getUnhealthyCleanupThresholdDays() > 0
        ? properties.getUnhealthyCleanupThresholdDays()
        : properties.getCleanupThresholdDays();
    Instant threshold = Instant.now().minusSeconds(thresholdDays * 24L * 60L * 60L);
    log.debug("Cleaning up instances with status UNHEALTHY and lastSeenAt < {} (threshold: {} days)", threshold,
        thresholdDays);

    ServiceInstanceCriteria criteria = ServiceInstanceCriteria.unhealthyInstances(threshold)
        .toBuilder()
        .status(ServiceInstance.InstanceStatus.UNHEALTHY)
        .build();
    List<ServiceInstance> unhealthyInstances = queryService
        .findAll(criteria, org.springframework.data.domain.Pageable.unpaged())
        .getContent();

    if (unhealthyInstances.isEmpty()) {
      log.debug("No unhealthy instances to cleanup");
      return;
    }

    int deleted = 0;
    for (ServiceInstance instance : unhealthyInstances) {
      try {
        commandService.deleteById(instance.getId());
        deleted++;
        unhealthyInstancesDeletedCounter.increment();
        log.debug("Deleted unhealthy instance {} (lastSeenAt: {})", instance.getId(), instance.getLastSeenAt());
      } catch (Exception e) {
        log.error("Failed to delete unhealthy instance {}", instance.getId(), e);
      }
    }

    // Evict cache entries for findAll and count caches
    // Deletions affect findAll queries and count caches
    if (deleted > 0) {
      cacheEvictionService.evictAll("Cleanup: deleted " + deleted + " unhealthy instances");
    }

    log.info("Deleted {} unhealthy instances", deleted);
  }

  /**
   * Mark instances as STALE if they haven't sent a heartbeat within the
   * threshold.
   * <p>
   * Runs periodically based on configured schedule.
   * 
   * @deprecated Use {@link #markUnhealthyInstances()} instead. Kept for backward
   *             compatibility.
   */
  @Deprecated
  @Scheduled(cron = "${service-instance.cleanup.schedule-cron:0 */5 * * * *}")
  @Transactional
  public void markStaleInstances() {
    log.warn("markStaleInstances() is deprecated, use markUnhealthyInstances() instead");
    markUnhealthyInstances();
  }
}
