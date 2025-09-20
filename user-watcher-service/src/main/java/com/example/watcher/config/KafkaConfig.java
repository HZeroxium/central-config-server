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
import com.example.kafka.serialization.AvroSerializer;
import com.example.kafka.serialization.AvroDeserializer;

import java.util.Map;

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
    return new DefaultKafkaProducerFactory<>(cfg);
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

  // Avro Producer Factory
  @Bean
  public ProducerFactory<String, Object> avroProducerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties();
    cfg.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
    cfg.putIfAbsent(ProducerConfig.RETRIES_CONFIG, 2147483647);
    cfg.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    cfg.putIfAbsent(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
    cfg.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer");
    cfg.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class.getName());
    cfg.putIfAbsent("schema.registry.url", "http://localhost:8087");
    cfg.putIfAbsent("auto.register.schemas", "true");
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  // Avro Consumer Factory
  @Bean
  public ConsumerFactory<String, Object> avroConsumerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildConsumerProperties();
    cfg.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    cfg.putIfAbsent(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
    cfg.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    cfg.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class.getName());
    cfg.putIfAbsent("schema.registry.url", "http://localhost:8087");
    cfg.putIfAbsent("specific.avro.reader", "true");
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
