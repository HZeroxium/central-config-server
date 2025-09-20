package com.example.watcher.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.Map;

@Configuration
public class KafkaConfig {

  @Bean
  public ProducerFactory<String, Object> watcherProducerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties();
    cfg.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  @Bean
  public KafkaTemplate<String, Object> watcherKafkaTemplate(ProducerFactory<String, Object> watcherProducerFactory) {
    KafkaTemplate<String, Object> template = new KafkaTemplate<>(watcherProducerFactory);
    template.setMessageConverter(new StringJsonMessageConverter());
    return template;
  }
}
