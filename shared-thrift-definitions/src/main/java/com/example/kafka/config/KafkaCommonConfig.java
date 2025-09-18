package com.example.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.*;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;

/**
 * Shared Kafka configuration for all services
 * Provides common beans for Producer, Consumer, Transaction Manager, Error Handling
 */
@EnableKafka
@Configuration
public class KafkaCommonConfig {

    // ==================== PRODUCER CONFIGURATION ====================
    
    @Bean
    public ProducerFactory<String, String> producerFactory(KafkaProperties props) {
        Map<String, Object> pp = props.buildProducerProperties();
        DefaultKafkaProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(pp);
        pf.setTransactionIdPrefix(props.getProducer().getTransactionIdPrefix());
        return pf;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public KafkaTransactionManager<String, String> kafkaTransactionManager(ProducerFactory<String, String> pf) {
        KafkaTransactionManager<String, String> tm = new KafkaTransactionManager<>(pf);
        return tm;
    }

    // ==================== CONSUMER CONFIGURATION ====================
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory(KafkaProperties props) {
        Map<String, Object> cfg = props.buildConsumerProperties();
        cfg.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(cfg);
    }

    // ==================== CONTAINER FACTORIES ====================
    
    @Bean(name = "txFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> txFactory(
            ConsumerFactory<String, String> cf,
            KafkaTransactionManager<String, String> txm,
            KafkaTemplate<String, String> template) {

        var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
        f.setConsumerFactory(cf);
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // Error handler với DLT
        var backoff = new ExponentialBackOff();
        backoff.setInitialInterval(500);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(5000);

        var dlpr = new DeadLetterPublishingRecoverer(template, (rec, ex) -> {
            // Map *.command -> *.command.DLT
            String dlt = rec.topic().endsWith(".command") ? rec.topic() + ".DLT" : rec.topic() + ".DLT";
            return new TopicPartition(dlt, rec.partition());
        });

        var eh = new DefaultErrorHandler(dlpr, backoff);
        f.setCommonErrorHandler(eh);
        return f;
    }

    @Bean(name = "batchFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory(ConsumerFactory<String, String> cf) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
        f.setConsumerFactory(cf);
        f.setBatchListener(true);
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return f;
    }

    // ==================== MESSAGE CONVERTER ====================
    
    @Bean
    public RecordMessageConverter messageConverter() {
        return new StringJsonMessageConverter();
    }

    // ==================== TOPIC DEFINITIONS ====================
    
    private static NewTopic cmd(String name) {
        return new NewTopic(name, 3, (short) 1)
            .configs(Map.of("cleanup.policy", "delete", "retention.ms", "604800000"));
    }
    
    private static NewTopic evt(String name) {
        return new NewTopic(name, 3, (short) 1);
    }

    // Commands
    @Bean NewTopic p1Cmd() { return cmd("user.update.phase_1.command"); }
    @Bean NewTopic p2Cmd() { return cmd("user.update.phase_2.command"); }
    @Bean NewTopic p3Cmd() { return cmd("user.update.phase_3.command"); }
    @Bean NewTopic p4Cmd() { return cmd("user.update.phase_4.command"); }

    // Events
    @Bean NewTopic p1Evt() { return evt("user.update.phase_1.event"); }
    @Bean NewTopic p2Evt() { return evt("user.update.phase_2.event"); }
    @Bean NewTopic p3Evt() { return evt("user.update.phase_3.event"); }
    @Bean NewTopic p4Evt() { return evt("user.update.phase_4.event"); }

    // DLT cho command
    @Bean NewTopic p1Dlt() { return evt("user.update.phase_1.command.DLT"); }
    @Bean NewTopic p2Dlt() { return evt("user.update.phase_2.command.DLT"); }
    @Bean NewTopic p3Dlt() { return evt("user.update.phase_3.command.DLT"); }
    @Bean NewTopic p4Dlt() { return evt("user.update.phase_4.command.DLT"); }
}
