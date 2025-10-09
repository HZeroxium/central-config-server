package com.vng.zing.zcm.autoconfigure;

import com.vng.zing.zcm.client.*;
import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategyFactory;
import com.vng.zing.zcm.pingconfig.ConfigHashCalculator;
import com.vng.zing.zcm.pingconfig.ConfigRefresher;
import com.vng.zing.zcm.pingconfig.PingScheduler;
import com.vng.zing.zcm.pingconfig.PingSender;
import com.vng.zing.zcm.pingconfig.RefreshListener;
import com.vng.zing.zcm.pingconfig.strategy.PingStrategy;
import com.vng.zing.zcm.pingconfig.strategy.PingProtocol;
import com.vng.zing.zcm.pingconfig.strategy.HttpRestPingStrategy;
import com.vng.zing.zcm.pingconfig.strategy.ThriftRpcPingStrategy;
import com.vng.zing.zcm.pingconfig.strategy.GrpcPingStrategy;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration class for the ZCM SDK integration.
 * <p>
 * This class bootstraps and wires together all core components of the SDK,
 * including:
 * <ul>
 *   <li>Config hash calculator for drift detection</li>
 *   <li>Ping and heartbeat scheduler for service liveness</li>
 *   <li>Config refresher and listener for dynamic configuration reload</li>
 *   <li>Client API abstraction for service-to-service communication</li>
 *   <li>Basic Kafka consumer factory and listener container</li>
 * </ul>
 *
 * <p>All beans are conditionally created if the application context does not
 * already define its own version (using {@link ConditionalOnMissingBean}).
 *
 * <p>Annotations used:
 * <ul>
 *   <li>{@link AutoConfiguration} — marks this class for Spring Boot auto-detection.</li>
 *   <li>{@link EnableConfigurationProperties} — binds {@link SdkProperties}.</li>
 *   <li>{@link EnableScheduling} — enables scheduled heartbeats.</li>
 *   <li>{@link LoadBalancerClients} — enables Spring Cloud LoadBalancer support.</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(SdkProperties.class)
@EnableScheduling
@LoadBalancerClients
@RequiredArgsConstructor
public class SdkAutoConfiguration {

  /** Centralized SDK configuration loaded from {@code application.yml} (prefix: zcm.sdk). */
  private final SdkProperties props;

  // ======================================================================
  //  SECTION 1. Core Networking Components
  // ======================================================================

  /**
   * Provides a load-balanced {@link RestClient.Builder} that supports resolving
   * logical service names via Spring Cloud Discovery.
   * <p>
   * RestClient provides better performance and simpler API compared to WebClient
   * for synchronous HTTP operations.
   *
   * @return configured {@link RestClient.Builder}
   */
  @Bean
  @ConditionalOnMissingBean(name = "zcmLoadBalancedRestClientBuilder")
  @LoadBalanced
  public RestClient.Builder zcmLoadBalancedRestClientBuilder() {
    return RestClient.builder()
        .defaultStatusHandler(
            httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
            (request, response) -> {
              throw new RuntimeException("HTTP error: " + response.getStatusCode());
            });
  }

  // ======================================================================
  //  SECTION 2. Configuration Management & Refresh
  // ======================================================================

  /**
   * Creates a {@link ConfigHashCalculator} for computing SHA-256 hashes
   * of the effective Spring configuration.
   *
   * @param env active {@link ConfigurableEnvironment}
   * @return a {@link ConfigHashCalculator} instance
   */
  @Bean
  @ConditionalOnMissingBean
  public ConfigHashCalculator configHashCalculator(ConfigurableEnvironment env) {
    return new ConfigHashCalculator(env);
  }

  /**
   * Creates a {@link PingStrategy} based on the configured protocol.
   *
   * @return the appropriate ping strategy implementation
   */
  @Bean
  @ConditionalOnMissingBean
  public PingStrategy pingStrategy() {
    String protocol = props.getPing().getProtocol();
    PingProtocol pingProtocol = PingProtocol.fromString(protocol);
    
    return switch (pingProtocol) {
      case HTTP -> new HttpRestPingStrategy();
      case THRIFT -> new ThriftRpcPingStrategy();
      case GRPC -> new GrpcPingStrategy();
    };
  }

  /**
   * Creates a {@link PingSender} responsible for sending heartbeat (ping)
   * requests to the control plane using the configured strategy.
   *
   * @param pingStrategy the ping strategy implementation
   * @param discoveryClient service discovery client
   * @param hashCalc    {@link ConfigHashCalculator} for config consistency check
   * @param environment current {@link Environment}
   * @return a {@link PingSender} bean
   */
  @Bean
  @ConditionalOnMissingBean
  public PingSender pingSender(PingStrategy pingStrategy,
                               DiscoveryClient discoveryClient,
                               ConfigHashCalculator hashCalc, 
                               Environment environment) {
    return new PingSender(pingStrategy, discoveryClient, props, hashCalc, environment);
  }

