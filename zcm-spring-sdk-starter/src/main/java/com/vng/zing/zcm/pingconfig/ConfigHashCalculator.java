package com.vng.zing.zcm.pingconfig;

import com.vng.zing.zcm.configsnapshot.ConfigSnapshotBuilder;

import lombok.Getter;

import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Provides a stable SHA-256 hash of effective configuration from Spring Cloud Config.
 */
public class ConfigHashCalculator {

  @Getter
  private final ConfigurableEnvironment environment;

  public ConfigHashCalculator(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  public String currentHash() {
    try {
      String application = environment.getProperty("spring.application.name", "unknown");
      String[] profiles = environment.getActiveProfiles();
      String profile = profiles.length > 0 ? profiles[0] : "default";
      String label = environment.getProperty("spring.cloud.config.label");
      String version = environment.getProperty("config.client.version");

      var snapshot = new ConfigSnapshotBuilder(environment)
          .build(application, profile, label, version);
      String canonical = snapshot.toCanonicalString();
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      return "NA";
    }
  }
}
