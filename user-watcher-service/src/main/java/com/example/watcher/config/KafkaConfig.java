package com.example.watcher.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      @Qualifier("avroProducerFactory") ProducerFactory<String, Object> pf) {
    return new KafkaTemplate<>(pf);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      @Qualifier("avroConsumerFactory") ConsumerFactory<String, Object> cf) {
    ConcurrentKafkaListenerContainerFactory<String, Object> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setBatchListener(false);
    return f;
  }

  // Avro Producer Factory using Confluent serializers
  @Bean
  public ProducerFactory<String, Object> avroProducerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties();
    cfg.put(ProducerConfig.ACKS_CONFIG, "all");
    cfg.put(ProducerConfig.RETRIES_CONFIG, 2147483647);
    cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    cfg.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
    cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer");
    cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        "io.confluent.kafka.serializers.KafkaAvroSerializer");
    cfg.put("schema.registry.url", "http://schema-registry:8081");
    cfg.put("auto.register.schemas", "true");
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  // Avro Consumer Factory using Confluent deserializers
  @Bean
  public ConsumerFactory<String, Object> avroConsumerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildConsumerProperties();
    cfg.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    cfg.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
    cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "io.confluent.kafka.serializers.KafkaAvroDeserializer");
    cfg.put("schema.registry.url", "http://schema-registry:8081");
    cfg.put("specific.avro.reader", "true");
    return new DefaultKafkaConsumerFactory<>(cfg);
  }

  // Avro Kafka Template
  @Bean
  public KafkaTemplate<String, Object> avroKafkaTemplate(
      @Qualifier("avroProducerFactory") ProducerFactory<String, Object> pf) {
    return new KafkaTemplate<>(pf);
  }

  // Avro Listener Container Factory
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> avroKafkaListenerContainerFactory(
      @Qualifier("avroConsumerFactory") ConsumerFactory<String, Object> cf) {
    ConcurrentKafkaListenerContainerFactory<String, Object> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setBatchListener(false);
    return f;
  }
}
