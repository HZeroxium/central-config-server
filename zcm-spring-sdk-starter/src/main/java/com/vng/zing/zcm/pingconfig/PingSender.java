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

/**
 * Sends periodic heartbeat ("ping") requests to a centralized control service.
 * <p>
 * This mechanism helps the control plane monitor the liveness and configuration
 * consistency of SDK-enabled services. The ping payload includes metadata such as:
 * <ul>
 *   <li>Service name and instance ID</li>
 *   <li>Configuration hash</li>
 *   <li>Host and port information</li>
 *   <li>Active profile and version</li>
 * </ul>
 *
 * <p>If pinging is disabled or the control URL is not configured,
 * the operation is safely skipped.
 */
@Slf4j
public class PingSender {

  private final RestClient rest;
  private final SdkProperties props;
  private final ConfigHashCalculator hash;
  private final Environment environment;

  /**
   * Creates a new {@code PingSender}.
   *
   * @param rest         Spring's {@link RestClient} used to perform the POST request
   * @param props        configuration properties for SDK (contains control URL, ping options, etc.)
   * @param hash         computes a SHA-256 hash of the current configuration
   * @param environment  Spring {@link Environment} for resolving runtime information
   */
  public PingSender(RestClient rest, SdkProperties props, ConfigHashCalculator hash, Environment environment) {
    this.rest = rest;
    this.props = props;
    this.hash = hash;
    this.environment = environment;
  }

  /**
   * Sends the heartbeat request to the control service.
   * <p>
   * If pinging is disabled or configuration is missing, it logs a warning and returns silently.
   * On network or HTTP failure, exceptions are caught and logged, ensuring no disruption
   * to scheduled tasks.
   */
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

    // --- Construct JSON payload ---
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
      // Swallow exception to prevent scheduler interruption
    }
  }

  /**
   * Resolves the local hostname of the machine.
   *
   * @return the hostname, or {@code "unknown"} if resolution fails
   */
  private String host() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      return "unknown";
    }
  }

  /**
   * Retrieves the current HTTP server port.
   *
   * @return port number from {@code server.port} or 8080 by default
   */
  private int getPort() {
    return environment.getProperty("server.port", Integer.class, 8080);
  }

  /**
   * Returns the active Spring profile or {@code "default"} if none is active.
   */
  private String getActiveProfile() {
    String[] profiles = environment.getActiveProfiles();
    return profiles.length > 0 ? profiles[0] : "default";
  }

  /**
   * Returns the current application version, defaulting to {@code 1.0.0}.
   */
  private String getVersion() {
    return environment.getProperty("spring.application.version", "1.0.0");
  }

  /**
   * Resolves the service instance ID used for identification in heartbeat payloads.
   * <p>
   * The resolution order is:
   * <ol>
   *   <li>{@code zcm.sdk.instance.id}</li>
   *   <li>{@code spring.cloud.consul.discovery.instance-id} (resolved placeholders)</li>
   *   <li>Fallback: {@code {serviceName}-{port}-{hostname}}</li>
   * </ol>
   *
   * @return resolved instance ID string
   */
  private String getInstanceId() {
    // Prefer explicit ZCM instance ID
    String instanceId = environment.getProperty("zcm.sdk.instance.id");
    if (StringUtils.hasText(instanceId)) {
      return instanceId;
    }

    // Fallback: derive from Consul pattern
    String consulInstanceId = environment.getProperty("spring.cloud.consul.discovery.instance-id");
    if (consulInstanceId != null && StringUtils.hasText(consulInstanceId)) {
      // Replace common placeholders with resolved values
      String resolved = consulInstanceId
          .replace("${server.port}", String.valueOf(getPort()))
          .replace("${random.value}", "unknown");
      return resolved;
    }

    // Final fallback pattern
    String hostname = host();
    return props.getServiceName() + "-" + getPort() + "-" + hostname;
  }
}
