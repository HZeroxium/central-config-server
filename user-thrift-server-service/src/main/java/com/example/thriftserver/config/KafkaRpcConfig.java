package com.example.thriftserver.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.Map;

@Configuration
public class KafkaRpcConfig {

  @Bean
  public ProducerFactory<String, Object> rpcProducerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties();
    cfg.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  @Bean
  public ConsumerFactory<String, Object> rpcConsumerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildConsumerProperties();
    cfg.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "user-thrift-server");
    return new DefaultKafkaConsumerFactory<>(cfg);
  }

  @Bean
  public ReplyingKafkaTemplate<String, Object, Object> replyingKafkaTemplate(
      ProducerFactory<String, Object> pf,
      ConsumerFactory<String, Object> cf) {
    // Create a separate container factory for RPC template
    ConcurrentKafkaListenerContainerFactory<String, Object> rpcFactory = new ConcurrentKafkaListenerContainerFactory<>();
    rpcFactory.setConsumerFactory(cf);
    rpcFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
    rpcFactory.setRecordMessageConverter(new StringJsonMessageConverter());
    rpcFactory.setBatchListener(false); // Ensure it's not a batch listener
    
    // Use a template that waits for replies from topic configured at send time (via headers)
    ReplyingKafkaTemplate<String, Object, Object> template = new ReplyingKafkaTemplate<>(pf, rpcFactory.createContainer("user.rpc.reply"));
    template.setMessageConverter(new StringJsonMessageConverter());
    return template;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> rpcListenerFactory(
      ConsumerFactory<String, Object> cf) {
    ConcurrentKafkaListenerContainerFactory<String, Object> f = new ConcurrentKafkaListenerContainerFactory<>();
    f.setConsumerFactory(cf);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    f.setRecordMessageConverter(new StringJsonMessageConverter());
    f.setBatchListener(false); // Ensure it's not a batch listener
    return f;
  }
}


