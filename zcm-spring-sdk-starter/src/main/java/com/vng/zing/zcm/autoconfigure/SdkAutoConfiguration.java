package com.vng.zing.zcm.autoconfigure;

import com.vng.zing.zcm.client.*;
import com.vng.zing.zcm.config.SdkProperties;
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
import org.springframework.web.client.RestClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@AutoConfiguration
@EnableConfigurationProperties(SdkProperties.class)
@EnableScheduling
@LoadBalancerClients
@RequiredArgsConstructor
public class SdkAutoConfiguration {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SdkAutoConfiguration.class);

  private final SdkProperties props;

  @Bean
  @ConditionalOnMissingBean(name = "zcmLoadBalancedWebClientBuilder")
  @LoadBalanced
  public WebClient.Builder zcmLoadBalancedWebClientBuilder() {
    return WebClient.builder()
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build());
  }

  @Bean
  @ConditionalOnMissingBean
  public ConfigHashCalculator configHashCalculator(ConfigurableEnvironment env) {
    return new ConfigHashCalculator(env);
  }

  @Bean
  @ConditionalOnMissingBean
  public PingSender pingSender(@Lazy RestClient.Builder restBuilder, ConfigHashCalculator hashCalc) {
    // Create RestClient for simple JSON POST (non-LB)
    RestClient restClient = restBuilder.build();
    return new PingSender(restClient, props, hashCalc);
  }

  @Bean
  @ConditionalOnMissingBean
  public PingScheduler pingScheduler(PingSender sender) {
    return new PingScheduler(props, sender);
  }

  @Bean
  @ConditionalOnMissingBean
  public ConfigRefresher configRefresher(ContextRefresher refresher,
      ConfigHashCalculator hashCalc) {
    return new ConfigRefresher(refresher, hashCalc);
  }

  @Bean
  @ConditionalOnMissingBean
  public RefreshListener zcmRefreshListener(SdkProperties properties, ConfigRefresher refresher) {
    return new RefreshListener(properties, refresher);
  }

  @Bean
  @ConditionalOnMissingBean
  public ClientApi zcmClientApi(WebClient.Builder lbWebClientBuilder,
      DiscoveryClient discoveryClient,
      ConfigHashCalculator hashCalc,
      PingSender pingSender) {
    return new ClientImpl(props, lbWebClientBuilder, discoveryClient, hashCalc, pingSender);
  }

  // ----- Bean property resolvers for SpEL -----

  @Bean("zcmRefreshTopic")
  public String zcmRefreshTopic() {
    return props.getBus().getRefreshTopic();
  }

  @Bean("zcmRefreshAutoStartup")
  public Boolean zcmRefreshAutoStartup() {
    return props.getBus().isRefreshEnabled();
  }

  // Basic Kafka Listener Container Factory (if app hasn't defined one)
  @Bean
  @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> cf) {
    var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
    f.setConsumerFactory(cf);
    return f;
  }

  // Basic ConsumerFactory (if app hasn't defined one)
  @Bean
  @ConditionalOnMissingBean(ConsumerFactory.class)
  public ConsumerFactory<String, String> consumerFactory(Environment env) {
    Map<String, Object> cfg = new HashMap<>();
    cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        env.getProperty("spring.kafka.bootstrap-servers", props.getBus().getKafkaBootstrapServers()));
    cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "zcm-sdk-starter");
    cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
    cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(cfg);
  }
}
