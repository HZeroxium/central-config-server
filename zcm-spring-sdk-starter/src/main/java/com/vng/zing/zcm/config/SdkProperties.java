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

  /** Configuration for Unleash Feature Flags integration. */
  private FeatureFlags featureFlags = new FeatureFlags();

  /** Configuration for Key-Value store integration. */
  private KV kv = new KV();

  /** Configuration for API key authentication. */
  private ApiKey apiKey = new ApiKey();

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

    /** Protocol to use for ping communication (HTTP, THRIFT, GRPC, KAFKA). */
    private String protocol = "HTTP";

    /** Service discovery name for finding config-control-service instances. */
    private String serviceDiscoveryName = "config-control-service";

    /** Kafka configuration for ping communication (used when protocol is KAFKA). */
    private Kafka kafka = new Kafka();

    /** Config hash caching configuration. */
    private HashCache hashCache = new HashCache();

    /** Circuit breaker configuration for Kafka ping operations. */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * Kafka configuration for ping operations.
     * <p>
     * These properties are used as fallback when config-control-service is unavailable
     * or when explicit configuration is required. The SDK will attempt to fetch
     * Kafka configuration from config-control-service first.
     */
    @Data
    public static class Kafka {

      /**
       * Kafka bootstrap servers (comma-separated list).
       * <p>
       * Can be overridden via environment variable {@code ZCM_SDK_PING_KAFKA_BOOTSTRAP_SERVERS}.
       * If not set, SDK will fetch from config-control-service.
       */
      private String bootstrapServers;

      /**
       * Kafka topic for heartbeat messages.
       * <p>
       * Defaults to "heartbeat-queue" if not specified.
       * Can be overridden via environment variable {@code ZCM_SDK_PING_KAFKA_TOPIC}.
       * If not set, SDK will fetch from config-control-service.
       */
      private String topic = "heartbeat-queue";

      /**
       * Config refresh interval in milliseconds.
       * <p>
       * How often to refresh Kafka configuration from config-control-service.
       * Defaults to 5 minutes (300000ms).
       * Can be overridden via environment variable {@code ZCM_SDK_PING_KAFKA_CONFIG_REFRESH_INTERVAL}.
       */
      private long configRefreshInterval = 300000L; // 5 minutes
    }

    /**
     * Configuration for config hash caching.
     */
    @Data
    public static class HashCache {

      /**
       * Whether config hash caching is enabled.
       * <p>
       * When enabled, config hash is cached and only recalculated when cache expires
       * or when refresh events are received.
       */
      private boolean enabled = true;

      /**
       * Cache TTL in milliseconds.
       * <p>
       * Defaults to 60 seconds (60000ms), which is ping interval (30s) + 30s buffer.
       * This ensures cache hits during normal ping cycles while reducing calculation frequency.
       * Can be overridden via environment variable {@code ZCM_SDK_PING_HASH_CACHE_TTL}.
       */
      private long ttl = 60000L; // 60 seconds (ping interval 30s + 30s buffer)

      /**
       * Maximum cache size (number of entries).
       * <p>
       * Defaults to 1000 entries. Each unique combination of application + profile + label
       * creates one cache entry.
       */
      private int maxSize = 1000;
    }

    /**
     * Circuit breaker configuration for Kafka ping operations.
     */
    @Data
    public static class CircuitBreaker {

      /**
       * Whether circuit breaker is enabled for Kafka ping operations.
       * <p>
       * When enabled, circuit breaker will fail-fast when Kafka is down,
       * preventing retry storms and reducing scheduler blocking.
       */
      private boolean enabled = true;

      /**
       * Failure rate threshold percentage.
       * <p>
       * Circuit opens when failure rate exceeds this threshold.
       * Default: 50%
       */
      private int failureRateThreshold = 50;

      /**
       * Wait duration in open state (milliseconds).
       * <p>
       * How long circuit stays open before transitioning to half-open.
       * Default: 30000ms (30 seconds)
       */
      private long waitDurationInOpenState = 30000L;

      /**
       * Number of permitted calls in half-open state.
       * <p>
       * Default: 3
       */
      private int permittedNumberOfCallsInHalfOpenState = 3;

      /**
       * Sliding window size for failure rate calculation.
       * <p>
       * Default: 10
       */
      private int slidingWindowSize = 10;
    }
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

  /**
   * Configuration for Unleash Feature Flags integration.
   */
  @Data
  public static class FeatureFlags {

    /** Whether feature flags functionality is enabled. */
    private boolean enabled = false;

    /** Base URL of the Unleash Server API (e.g., http://unleash:4242/api/). */
    private String unleashApiUrl;

    /** API key (backend token) for authenticating with Unleash Server. */
    private String apiKey;

    /** Application name used for Unleash (defaults to serviceName). */
    private String appName;

    /** Instance ID for Unleash (defaults to instanceId). */
    private String instanceId;

    /** Whether to fetch flags synchronously on initialization. */
    private boolean synchronousFetchOnInitialisation = true;

    /** Interval in seconds for sending metrics to Unleash Server (default: 60s). */
    private int sendMetricsInterval = 60;
  }

  /**
   * Configuration for Key-Value store integration.
   */
  @Data
  public static class KV {

    /** Whether KV functionality is enabled. */
    private boolean enabled = false;

    /** Keycloak configuration for authentication. */
    private KVKeycloak keycloak = new KVKeycloak();
  }

  /**
   * Keycloak configuration for KV authentication.
   */
  @Data
  public static class KVKeycloak {

    /** Token endpoint URL (e.g., http://keycloak:8080/realms/config-control/protocol/openid-connect/token). */
    private String tokenEndpoint;

    /** Client ID for client credentials flow. */
    private String clientId;

    /** Client secret for client credentials flow. */
    private String clientSecret;

    /** Realm name (default: config-control). */
    private String realm = "config-control";
  }

  /**
   * Configuration for API key authentication.
   */
  @Data
  public static class ApiKey {

    /** Whether API key authentication is enabled. */
    private boolean enabled = false;

    /** The API key value to send in X-API-Key header. */
    private String key;
  }
}
