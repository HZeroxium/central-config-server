package com.vng.zing.zcm.configsnapshot;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Builder class responsible for creating a {@link ConfigSnapshot}
 * from a Spring {@link ConfigurableEnvironment}.
 * <p>
 * It mirrors the same rules used by the Config Server for selecting
 * which properties are included or filtered out.
 * <p>
 * This class filters out:
 * <ul>
 *   <li>Non-ConfigServer sources</li>
 *   <li>Volatile or system-related keys</li>
 *   <li>Sensitive data (passwords, tokens, secrets)</li>
 * </ul>
 */
public final class ConfigSnapshotBuilder {

  private final ConfigurableEnvironment environment;

  /**
   * Constructs a new {@code ConfigSnapshotBuilder} using the provided environment.
   *
   * @param environment the active Spring environment
   */
  public ConfigSnapshotBuilder(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  /**
   * Builds a normalized configuration snapshot.
   * <p>
   * It scans all property sources in order of precedence and collects
   * the first-seen value for each property key from Config Server sources.
   *
   * @param application the application name
   * @param profile     the active Spring profile
   * @param label       the config label (branch/tag)
   * @param version     the config version (commit hash)
   * @return a constructed {@link ConfigSnapshot} instance
   */
  public ConfigSnapshot build(String application, String profile, String label, String version) {
    SortedMap<String, String> props = new TreeMap<>();

    for (PropertySource<?> ps : environment.getPropertySources()) {
      if (!(ps instanceof EnumerablePropertySource<?> eps)) continue;
      String name = ps.getName();
      if (!isFromConfigServer(name)) continue;

      for (String key : eps.getPropertyNames()) {
        if (isVolatileOrSensitive(key)) continue;
        Object v = eps.getProperty(key);

        // Respect Spring Environment precedence:
        // First-seen values from higher-precedence sources are retained.
        if (v != null) props.putIfAbsent(key, String.valueOf(v));
      }
    }

    return new ConfigSnapshot(application, profile, label, version, props);
  }

  /**
   * Determines whether a property source originated from a Config Server.
   *
   * @param sourceName the property source name
   * @return true if the source is Config Server-backed
   */
  private boolean isFromConfigServer(String sourceName) {
    if (sourceName == null) return false;
    String n = sourceName.toLowerCase();
    if (n.startsWith("configserver:")) return true;
    if (n.startsWith("http://") || n.startsWith("https://")) return true;
    if (n.startsWith("applicationconfig:")) return false;
    if (n.contains("systemenvironment")) return false;
    if (n.contains("systemproperties")) return false;
    if (n.contains("randomvaluepropertysource")) return false;
    if (n.startsWith("classpath:")) return false;
    return false;
  }

  /**
   * Filters out volatile, sensitive, or environment-specific properties.
   *
   * @param key the property key
   * @return true if the property should be excluded
   */
  private boolean isVolatileOrSensitive(String key) {
    if (key == null) return true;
    String k = key.toLowerCase();
    return k.contains("password") || k.contains("secret") || k.contains("token") || k.contains("credential")
        || k.startsWith("random.")
        || k.startsWith("local.server.port")
        || k.startsWith("local.management.port")
        || k.startsWith("management.metrics")
        || k.startsWith("logging.")
        || k.startsWith("spring.application.instance_id")
        || k.startsWith("info.")
        || k.startsWith("server.address")
        || k.startsWith("java.")
        || k.startsWith("sun.")
        || k.startsWith("user.");
  }
}
