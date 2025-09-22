package com.example.rest.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.Map;

/**
 * Kafka configuration for REST service
 */
@EnableKafka
@Configuration
public class KafkaConfig {

  @Bean
  public ProducerFactory<String, String> producerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties();
    cfg.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
    cfg.putIfAbsent(ProducerConfig.RETRIES_CONFIG, 2147483647);
    cfg.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    cfg.putIfAbsent(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
    // Remove orchestrator-specific transactional prefix; keep idempotence enabled
    cfg.remove(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
    DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(cfg);
    return factory;
  }

  @Bean
  public ConsumerFactory<String, String> consumerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildConsumerProperties();
    cfg.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    cfg.putIfAbsent(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
    return new DefaultKafkaConsumerFactory<>(cfg);
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
    return new KafkaTemplate<>(pf);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> cf) {
    ConcurrentKafkaListenerContainerFactory<String, String> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setBatchListener(false);
    return f;
  }

  // Removed specialized transactional container factory previously used by
  // orchestrator

  @Bean
  public StringJsonMessageConverter messageConverter() {
    return new StringJsonMessageConverter();
  }
}
