package com.vng.zing.zcm.configsnapshot;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Build a normalized snapshot from Spring Environment. Mirrors server-side rules.
 */
public final class ConfigSnapshotBuilder {

  private final ConfigurableEnvironment environment;

  public ConfigSnapshotBuilder(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  public ConfigSnapshot build(String application, String profile, String label, String version) {
    SortedMap<String, String> props = new TreeMap<>();

    for (PropertySource<?> ps : environment.getPropertySources()) {
      if (!(ps instanceof EnumerablePropertySource<?> eps)) continue;
      String name = ps.getName();
      if (!isFromConfigServer(name)) continue;

      for (String key : eps.getPropertyNames()) {
        if (isVolatileOrSensitive(key)) continue;
        Object v = eps.getProperty(key);
        // Respect precedence: Spring Environment orders property sources by precedence
        // Keep first-seen value from higher-precedence sources
        if (v != null) props.putIfAbsent(key, String.valueOf(v));
      }
    }

    return new ConfigSnapshot(application, profile, label, version, props);
  }

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


