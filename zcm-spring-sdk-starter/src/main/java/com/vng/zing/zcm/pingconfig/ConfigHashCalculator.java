package com.vng.zing.zcm.pingconfig;

import com.vng.zing.zcm.configsnapshot.ConfigSnapshotBuilder;
import lombok.Getter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Calculates a deterministic SHA-256 hash of the effective configuration
 * currently loaded in the {@link ConfigurableEnvironment}.
 * <p>
 * The resulting hash can be used for drift detection — allowing systems
 * to detect when a configuration has changed across deployments or nodes.
 * <p>
 * Internally, this class uses {@link ConfigSnapshotBuilder} to normalize
 * the configuration before hashing.
 * <p>
 * The hash is cached to avoid expensive recalculation on every ping.
 * Cache is invalidated when refresh events are received.
 */
public class ConfigHashCalculator {

  /** The Spring environment that holds the active configuration. */
  @Getter
  private final ConfigurableEnvironment environment;

  /**
   * Constructs a {@code ConfigHashCalculator} using the provided Spring environment.
   *
   * @param environment the environment that contains the active configuration properties
   */
  public ConfigHashCalculator(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  /**
   * Computes a SHA-256 hash of the canonicalized configuration snapshot.
   * <p>
   * The algorithm collects key properties (application name, profile, label, version),
   * builds a normalized snapshot via {@link ConfigSnapshotBuilder}, converts it to
   * a canonical text form, and hashes the UTF-8 bytes.
   * <p>
   * The result is cached using Spring Cache with cache name "config-hash-cache".
   * Cache key is based on application name, profile, and label to ensure uniqueness.
   * Cache TTL is configured via {@code zcm.sdk.ping.hash-cache.ttl} (default: 30s).
   *
   * @return a lowercase hexadecimal representation of the configuration hash,
   *         or {@code "NA"} if an error occurs
   */
  @Cacheable(value = "config-hash-cache", keyGenerator = "configHashCacheKeyGenerator", unless = "#result == null || #result == 'NA'")
  public String currentHash() {
    try {
      String application = environment.getProperty("spring.application.name", "unknown");
      String[] profiles = environment.getActiveProfiles();
      String profile = profiles.length > 0 ? profiles[0] : "default";
      String label = environment.getProperty("spring.cloud.config.label");
      String version = environment.getProperty("config.client.version");

      // Build a canonical snapshot of the current configuration
      var snapshot = new ConfigSnapshotBuilder(environment)
          .build(application, profile, label, version);
      String canonical = snapshot.toCanonicalString();

      // Compute SHA-256 digest
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));

      // Convert digest bytes to lowercase hex string
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      // Fail-safe fallback, ensuring no exception propagation
      return "NA";
    }
  }

  /**
   * Generates a cache key for the current configuration context.
   * <p>
   * The cache key is based on application name, profile, and label to ensure
   * that different configuration contexts have different cache entries.
   *
   * @return cache key string
   */
  public String getCacheKey() {
    String application = environment.getProperty("spring.application.name", "unknown");
    String[] profiles = environment.getActiveProfiles();
    String profile = profiles.length > 0 ? profiles[0] : "default";
    String label = environment.getProperty("spring.cloud.config.label", "master");
    return application + ":" + profile + ":" + label;
  }
}
