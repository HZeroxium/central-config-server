package com.example.control.configsnapshot;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Build snapshot from Config Server environment JSON.
 */
public final class ConfigSnapshotBuilderFromConfigServer {

  public ConfigSnapshot build(String application, String profile, String label, JsonNode envJson) {
    String version = envJson != null && envJson.hasNonNull("version") ? envJson.get("version").asText() : null;

    SortedMap<String, String> props = new TreeMap<>();

    if (envJson != null && envJson.has("propertySources") && envJson.get("propertySources").isArray()) {
      for (JsonNode ps : envJson.get("propertySources")) {
        String name = ps.hasNonNull("name") ? ps.get("name").asText() : null;
        if (!includeSource(name)) continue;
        JsonNode source = ps.get("source");
        if (source == null || !source.isObject()) continue;
        Iterator<String> it = source.fieldNames();
        while (it.hasNext()) {
          String key = it.next();
          if (excludeKey(key)) continue;
          JsonNode v = source.get(key);
          // Respect precedence: Config Server returns highest-precedence sources first
          // Keep the first-seen value; do not overwrite with lower-precedence ones
          if (v != null && !v.isNull()) props.putIfAbsent(key, v.asText());
        }
      }
    }

    return new ConfigSnapshot(application, profile != null ? profile : "default", label, version, props);
  }

  private boolean includeSource(String name) {
    if (name == null) return false;
    String n = name.toLowerCase();
    if (n.startsWith("configserver:")) return true;
    if (n.startsWith("http://") || n.startsWith("https://")) return true;
    if (n.startsWith("classpath:")) return false;
    if (n.startsWith("applicationconfig:")) return false;
    if (n.contains("systemenvironment")) return false;
    if (n.contains("systemproperties")) return false;
    if (n.contains("randomvaluepropertysource")) return false;
    return false;
  }

  private boolean excludeKey(String key) {
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


