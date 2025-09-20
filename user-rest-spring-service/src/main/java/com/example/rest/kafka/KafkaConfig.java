package com.example.rest.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.ExponentialBackOff;

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
    cfg.putIfAbsent(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "rest-service-tx-");
    DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(cfg);
    factory.setTransactionIdPrefix("rest-service-tx-");
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

  @Bean(name = "txFactory")
  public ConcurrentKafkaListenerContainerFactory<String, String> txFactory(
      @Qualifier("consumerFactory") ConsumerFactory<String, String> cf,
      @Qualifier("kafkaTemplate") KafkaTemplate<String, String> template) {

    var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

    // Error handler with DLT
    var backoff = new ExponentialBackOff();
    backoff.setInitialInterval(500);
    backoff.setMultiplier(2.0);
    backoff.setMaxInterval(5000);

    var eh = new DefaultErrorHandler(backoff);
    f.setCommonErrorHandler(eh);
    return f;
  }

  @Bean
  public StringJsonMessageConverter messageConverter() {
    return new StringJsonMessageConverter();
  }
}
