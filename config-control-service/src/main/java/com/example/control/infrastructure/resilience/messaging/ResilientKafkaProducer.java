package com.example.control.infrastructure.resilience.messaging;

import com.example.control.infrastructure.resilience.ResilienceDecoratorsFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Wrapper service for protecting Kafka producer send operations with resilience
 * patterns.
 * <p>
 * Provides Circuit Breaker + Bulkhead + TimeLimiter protection for Kafka
 * producer send operations.
 * </p>
 * <p>
 * Note: No retry (KafkaTemplate has built-in retry mechanism).
 * </p>
 * <p>
 * Usage:
 * 
 * <pre>
 * CompletableFuture&lt;SendResult&lt;String, String&gt;&gt; future = resilientKafkaProducer.send(template, topic, key, value);
 * </pre>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientKafkaProducer {

  private static final String SERVICE_NAME = "kafka-producer";

  private final ResilienceDecoratorsFactory resilienceFactory;

  /**
   * Send message to Kafka topic with resilience protection.
   *
   * @param template KafkaTemplate instance
   * @param topic    Topic name
   * @param key      Message key
   * @param value    Message value
   * @param <K>      Key type
   * @param <V>      Value type
   * @return CompletableFuture for send result (never null)
   * @throws RuntimeException if send fails and fallback is triggered
   */
  public <K, V> CompletableFuture<SendResult<K, V>> send(
      KafkaTemplate<K, V> template,
      String topic,
      K key,
      V value) {
    Supplier<CompletableFuture<SendResult<K, V>>> sendOperation = () -> template.send(topic, key, value);
    
    CompletableFuture<SendResult<K, V>> result = resilienceFactory.decorateSupplierWithoutRetry(
        SERVICE_NAME,
        sendOperation,
        null) // Fail if send fails
        .get();
    
    // If fallback was triggered (circuit breaker open, bulkhead full, etc.), result will be null
    // Return a failed CompletableFuture instead of null to avoid NPE
    if (result == null) {
      log.warn("Kafka send operation failed due to resilience protection (circuit breaker/bulkhead), returning failed future");
      CompletableFuture<SendResult<K, V>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException(
          "Kafka send failed: circuit breaker open or bulkhead full"));
      return failedFuture;
    }
    
    return result;
  }

  /**
   * Send message to Kafka topic (partition) with resilience protection.
   *
   * @param template  KafkaTemplate instance
   * @param topic     Topic name
   * @param partition Partition number
   * @param key       Message key
   * @param value     Message value
   * @param <K>       Key type
   * @param <V>       Value type
   * @return CompletableFuture for send result (never null)
   * @throws RuntimeException if send fails and fallback is triggered
   */
  public <K, V> CompletableFuture<SendResult<K, V>> send(
      KafkaTemplate<K, V> template,
      String topic,
      Integer partition,
      K key,
      V value) {
    Supplier<CompletableFuture<SendResult<K, V>>> sendOperation = () -> template.send(topic, partition, key, value);
    
    CompletableFuture<SendResult<K, V>> result = resilienceFactory.decorateSupplierWithoutRetry(
        SERVICE_NAME,
        sendOperation,
        null) // Fail if send fails
        .get();
    
    if (result == null) {
      log.warn("Kafka send operation failed due to resilience protection (circuit breaker/bulkhead), returning failed future");
      CompletableFuture<SendResult<K, V>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException(
          "Kafka send failed: circuit breaker open or bulkhead full"));
      return failedFuture;
    }
    
    return result;
  }

  /**
   * Send message to Kafka topic without key with resilience protection.
   *
   * @param template KafkaTemplate instance
   * @param topic    Topic name
   * @param value    Message value
   * @param <K>      Key type
   * @param <V>      Value type
   * @return CompletableFuture for send result (never null)
   * @throws RuntimeException if send fails and fallback is triggered
   */
  public <K, V> CompletableFuture<SendResult<K, V>> send(
      KafkaTemplate<K, V> template,
      String topic,
      V value) {
    Supplier<CompletableFuture<SendResult<K, V>>> sendOperation = () -> template.send(topic, value);
    
    CompletableFuture<SendResult<K, V>> result = resilienceFactory.decorateSupplierWithoutRetry(
        SERVICE_NAME,
        sendOperation,
        null) // Fail if send fails
        .get();
    
    if (result == null) {
      log.warn("Kafka send operation failed due to resilience protection (circuit breaker/bulkhead), returning failed future");
      CompletableFuture<SendResult<K, V>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException(
          "Kafka send failed: circuit breaker open or bulkhead full"));
      return failedFuture;
    }
    
    return result;
  }
}