  /**
   * Creates a {@link PingScheduler} which periodically triggers {@link PingSender}.
   * <p>
   * The delay interval is controlled by {@code zcm.sdk.ping.fixed-delay} (default: 30s).
   *
   * @param sender the heartbeat sender
   * @return the scheduler bean
   */
  @Bean
  @ConditionalOnMissingBean
  public PingScheduler pingScheduler(PingSender sender) {
    return new PingScheduler(props, sender);
  }

  /**
   * Creates a {@link ConfigRefresher} that triggers Spring’s {@link ContextRefresher}
   * when a config change is detected.
   *
   * @param refresher the Spring context refresher
   * @param hashCalc  used for hash comparison
   * @return the config refresher bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ConfigRefresher configRefresher(ContextRefresher refresher, ConfigHashCalculator hashCalc) {
    return new ConfigRefresher(refresher, hashCalc);
  }

  /**
   * Registers a {@link RefreshListener} which listens for configuration refresh
   * events from the message bus (e.g., Kafka topic).
   *
   * @param refresher the {@link ConfigRefresher}
   * @return a new {@link RefreshListener} bean
   */
  @Bean
  @ConditionalOnMissingBean
  public RefreshListener zcmRefreshListener(ConfigRefresher refresher) {
    return new RefreshListener(refresher);
  }

  // ======================================================================
  //  SECTION 3. Load Balancing & Client API
  // ======================================================================

  /**
   * Initializes the default {@link LoadBalancerStrategy} based on configuration.
   * Supported policies include ROUND_ROBIN, RANDOM, and WEIGHTED_RANDOM.
   *
   * @return the chosen {@link LoadBalancerStrategy}
   */
  @Bean
  @ConditionalOnMissingBean
  public LoadBalancerStrategy loadBalancerStrategy() {
    String policy = props.getLb().getPolicy();
    return LoadBalancerStrategyFactory.create(policy);
  }

  /**
   * Builds the main {@link ClientApi} bean used by SDK consumers.
   * This combines discovery, configuration hash, load balancing, and ping features.
   *
   * @param lbRestClientBuilder load-balanced RestClient
   * @param discoveryClient    service discovery client
   * @param hashCalc           config hash calculator
   * @param pingSender         ping sender
   * @param loadBalancerStrategy chosen LB strategy
   * @return a fully configured {@link ClientApi} implementation
   */
  @Bean
  @ConditionalOnMissingBean
  public ClientApi zcmClientApi(RestClient.Builder lbRestClientBuilder,
      DiscoveryClient discoveryClient,
      ConfigHashCalculator hashCalc,
      PingSender pingSender,
      LoadBalancerStrategy loadBalancerStrategy) {
    return new ClientImpl(lbRestClientBuilder, discoveryClient, hashCalc, pingSender, loadBalancerStrategy);
  }

  // ======================================================================
  //  SECTION 4. Spring Expression Language (SpEL) Helpers
  // ======================================================================

  /**
   * Exposes the ZCM refresh topic for SpEL expressions.
   *
   * @return the Kafka topic name used for configuration refresh events
   */
  @Bean("zcmRefreshTopic")
  public String zcmRefreshTopic() {
    return props.getBus().getRefreshTopic();
  }

  /**
   * Exposes whether refresh events should be auto-started.
   *
   * @return {@code true} if automatic config refresh is enabled
   */
  @Bean("zcmRefreshAutoStartup")
  public Boolean zcmRefreshAutoStartup() {
    return props.getBus().isRefreshEnabled();
  }

  // ======================================================================
  //  SECTION 5. Kafka Integration (Optional, fallback defaults)
  // ======================================================================

  /**
   * Provides a basic {@link ConcurrentKafkaListenerContainerFactory} if none exists.
   * <p>
   * This is primarily used to receive configuration refresh messages from Kafka.
   *
   * @param cf the Kafka {@link ConsumerFactory}
   * @return a listener container factory
   */
  @Bean
  @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> cf) {
    var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
    f.setConsumerFactory(cf);
    return f;
  }

  /**
   * Provides a minimal default Kafka {@link ConsumerFactory} if not already defined.
   * <p>
   * It configures basic deserializers, group ID, and bootstrap servers for internal
   * event consumption.
   *
   * @param env Spring environment to resolve properties
   * @return a configured {@link ConsumerFactory}
   */
  @Bean
  @ConditionalOnMissingBean(ConsumerFactory.class)
  public ConsumerFactory<String, String> consumerFactory(Environment env) {
    Map<String, Object> cfg = new HashMap<>();
    cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        env.getProperty("spring.kafka.bootstrap-servers", props.getBus().getKafkaBootstrapServers()));
    cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "zcm-sdk-starter");
    cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(cfg);
  }
}
