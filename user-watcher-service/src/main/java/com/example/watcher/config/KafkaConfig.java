package com.example.watcher.config;

import com.example.kafka.serialization.ThriftKafkaSerializers;
import com.example.watcher.constants.WatcherConstants;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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
      @Qualifier("thriftProducerFactory") ProducerFactory<String, Object> pf) {
    return new KafkaTemplate<>(pf);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      @Qualifier("thriftConsumerFactory") ConsumerFactory<String, Object> cf) {
    ConcurrentKafkaListenerContainerFactory<String, Object> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setBatchListener(false);
    return f;
  }

  // Thrift Producer Factory using custom Thrift serializers
  @Bean
  public ProducerFactory<String, Object> thriftProducerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties();
    cfg.put(ProducerConfig.ACKS_CONFIG, "all");
    cfg.put(ProducerConfig.RETRIES_CONFIG, 2147483647);
    cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    cfg.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
    cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftKafkaSerializers.ThriftObjectSerializer.class);
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  // Thrift Consumer Factory using custom Thrift deserializers
  @Bean
  public ConsumerFactory<String, Object> thriftConsumerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildConsumerProperties();
    cfg.put(ConsumerConfig.GROUP_ID_CONFIG, WatcherConstants.CONSUMER_GROUP_ID);
    cfg.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    cfg.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
    cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    // We'll use ByteArrayDeserializer and handle type-specific deserialization in
    // listeners
    cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    return new DefaultKafkaConsumerFactory<>(cfg);
  }

  // Thrift Kafka Template
  @Bean
  public KafkaTemplate<String, Object> thriftKafkaTemplate(
      @Qualifier("thriftProducerFactory") ProducerFactory<String, Object> pf) {
    return new KafkaTemplate<>(pf);
  }

  // Thrift Listener Container Factory
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> thriftKafkaListenerContainerFactory(
      @Qualifier("thriftConsumerFactory") ConsumerFactory<String, Object> cf) {
    ConcurrentKafkaListenerContainerFactory<String, Object> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setBatchListener(false);
    return f;
  }

  // Backward compatibility beans (for transition period)
  @Bean
  public ProducerFactory<String, Object> avroProducerFactory(
      @Qualifier("thriftProducerFactory") ProducerFactory<String, Object> pf) {
    return pf;
  }

  @Bean
  public ConsumerFactory<String, Object> avroConsumerFactory(
      @Qualifier("thriftConsumerFactory") ConsumerFactory<String, Object> cf) {
    return cf;
  }

  @Bean
  public KafkaTemplate<String, Object> avroKafkaTemplate(
      @Qualifier("thriftKafkaTemplate") KafkaTemplate<String, Object> template) {
    return template;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> avroKafkaListenerContainerFactory(
      @Qualifier("thriftKafkaListenerContainerFactory") ConcurrentKafkaListenerContainerFactory<String, Object> factory) {
    return factory;
  }
}