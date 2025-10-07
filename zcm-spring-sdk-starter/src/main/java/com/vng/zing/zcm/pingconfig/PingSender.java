package com.vng.zing.zcm.pingconfig;

import com.vng.zing.zcm.config.SdkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PingSender {

  private final RestClient rest;
  private final SdkProperties props;
  private final ConfigHashCalculator hash;
  private final Environment environment;

  public PingSender(RestClient rest, SdkProperties props, ConfigHashCalculator hash, Environment environment) {
    this.rest = rest;
    this.props = props;
    this.hash = hash;
    this.environment = environment;
  }

  public void send() {
    if (!props.getPing().isEnabled()) {
      log.debug("ZCM ping disabled, skipping");
      return;
    }
    String base = props.getControlUrl();
    if (!StringUtils.hasText(base)) {
      log.warn("ZCM ping control URL not configured, skipping");
      return;
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("serviceName", props.getServiceName());
    payload.put("instanceId", getInstanceId());
    payload.put("configHash", hash.currentHash());
    payload.put("host", host());
    payload.put("port", getPort());
    payload.put("environment", getActiveProfile());
    payload.put("version", getVersion());
    
    Map<String, String> metadata = new HashMap<>();
    metadata.put("hostname", host());
    metadata.put("profile", getActiveProfile());
    payload.put("metadata", metadata);

    log.debug("ZCM ping sending to {} with payload: {}", base + "/api/heartbeat", payload);
    try {
      rest.post()
          .uri(base + "/api/heartbeat")
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
      log.info("ZCM ping sent successfully to control service");
    } catch (Exception e) {
      log.error("ZCM ping failed: {}", e.getMessage());
      // swallow, avoid crashing schedule loop
    }
  }

  private String host() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      return "unknown";
    }
  }

  private int getPort() {
    return environment.getProperty("server.port", Integer.class, 8080);
  }

  private String getActiveProfile() {
    String[] profiles = environment.getActiveProfiles();
    return profiles.length > 0 ? profiles[0] : "default";
  }

  private String getVersion() {
    return environment.getProperty("spring.application.version", "1.0.0");
  }

  private String getInstanceId() {
    // Try to get from zcm.sdk.instance.id first, then fallback to Consul instance-id
    String instanceId = environment.getProperty("zcm.sdk.instance.id");
    if (StringUtils.hasText(instanceId)) {
      return instanceId;
    }
    
    // Fallback to Consul instance-id pattern
    String consulInstanceId = environment.getProperty("spring.cloud.consul.discovery.instance-id");
    if (StringUtils.hasText(consulInstanceId)) {
      // Resolve placeholders like ${server.port} and ${random.value}
      if (consulInstanceId != null) {
        String resolved = consulInstanceId
            .replace("${server.port}", String.valueOf(getPort()))
            .replace("${random.value}", "unknown"); // fallback for random
        return resolved;
      }
    }
    
    // Final fallback
    String hostname = host();
    return props.getServiceName() + "-" + getPort() + "-" + hostname;
  }
}
