package com.vng.zing.zcm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Represents the centralized configuration properties for the ZCM SDK.
 * <p>
 * These properties are loaded from Spring Boot’s configuration system
 * using the prefix {@code zcm.sdk}.
 * <p>
 * The configuration includes service metadata (name, instance),
 * Config Server endpoints, discovery setup, load balancing (LB),
 * periodic ping behavior, and Spring Cloud Bus configuration.
 *
 * <p>Example YAML structure:
 * <pre>
 * zcm:
 *   sdk:
 *     service-name: sample-service
 *     instance-id: instance-001
 *     config-server-url: http://config:8888
 *     discovery:
 *       provider: CONSUL
 *       consul:
 *         host: localhost
 *         port: 8500
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "zcm.sdk")
public class SdkProperties {

  /** The logical service name registered with Discovery (e.g., "user-service"). */
  private String serviceName;

  /** The unique identifier for this service instance. */
  private String instanceId;

  /** Base URL of the centralized Spring Cloud Config Server (e.g., http://config:8888). */
  private String configServerUrl;

  /** Base URL of the Control Service (used for configuration commands). */
  private String controlUrl;

  /** Configuration for service discovery mechanism (Consul or Control). */
  private Discovery discovery = new Discovery();

  /** Configuration for Load Balancer strategy (e.g., ROUND_ROBIN). */
  private Lb lb = new Lb();

  /** Configuration for periodic heartbeat/ping mechanism. */
  private Ping ping = new Ping();

  /** Configuration for Spring Cloud Bus integration (for config refresh events). */
  private Bus bus = new Bus();

  /**
   * Configuration related to service discovery.
   */
  @Data
  public static class Discovery {
    /** The selected discovery provider (CONSUL or CONTROL). */
    private Provider provider = Provider.CONSUL;

    /** Consul-specific configuration. */
    private Consul consul = new Consul();
  }

  /**
   * Supported service discovery providers.
   */
  public enum Provider {
    CONSUL, CONTROL
  }

  /**
   * Consul-related discovery configuration mapped to Spring Cloud Consul properties.
   */
  @Data
  public static class Consul {

    /** Hostname of the Consul agent (default: localhost). */
    private String host = "localhost";

    /** Port of the Consul agent (default: 8500). */
    private int port = 8500;

    /** Whether heartbeat registration is enabled. */
    private boolean heartbeatEnabled = true;

    /** TTL for Consul heartbeat, mapped to spring.cloud.consul.discovery.heartbeat.ttl. */
    private String heartbeatTtl = "10s";

    /** Whether the service should be registered with Consul. */
    private boolean register = true;

    /** Whether to prefer IP address registration over hostname. */
    private boolean preferIpAddress = true;

    /** Time to deregister service after critical state, mapped to Consul property. */
    private String deregisterCriticalServiceAfter = "60s";
  }

  /**
   * Load balancing (LB) configuration.
   */
  @Data
  public static class Lb {

    /**
     * The selected load balancing policy.
     * Supported values: ROUND_ROBIN, RANDOM, WEIGHTED_RANDOM.
     */
    private String policy = "ROUND_ROBIN";
  }

  /**
   * Configuration for background ping or health check mechanism.
   */
  @Data
  public static class Ping {

    /** Whether the periodic ping task is enabled. */
    private boolean enabled = true;

    /** Delay between ping executions in milliseconds. */
    private String fixedDelay = "30000";
  }

  /**
   * Configuration for Spring Cloud Bus events.
   */
  @Data
  public static class Bus {

    /** Whether automatic config refresh events via Cloud Bus are enabled. */
    private boolean refreshEnabled = false;

    /** Kafka topic used for config refresh notifications. */
    private String refreshTopic = "springCloudBus";

    /** Kafka bootstrap servers for Cloud Bus communication. */
    private String kafkaBootstrapServers;
  }
}
