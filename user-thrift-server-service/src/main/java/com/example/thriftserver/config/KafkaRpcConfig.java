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
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaRpcConfig {

  @Bean
  public ProducerFactory<String, String> rpcProducerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties();
    cfg.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  @Bean
  public ConsumerFactory<String, String> rpcConsumerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildConsumerProperties();
    cfg.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "user-thrift-server");
    return new DefaultKafkaConsumerFactory<>(cfg);
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(
      @Qualifier("rpcProducerFactory") ProducerFactory<String, String> pf) {
    return new KafkaTemplate<>(pf);
  }

  @Bean
  public ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate(
      @Qualifier("rpcProducerFactory") ProducerFactory<String, String> pf,
      @Qualifier("rpcConsumerFactory") ConsumerFactory<String, String> cf) {
    // Create a separate container factory for RPC template
    ConcurrentKafkaListenerContainerFactory<String, String> rpcFactory = new ConcurrentKafkaListenerContainerFactory<>();
    rpcFactory.setConsumerFactory(cf);
    rpcFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
    rpcFactory.setBatchListener(false); // Ensure it's not a batch listener

    // Use a template that waits for replies from topic configured at send time (via
    // headers)
    ReplyingKafkaTemplate<String, String, String> template = new ReplyingKafkaTemplate<>(pf,
        rpcFactory.createContainer("user.rpc.reply"));
    return template;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      @Qualifier("rpcConsumerFactory") ConsumerFactory<String, String> cf) {
    ConcurrentKafkaListenerContainerFactory<String, String> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setBatchListener(false);
    return f;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> rpcListenerFactory(
      @Qualifier("rpcConsumerFactory") ConsumerFactory<String, String> cf) {
    ConcurrentKafkaListenerContainerFactory<String, String> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setBatchListener(false); // Ensure it's not a batch listener
    return f;
  }
}
