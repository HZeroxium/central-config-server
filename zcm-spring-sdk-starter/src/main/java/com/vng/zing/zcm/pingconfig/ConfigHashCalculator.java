package com.vng.zing.zcm.pingconfig;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.configsnapshot.ConfigSnapshotBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
 * <p>
 * Supports mock mode for load testing: when enabled, generates deterministic
 * mock hashes matching server-side strategy to avoid drift events.
 */
@Slf4j
public class ConfigHashCalculator {

  /**
   * Prefix for mock hash generation to distinguish from real hashes.
   * Matches server-side mock prefix for consistency.
   */
  private static final String MOCK_PREFIX = "mock-";

  /** The Spring environment that holds the active configuration. */
  @Getter
  private final ConfigurableEnvironment environment;

  private final SdkProperties sdkProperties;

  /**
   * Constructs a {@code ConfigHashCalculator} using the provided Spring environment.
   *
   * @param environment the environment that contains the active configuration properties
   */
  public ConfigHashCalculator(ConfigurableEnvironment environment, SdkProperties sdkProperties) {
    this.environment = environment;
    this.sdkProperties = sdkProperties;
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
   * <p>
   * If mock mode is enabled (via {@code zcm.sdk.ping.hash-mock.enabled}), generates
   * a deterministic mock hash matching server-side strategy to avoid drift events
   * during load testing.
   *
   * @return a lowercase hexadecimal representation of the configuration hash,
   *         or {@code "NA"} if an error occurs
   */
  // @Cacheable(value = "config-hash-cache", keyGenerator = "configHashCacheKeyGenerator", unless = "#result == null || #result == 'NA'")
  public String currentHash() {
    try {
      String application = environment.getProperty("spring.application.name", "unknown");
      String[] profiles = environment.getActiveProfiles();
      String profile = profiles.length > 0 ? profiles[0] : "default";

      // Check if mock mode is enabled
      boolean mockEnabled = environment.getProperty("zcm.sdk.ping.hash-mock.enabled", Boolean.class, false);
      if (mockEnabled) {
        log.info("Mock hash mode is enabled, returning mock config hash");
        String mockHash = getMockConfigHash(application, profile);
        log.info("Mock config hash: {}", mockHash);
        return mockHash;
      }
      else {
        log.info("Mock hash mode is disabled, returning real config hash");
      }

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
   * Generates a mock config hash based on configured strategy.
   * <p>
   * Matches server-side mock hash generation for consistency.
   * Uses the same prefix and formula as ConfigProxyService.
   *
   * @param serviceName service name
   * @param profile     environment profile
   * @return mock config hash
   */
  private String getMockConfigHash(String serviceName, String profile) {
    String strategy = sdkProperties.getPing().getHashMock().getStrategy().name();
    String mockHash;

    switch (strategy.toUpperCase()) {
      case "DETERMINISTIC":
        // Generate stable hash from serviceName + profile (matches server-side)
        String input = serviceName + ":" + (profile != null ? profile : "default");
        mockHash = hash(MOCK_PREFIX + input);
        break;

      case "RANDOM":
        // Generate random hash each time (includes timestamp)
        String randomInput = serviceName + ":" +
            (profile != null ? profile : "default") + ":" + System.currentTimeMillis();
        mockHash = hash(MOCK_PREFIX + randomInput);
        break;

      case "STATIC":
        // Return fixed hash value
        mockHash = sdkProperties.getPing().getHashMock().getStaticHash();
        break;

      default:
        // Fallback to deterministic
        String fallbackInput = serviceName + ":" + (profile != null ? profile : "default");
        mockHash = hash(MOCK_PREFIX + fallbackInput);
    }

    if (log.isDebugEnabled()) {
      log.debug("Returning mock config hash for {}:{} -> {} (strategy: {})",
          serviceName, profile, mockHash, strategy);
    }

    return mockHash;
  }

  /**
   * Computes SHA-256 hash for a given input string.
   * <p>
   * Utility method matching server-side ConfigHashCalculator.hash() implementation.
   *
   * @param input input string to hash
   * @return SHA-256 hash as lowercase hex string
   */
  public static String hash(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      log.warn("Failed to compute hash for mock config", e);
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
