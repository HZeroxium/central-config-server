package com.example.control.application.external.fallback;

import com.example.control.infrastructure.resilience.fallback.CachedFallbackProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fallback provider for ConfigServer operations.
 * <p>
 * Returns last known good configuration from cache when ConfigServer is
 * unavailable.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigServerFallback {

  private final CachedFallbackProvider cachedFallbackProvider;

  private static final String CACHE_NAME = "config-hashes";

  /**
   * Get fallback configuration from cache.
   *
   * @param application Application name
   * @param profiles    Profiles (comma-separated)
   * @return Cached configuration if available, empty string otherwise
   */
  public String getFallbackConfig(String application, String profiles) {
    String key = application + ":" + profiles;

    Optional<String> cached = cachedFallbackProvider.getFromCache(CACHE_NAME, key, String.class);

    if (cached.isPresent()) {
      log.info("Returning cached config for {}:{} (ConfigServer fallback)", application, profiles);
      return cached.get();
    } else {
      log.warn("No cached config available for {}:{}, using empty fallback", application, profiles);
      return "{}"; // Return empty JSON object as fallback
    }
  }

  /**
   * Get fallback configuration with label from cache.
   *
   * @param application Application name
   * @param profiles    Profiles
   * @param label       Git label/branch
   * @return Cached configuration if available
   */
  public String getFallbackConfigWithLabel(String application, String profiles, String label) {
    String key = application + ":" + profiles + ":" + label;

    Optional<String> cached = cachedFallbackProvider.getFromCache(CACHE_NAME, key, String.class);

    if (cached.isPresent()) {
      log.info("Returning cached config for {}:{}:{} (ConfigServer fallback)", application, profiles, label);
      return cached.get();
    } else {
      log.warn("No cached config available for {}:{}:{}, using empty fallback", application, profiles, label);
      return "{}";
    }
  }

  /**
   * Save configuration to cache for future fallback.
   *
   * @param application Application name
   * @param profiles    Profiles
   * @param config      Configuration content
   */
  public void saveConfigToCache(String application, String profiles, String config) {
    String key = application + ":" + profiles;
    cachedFallbackProvider.saveToCache(CACHE_NAME, key, config);
    log.debug("Saved config to fallback cache for {}:{}", application, profiles);
  }
}
