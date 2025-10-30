package com.example.control.infrastructure.config.resilience;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for rate limiting public endpoints.
 * <p>
 * Creates Resilience4j rate limiters with in-memory tracking (can be extended
 * to Redis for distributed rate limiting).
 * </p>
 */
@Slf4j
@Configuration
public class RateLimiterConfiguration {

  /**
   * Rate limiter for heartbeat endpoint.
   * <p>
   * Configured in application.yml under
   * resilience4j.ratelimiter.instances.heartbeat-endpoint
   * </p>
   */
  @Bean
  public RateLimiter heartbeatRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
    RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("heartbeat-endpoint");
    log.info("Heartbeat rate limiter created with config: {}", rateLimiter.getRateLimiterConfig());
    return rateLimiter;
  }

  /**
   * Rate limiter for admin endpoints.
   * <p>
   * Configured in application.yml under
   * resilience4j.ratelimiter.instances.admin-endpoints
   * </p>
   */
  @Bean
  public RateLimiter adminRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
    RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("admin-endpoints");
    log.info("Admin rate limiter created with config: {}", rateLimiter.getRateLimiterConfig());
    return rateLimiter;
  }
}
