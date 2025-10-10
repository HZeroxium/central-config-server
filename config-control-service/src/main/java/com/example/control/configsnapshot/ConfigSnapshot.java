package com.example.control.configsnapshot;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents a canonical, deterministic configuration snapshot for server-side hashing.
 * <p>
 * This class is used to compute a consistent configuration signature (hash)
 * that mirrors the SDK-side representation, ensuring identical hash computation
 * between Config Server and Client SDK.
 * <p>
 * The canonical form includes a deterministic order of all key-value pairs
 * (sorted alphabetically) and selected environment metadata.
 * 
 * <h2>Example usage:</h2>
 * <pre>{@code
 * ConfigSnapshot snapshot = new ConfigSnapshot("my-app", "dev", "main", "v12", properties);
 * String canonical = snapshot.toCanonicalString();
 * String hash = Sha256Hasher.hash(canonical);
 * }</pre>
 */
public final class ConfigSnapshot {

  /** Name of the application (Spring application name). */
  private final String application;

  /** Environment profile (e.g., dev, staging, prod). */
  private final String profile;

  /** Label of the configuration version (typically Git branch or tag). */
  private final String label;

  /** Version identifier (e.g., Git commit hash). */
  private final String version;

  /** Canonically sorted property key-value pairs for hash computation. */
  private final SortedMap<String, String> properties;

  /**
   * Constructs a new canonical configuration snapshot.
   *
   * @param application the application name
   * @param profile the active profile
   * @param label the configuration label (branch or tag)
   * @param version the version identifier
   * @param properties a sorted map of configuration key-value pairs
   */
  public ConfigSnapshot(String application,
                        String profile,
                        String label,
                        String version,
                        SortedMap<String, String> properties) {
    this.application = application;
    this.profile = profile;
    this.label = label;
    this.version = version;
    this.properties = properties != null ? new TreeMap<>(properties) : new TreeMap<>();
  }

  /**
   * Converts this configuration snapshot into a canonical string representation.
   * <p>
   * The result is a newline-separated list of metadata and key-value pairs.
   * This ensures a deterministic, platform-independent format suitable for hashing.
   *
   * @return canonical configuration string
   */
  public String toCanonicalString() {
    StringBuilder sb = new StringBuilder(256 + properties.size() * 32);
    if (application != null) sb.append("application=").append(application).append('\n');
    if (profile != null) sb.append("profile=").append(profile).append('\n');
    if (label != null) sb.append("label=").append(label).append('\n');
    if (version != null) sb.append("version=").append(version).append('\n');
    for (var e : properties.entrySet()) {
      sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
    }
    return sb.toString();
  }

  /**
   * Returns an unmodifiable view of the sorted property map.
   *
   * @return unmodifiable map of configuration properties
   */
  public SortedMap<String, String> getProperties() {
    return Collections.unmodifiableSortedMap(properties);
  }
}
