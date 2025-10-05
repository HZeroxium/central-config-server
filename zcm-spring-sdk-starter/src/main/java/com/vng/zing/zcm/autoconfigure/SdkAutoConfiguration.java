package com.vng.zing.zcm.autoconfigure;

import com.vng.zing.zcm.client.*;
import com.vng.zing.zcm.config.SdkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@EnableConfigurationProperties(SdkProperties.class)
@EnableScheduling
@LoadBalancerClients
@RequiredArgsConstructor
public class SdkAutoConfiguration {

  private final SdkProperties props;

  @Bean
  @ConditionalOnMissingBean(name = "zcmLoadBalancedWebClientBuilder")
  @org.springframework.cloud.client.loadbalancer.LoadBalanced
  public WebClient.Builder zcmLoadBalancedWebClientBuilder() {
    return WebClient.builder()
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build());
  }

  @Bean
  @ConditionalOnMissingBean
  public ConfigHashCalculator configHashCalculator(org.springframework.core.env.ConfigurableEnvironment env) {
    return new ConfigHashCalculator(env);
  }

  @Bean
  @ConditionalOnMissingBean
  public PingSender pingSender(ConfigHashCalculator hashCalc) {
    // Create RestClient for simple JSON POST (non-LB)
    RestClient restClient = RestClient.builder().build();
    return new PingSender(restClient, props, hashCalc);
  }

  @Bean
  @ConditionalOnMissingBean
  public PingScheduler pingScheduler(PingSender sender) {
    return new PingScheduler(props, sender);
  }

  @Bean
  @ConditionalOnMissingBean
  public ConfigRefresher configRefresher(org.springframework.cloud.context.refresh.ContextRefresher refresher,
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
  public org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      org.springframework.kafka.core.ConsumerFactory<String, String> cf) {
    var f = new org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, String>();
    f.setConsumerFactory(cf);
    return f;
  }

  // Basic ConsumerFactory (if app hasn't defined one)
  @Bean
  @ConditionalOnMissingBean(org.springframework.kafka.core.ConsumerFactory.class)
  public org.springframework.kafka.core.ConsumerFactory<String, String> consumerFactory(
      org.springframework.core.env.Environment env) {
    java.util.Map<String, Object> cfg = new java.util.HashMap<>();
    cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        env.getProperty("spring.kafka.bootstrap-servers", props.getBus().getKafkaBootstrapServers()));
    cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "zcm-sdk-starter");
    cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringDeserializer.class);
    cfg.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringDeserializer.class);
    return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(cfg);
  }
}
