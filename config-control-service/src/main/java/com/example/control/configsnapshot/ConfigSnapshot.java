package com.example.control.configsnapshot;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Canonical snapshot for server-side hashing. Must mirror SDK representation.
 */
public final class ConfigSnapshot {

  private final String application;
  private final String profile;
  private final String label;
  private final String version;
  private final SortedMap<String, String> properties;

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

  public SortedMap<String, String> getProperties() {
    return Collections.unmodifiableSortedMap(properties);
  }
}


