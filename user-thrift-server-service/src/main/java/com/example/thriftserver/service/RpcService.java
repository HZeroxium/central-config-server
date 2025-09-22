package com.example.thriftserver.service;

import com.example.thriftserver.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated(since = "1.0.0", forRemoval = true)
public class RpcService {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final AppProperties appProperties;
  private final ConcurrentHashMap<String, CompletableFuture<Object>> pendingReplies = new ConcurrentHashMap<>();

  public <T> T sendRpcRequest(String requestTopic, String responseTopic, Object request, Class<T> responseType) {
    try {
      String correlationId = UUID.randomUUID().toString();
      log.debug("Sending RPC request to topic: {} with correlationId: {}", requestTopic, correlationId);

      ProducerRecord<String, Object> record = new ProducerRecord<>(requestTopic, correlationId, request);
      record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, responseTopic.getBytes()));
      record.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

      CompletableFuture<Object> future = new CompletableFuture<>();
      pendingReplies.put(correlationId, future);

      kafkaTemplate.send(record);

      Object response = future.get(appProperties.getRpcTimeoutSeconds(), TimeUnit.SECONDS);
      pendingReplies.remove(correlationId);

      if (responseType.isInstance(response)) {
        return responseType.cast(response);
      } else {
        throw new RuntimeException("Unexpected response type: " + response.getClass());
      }
    } catch (Exception e) {
      log.error("RPC request failed for topic {}: {}", requestTopic, e.getMessage(), e);
      throw new RuntimeException("RPC request failed: " + e.getMessage(), e);
    }
  }

  public void handleResponse(String correlationId, Object response) {
    CompletableFuture<Object> future = pendingReplies.remove(correlationId);
    if (future != null) {
      future.complete(response);
    } else {
      log.warn("No pending reply found for correlationId: {}", correlationId);
    }
  }
}
