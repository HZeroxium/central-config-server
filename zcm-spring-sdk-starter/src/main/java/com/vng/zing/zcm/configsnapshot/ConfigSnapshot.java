package com.vng.zing.zcm.configsnapshot;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents a canonical snapshot of the application's configuration state.
 * <p>
 * This snapshot is typically used for **configuration drift detection** and hashing.
 * It contains normalized, deterministic properties collected from
 * the Spring Cloud Config Server and excludes volatile or local-only keys.
 *
 * <p>Each {@code ConfigSnapshot} is immutable and thread-safe.
 */
public final class ConfigSnapshot {

  private final String application;
  private final String profile;
  private final String label;
  private final String version;
  private final SortedMap<String, String> properties;

  /**
   * Creates a new {@code ConfigSnapshot} instance.
   *
   * @param application the application name
   * @param profile     the active Spring profile
   * @param label       the configuration label (e.g., Git branch)
   * @param version     the configuration version (e.g., commit hash)
   * @param properties  a sorted map of normalized configuration key-value pairs
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

  /** @return the application name */
  public String getApplication() {
    return application;
  }

  /** @return the active profile */
  public String getProfile() {
    return profile;
  }

  /** @return the config label (branch/tag identifier) */
  public String getLabel() {
    return label;
  }

  /** @return the config version (commit or revision ID) */
  public String getVersion() {
    return version;
  }

  /**
   * Returns an immutable sorted view of all configuration properties.
   *
   * @return a read-only map of configuration properties
   */
  public SortedMap<String, String> getProperties() {
    return Collections.unmodifiableSortedMap(properties);
  }

  /**
   * Builds a deterministic canonical string representation of this configuration.
   * <p>
   * This is mainly used for hashing or drift comparison. The output is line-oriented
   * with each property expressed as {@code key=value}.
   *
   * @return a canonical text representation of this snapshot
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
}
