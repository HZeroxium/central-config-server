package com.vng.zing.zcm.client;

import lombok.Getter;
import org.springframework.core.env.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class ConfigHashCalculator {

  @Getter
  private final ConfigurableEnvironment environment;

  public ConfigHashCalculator(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  public String currentHash() {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      SortedMap<String, String> flat = new TreeMap<>();
      for (PropertySource<?> ps : environment.getPropertySources()) {
        if (!(ps instanceof EnumerablePropertySource<?> eps))
          continue;
        for (String name : eps.getPropertyNames()) {
          if (isSensitive(name))
            continue;
          Object v = eps.getProperty(name);
          if (v != null)
            flat.put(name, String.valueOf(v));
        }
      }
      for (Map.Entry<String, String> e : flat.entrySet()) {
        md.update(e.getKey().getBytes(StandardCharsets.UTF_8));
        md.update((byte) '=');
        md.update(e.getValue().getBytes(StandardCharsets.UTF_8));
        md.update((byte) '\n');
      }
      byte[] digest = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte b : digest)
        sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      return "NA";
    }
  }

  private boolean isSensitive(String key) {
    String k = key.toLowerCase(Locale.ROOT);
    return k.contains("password") || k.contains("secret") || k.contains("token") || k.contains("credential");
  }
}
