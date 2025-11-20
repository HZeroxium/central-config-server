package com.vng.zing.zcm.client.config;

import com.vng.zing.zcm.configsnapshot.ConfigSnapshot;
import com.vng.zing.zcm.configsnapshot.ConfigSnapshotBuilder;
import com.vng.zing.zcm.pingconfig.ConfigHashCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ConfigApiImpl implements ConfigApi {
  
  private final ConfigHashCalculator hashCalc;
  
  @Override
  public String get(String key) {
    return env().getProperty(key);
  }
  
  @Override
  public Map<String, Object> getAll(String prefix) {
    Map<String, Object> out = new LinkedHashMap<>();
    if (env() instanceof ConfigurableEnvironment configurableEnv) {
      configurableEnv.getPropertySources().forEach(ps -> {
        if (ps.containsProperty(prefix)) {
          out.put(ps.getName(), ps.getProperty(prefix));
        }
      });
    }
    return out;
  }
  
  @Override
  public String hash() {
    return hashCalc.currentHash();
  }
  
  @Override
  public Map<String, Object> snapshot() {
    String application = env().getProperty("spring.application.name", "unknown");
    String[] profiles = env().getActiveProfiles();
    String profile = profiles.length > 0 ? profiles[0] : "default";
    String label = env().getProperty("spring.cloud.config.label");
    String version = env().getProperty("config.client.version");
    
    var snapshot = new ConfigSnapshotBuilder((ConfigurableEnvironment) env())
        .build(application, profile, label, version);
    
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("application", application);
    map.put("profile", profile);
    map.put("label", label);
    map.put("version", version);
    map.put("properties", snapshot.getProperties());
    
    return map;
  }
  
  @Override
  public Map<String, Object> hashDetails() {
    String application = env().getProperty("spring.application.name", "unknown");
    String[] profiles = env().getActiveProfiles();
    String profile = profiles.length > 0 ? profiles[0] : "default";
    String label = env().getProperty("spring.cloud.config.label");
    String version = env().getProperty("config.client.version");
    
    ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) env();
    
    // Build snapshot ONCE
    ConfigSnapshot snapshot = new ConfigSnapshotBuilder(configurableEnv)
        .build(application, profile, label, version);
    
    // Build canonical string from the SAME snapshot
    String canonicalString = snapshot.toCanonicalString();
    
    // Calculate hash from the SAME canonical string (not from cache)
    String hash = ConfigHashCalculator.hash(canonicalString);
    
    // Collect metadata
    List<String> sourceNames = new ArrayList<>();
    int excludedKeyCount = 0;
    int totalKeyCount = 0;
    
    for (PropertySource<?> ps : configurableEnv.getPropertySources()) {
      if (!(ps instanceof EnumerablePropertySource<?> eps)) continue;
      String name = ps.getName();
      if (isFromConfigServer(name)) {
        sourceNames.add(name);
        for (String key : eps.getPropertyNames()) {
          totalKeyCount++;
          if (isVolatileOrSensitive(key)) {
            excludedKeyCount++;
          }
        }
      }
    }
    
    // Build response map
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("serviceName", application);
    response.put("profile", profile);
    
    // Hash
    response.put("hash", hash);
    
    // Snapshot
    Map<String, Object> snapshotMap = new LinkedHashMap<>();
    snapshotMap.put("application", snapshot.getApplication());
    snapshotMap.put("profile", snapshot.getProfile());
    snapshotMap.put("label", snapshot.getLabel());
    snapshotMap.put("version", snapshot.getVersion());
    snapshotMap.put("properties", snapshot.getProperties());
    snapshotMap.put("propertyCount", snapshot.getProperties().size());
    response.put("snapshot", snapshotMap);
    
    // Canonical string
    response.put("canonicalString", canonicalString);
    
    // Metadata
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("keyCount", snapshot.getProperties().size());
    metadata.put("excludedKeyCount", excludedKeyCount);
    metadata.put("totalKeyCount", totalKeyCount);
    metadata.put("sourceNames", sourceNames);
    metadata.put("computedAt", Instant.now().toString());
    response.put("metadata", metadata);
    
    return response;
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
  
  private Environment env() {
    return hashCalc.getEnvironment();
  }
}
