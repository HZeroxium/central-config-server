package com.vng.zing.zcm.pingconfig;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.pingconfig.strategy.PingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates periodic heartbeat ("ping") requests to a centralized control service.
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
 * <p>The PingSender uses a pluggable strategy pattern to support multiple
 * communication protocols (HTTP REST, Thrift RPC, gRPC) and integrates with
 * service discovery for endpoint resolution with fallback to direct URLs.
 */
@Slf4j
public class PingSender {

  private final PingStrategy pingStrategy;
  private final DiscoveryClient discoveryClient;
  private final SdkProperties props;
  private final ConfigHashCalculator hash;
  private final Environment environment;

  /**
   * Creates a new {@code PingSender}.
   *
   * @param pingStrategy      the ping strategy implementation for the selected protocol
   * @param discoveryClient   service discovery client for finding control service instances
   * @param props            configuration properties for SDK (contains control URL, ping options, etc.)
   * @param hash             computes a SHA-256 hash of the current configuration
   * @param environment      Spring {@link Environment} for resolving runtime information
   */
  public PingSender(PingStrategy pingStrategy, 
                    DiscoveryClient discoveryClient,
                    SdkProperties props, 
                    ConfigHashCalculator hash, 
                    Environment environment) {
    this.pingStrategy = pingStrategy;
    this.discoveryClient = discoveryClient;
    this.props = props;
    this.hash = hash;
    this.environment = environment;
  }

  /**
   * Sends the heartbeat request to the control service using the configured strategy.
   * <p>
   * If pinging is disabled or no endpoint can be resolved, it logs a warning and returns silently.
   * On network or protocol failure, exceptions are caught and logged, ensuring no disruption
   * to scheduled tasks.
   */
  public void send() {
    if (!props.getPing().isEnabled()) {
      log.debug("ZCM ping disabled, skipping");
      return;
    }

    String endpoint = resolveEndpoint();
    if (!StringUtils.hasText(endpoint)) {
      log.warn("ZCM ping endpoint not resolved, skipping");
      return;
    }

    HeartbeatPayload payload = buildPayload();

    try {
      pingStrategy.sendHeartbeat(endpoint, payload);
      log.info("ZCM ping sent successfully using {} to {}", 
          pingStrategy.getName(), endpoint);
    } catch (Exception e) {
      log.error("ZCM ping failed using {}: {}", 
          pingStrategy.getName(), e.getMessage());
      // Swallow exception to prevent scheduler interruption
    }
  }

  /**
   * Resolves the endpoint for sending heartbeat using service discovery or direct URL.
   * 
   * @return resolved endpoint or null if no endpoint can be determined
   */
  private String resolveEndpoint() {
    // Try service discovery first
    String serviceDiscoveryName = props.getPing().getServiceDiscoveryName();
    if (StringUtils.hasText(serviceDiscoveryName)) {
      try {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceDiscoveryName);
        if (!instances.isEmpty()) {
          ServiceInstance instance = instances.get(0); // Simple selection - could be enhanced with load balancing
          String endpoint = buildEndpoint(instance);
          log.debug("Resolved endpoint via discovery: {}", endpoint);
          return endpoint;
        }
      } catch (Exception e) {
        log.warn("Service discovery failed, falling back to direct URL: {}", e.getMessage());
      }
    }

    // Fallback to direct URL
    String directUrl = props.getControlUrl();
    if (StringUtils.hasText(directUrl)) {
      log.debug("Using direct URL: {}", directUrl);
      return directUrl;
    }

    return null;
  }

  /**
   * Builds the appropriate endpoint format based on the ping protocol.
   * 
   * @param instance the service instance from discovery
   * @return formatted endpoint for the protocol
   */
  private String buildEndpoint(ServiceInstance instance) {
    return switch (pingStrategy.getProtocol()) {
      case HTTP -> instance.getUri().toString();
      case THRIFT -> {
        String thriftPort = instance.getMetadata().get("thrift_port");
        int port = thriftPort != null ? Integer.parseInt(thriftPort) : 9090;
        yield instance.getHost() + ":" + port;
      }
      case GRPC -> {
        String grpcPort = instance.getMetadata().get("grpc_port");
        int port = grpcPort != null ? Integer.parseInt(grpcPort) : 9091;
        yield instance.getHost() + ":" + port;
      }
    };
  }

  /**
   * Builds the heartbeat payload from current service information.
   * 
   * @return heartbeat payload object
   */
  private HeartbeatPayload buildPayload() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("hostname", host());
    metadata.put("profile", getActiveProfile());

    return HeartbeatPayload.builder()
        .serviceName(props.getServiceName())
        .instanceId(getInstanceId())
        .configHash(hash.currentHash())
        .host(host())
        .port(getPort())
        .environment(getActiveProfile())
        .version(getVersion())
        .metadata(metadata)
        .build();
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
