package com.example.control.application.service.infra;

import com.example.control.application.command.ServiceInstanceCommandService;
import com.example.control.application.query.ServiceInstanceQueryService;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.model.ServiceInstance;
import com.example.control.infrastructure.config.misc.ServiceInstanceCleanupProperties;
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
 * Scheduled service for cleaning up stale service instances.
 * <p>
 * This service periodically:
 * <ul>
 * <li>Marks instances as STALE if they haven't sent a heartbeat within the
 * threshold</li>
 * <li>Deletes instances that have been STALE for longer than the cleanup
 * threshold</li>
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

  private Counter staleInstancesMarkedCounter;
  private Counter staleInstancesDeletedCounter;

  /**
   * Initialize metrics counters.
   */
  private void initMetrics() {
    if (staleInstancesMarkedCounter == null) {
      staleInstancesMarkedCounter = Counter.builder("config_control.cleanup.stale_instances_marked")
          .description("Number of instances marked as stale")
          .register(meterRegistry);
    }
    if (staleInstancesDeletedCounter == null) {
      staleInstancesDeletedCounter = Counter.builder("config_control.cleanup.stale_instances_deleted")
          .description("Number of stale instances deleted")
          .register(meterRegistry);
    }
  }

  /**
   * Mark instances as STALE if they haven't sent a heartbeat within the
   * threshold.
   * <p>
   * Runs periodically based on configured schedule.
   */
  @Scheduled(cron = "${service-instance.cleanup.schedule-cron:0 */5 * * * *}")
  @Transactional
  public void markStaleInstances() {
    if (!properties.isEnabled()) {
      log.debug("Cleanup is disabled, skipping stale instance marking");
      return;
    }

    initMetrics();

    Instant threshold = Instant.now().minusSeconds(properties.getStaleThresholdMinutes() * 60L);
    log.debug("Marking instances as STALE if lastSeenAt < {}", threshold);

    ServiceInstanceCriteria criteria = ServiceInstanceCriteria.staleInstances(threshold);
    List<ServiceInstance> staleInstances = queryService
        .findAll(criteria, org.springframework.data.domain.Pageable.unpaged())
        .getContent()
        .stream()
        .filter(instance -> instance.getStatus() != ServiceInstance.InstanceStatus.STALE)
        .toList();

    if (staleInstances.isEmpty()) {
      log.debug("No instances to mark as STALE");
      return;
    }

    int marked = 0;
    for (ServiceInstance instance : staleInstances) {
      try {
        instance.setStatus(ServiceInstance.InstanceStatus.STALE);
        instance.setUpdatedAt(Instant.now());
        commandService.save(instance);
        marked++;
        staleInstancesMarkedCounter.increment();
        log.debug("Marked instance {} as STALE (lastSeenAt: {})", instance.getId(), instance.getLastSeenAt());
      } catch (Exception e) {
        log.error("Failed to mark instance {} as STALE", instance.getId(), e);
      }
    }

    log.info("Marked {} instances as STALE", marked);
  }

  /**
   * Cleanup old stale instances that have been STALE for longer than the cleanup
   * threshold.
   * <p>
   * Runs periodically based on configured schedule.
   */
  @Scheduled(cron = "${service-instance.cleanup.schedule-cron:0 */5 * * * *}")
  @Transactional
  public void cleanupOldStaleInstances() {
    if (!properties.isEnabled()) {
      log.debug("Cleanup is disabled, skipping stale instance cleanup");
      return;
    }

    initMetrics();

    Instant threshold = Instant.now().minusSeconds(properties.getCleanupThresholdDays() * 24L * 60L * 60L);
    log.debug("Cleaning up instances with status STALE and lastSeenAt < {}", threshold);

    ServiceInstanceCriteria criteria = ServiceInstanceCriteria.staleInstances(threshold);
    List<ServiceInstance> staleInstances = queryService
        .findAll(criteria, org.springframework.data.domain.Pageable.unpaged())
        .getContent()
        .stream()
        .filter(instance -> instance.getStatus() == ServiceInstance.InstanceStatus.STALE)
        .toList();

    if (staleInstances.isEmpty()) {
      log.debug("No stale instances to cleanup");
      return;
    }

    int deleted = 0;
    for (ServiceInstance instance : staleInstances) {
      try {
        commandService.deleteById(instance.getId());
        deleted++;
        staleInstancesDeletedCounter.increment();
        log.debug("Deleted stale instance {} (lastSeenAt: {})", instance.getId(), instance.getLastSeenAt());
      } catch (Exception e) {
        log.error("Failed to delete stale instance {}", instance.getId(), e);
      }
    }

    log.info("Deleted {} stale instances", deleted);
  }
}
