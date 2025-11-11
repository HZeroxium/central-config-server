package com.vng.zing.zcm.client.config;

import com.vng.zing.zcm.configsnapshot.ConfigSnapshotBuilder;
import com.vng.zing.zcm.pingconfig.ConfigHashCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
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
  
  private Environment env() {
    return hashCalc.getEnvironment();
  }
}
