package com.example.control.infrastructure.external.fallback;

import com.example.control.infrastructure.resilience.fallback.CachedFallbackProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fallback provider for Consul operations.
 * <p>
 * Returns cached service registry and health data when Consul is unavailable.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsulFallback {

  private final CachedFallbackProvider cachedFallbackProvider;

  private static final String SERVICES_CACHE = "consul-services";
  private static final String HEALTH_CACHE = "consul-health";

  /**
   * Get fallback service list from cache.
   *
   * @return Cached service list if available, empty JSON array otherwise
   */
  public String getFallbackServices() {
    Optional<String> cached = cachedFallbackProvider.getFromCache(SERVICES_CACHE, "catalog", String.class);

    if (cached.isPresent()) {
      log.info("Returning cached services list (Consul fallback)");
      return cached.get();
    } else {
      log.warn("No cached services list available, using empty fallback");
      return "{}";
    }
  }

  /**
   * Get fallback service details from cache.
   *
   * @param serviceName Service name
   * @return Cached service details if available
   */
  public String getFallbackService(String serviceName) {
    Optional<String> cached = cachedFallbackProvider.getFromCache(SERVICES_CACHE, serviceName, String.class);

    if (cached.isPresent()) {
      log.info("Returning cached service details for {} (Consul fallback)", serviceName);
      return cached.get();
    } else {
      log.warn("No cached service details for {}, using empty fallback", serviceName);
      return "[]";
    }
  }

  /**
   * Get fallback health data from cache.
   *
   * @param serviceName Service name
   * @return Cached health data if available
   */
  public String getFallbackHealth(String serviceName) {
    Optional<String> cached = cachedFallbackProvider.getFromCache(HEALTH_CACHE, serviceName, String.class);

    if (cached.isPresent()) {
      log.info("Returning cached health for {} (Consul fallback)", serviceName);
      return cached.get();
    } else {
      log.warn("No cached health for {}, using empty fallback", serviceName);
      return "[]";
    }
  }

  /**
   * Save service data to cache for future fallback.
   *
   * @param serviceName Service name
   * @param data        Service data
   */
  public void saveServiceToCache(String serviceName, String data) {
    cachedFallbackProvider.saveToCache(SERVICES_CACHE, serviceName, data);
    log.debug("Saved service {} to fallback cache", serviceName);
  }

  /**
   * Save health data to cache for future fallback.
   *
   * @param serviceName Service name
   * @param healthData  Health data
   */
  public void saveHealthToCache(String serviceName, String healthData) {
    cachedFallbackProvider.saveToCache(HEALTH_CACHE, serviceName, healthData);
    log.debug("Saved health for {} to fallback cache", serviceName);
  }
}
