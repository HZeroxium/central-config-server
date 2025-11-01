package com.example.control.infrastructure.cache;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.IamTeamQueryService;
import com.example.control.application.query.IamUserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Critical cache warmer that warms up essential caches at application startup.
 * <p>
 * Loads frequently accessed data into cache to improve initial response times:
 * <ul>
 * <li>IAM Users: Top N most accessed users</li>
 * <li>IAM Teams: All teams</li>
 * <li>Application Services: Top N services</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CriticalCacheWarmer {

  private final IamUserQueryService iamUserQueryService;
  private final IamTeamQueryService iamTeamQueryService;
  private final ApplicationServiceQueryService applicationServiceQueryService;

  /**
   * Warm critical caches after application is ready.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void warmCaches() {
    log.info("Starting critical cache warmup...");

    try {
      warmIamTeams();
      warmIamUsers();
      warmApplicationServices();

      log.info("Critical cache warmup completed successfully");
    } catch (Exception e) {
      log.warn("Error during cache warmup, continuing anyway", e);
    }
  }

  /**
   * Warm IAM teams cache.
   */
  private void warmIamTeams() {
    try {
      log.debug("Warming IAM teams cache...");
      iamTeamQueryService.countAll();
      iamTeamQueryService.findAll(null, PageRequest.of(0, 100));
      log.debug("Warmed IAM teams cache");
    } catch (Exception e) {
      log.warn("Failed to warm IAM teams cache", e);
    }
  }

  /**
   * Warm IAM users cache.
   */
  private void warmIamUsers() {
    try {
      log.debug("Warming IAM users cache...");
      iamUserQueryService.countAll();
      iamUserQueryService.findAll(null, PageRequest.of(0, 100));
      log.debug("Warmed IAM users cache");
    } catch (Exception e) {
      log.warn("Failed to warm IAM users cache", e);
    }
  }

  /**
   * Warm application services cache.
   */
  private void warmApplicationServices() {
    try {
      log.debug("Warming application services cache...");
      // Use findAll() which loads all services into cache
      applicationServiceQueryService.findAll();
      log.debug("Warmed application services cache");
    } catch (Exception e) {
      log.warn("Failed to warm application services cache", e);
    }
  }
}
