package com.example.thriftserver.config;

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
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import com.example.kafka.serialization.AvroSerializer;
import com.example.kafka.serialization.AvroDeserializer;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaRpcConfig {

  @Bean
  public ReplyingKafkaTemplate<String, Object, Object> replyingKafkaTemplate(
      @Qualifier("avroProducerFactory") ProducerFactory<String, Object> pf,
      @Qualifier("avroConsumerFactory") ConsumerFactory<String, Object> cf) {
    // Create a separate container factory for RPC template
    ConcurrentKafkaListenerContainerFactory<String, Object> rpcFactory = new ConcurrentKafkaListenerContainerFactory<>();
    rpcFactory.setConsumerFactory(cf);
    rpcFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
    rpcFactory.setBatchListener(false); // Ensure it's not a batch listener

    // Create a template that can handle dynamic reply topics
    // We need to provide at least one topic for the container to be valid
    ReplyingKafkaTemplate<String, Object, Object> template = new ReplyingKafkaTemplate<String, Object, Object>(pf,
        rpcFactory.createContainer("temp-reply-topic"));
    return template;
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

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> rpcListenerFactory(
      @Qualifier("avroConsumerFactory") ConsumerFactory<String, Object> cf) {
    ConcurrentKafkaListenerContainerFactory<String, Object> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setBatchListener(false); // Ensure it's not a batch listener
    return f;
  }

  // Avro Producer Factory
  @Bean
  public ProducerFactory<String, Object> avroProducerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties();
    cfg.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
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
    cfg.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "user-thrift-server-avro");
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
}
