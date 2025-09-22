package com.example.thriftserver.service;

import com.example.thriftserver.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed RPC service with resilience patterns.
 * Replaces in-memory ConcurrentHashMap with distributed Redis storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRpcService {

  private static final String PENDING_REPLIES_KEY_PREFIX = "rpc:pending:";
  private static final String CIRCUIT_BREAKER_NAME = "rpc-service";
  private static final String RETRY_NAME = "rpc-service";
  private static final String TIME_LIMITER_NAME = "rpc-service";

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final AppProperties appProperties;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final ScheduledExecutorService scheduledExecutorService;

  /**
   * Send RPC request with circuit breaker, retry, and time limiter protection.
   */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackRpcRequest")
  @Retry(name = RETRY_NAME)
  @TimeLimiter(name = TIME_LIMITER_NAME)
  public <T> CompletableFuture<T> sendRpcRequestAsync(String requestTopic, String responseTopic,
      Object request, Class<T> responseType) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        String correlationId = UUID.randomUUID().toString();
        log.debug("Sending RPC request to topic: {} with correlationId: {}", requestTopic, correlationId);

        // Store pending request in Redis with TTL
        String pendingKey = PENDING_REPLIES_KEY_PREFIX + correlationId;
        PendingRpcRequest pendingRequest = new PendingRpcRequest(correlationId, responseType.getName(),
            System.currentTimeMillis());

        storePendingRequest(pendingKey, pendingRequest);

        // Send Kafka message
        ProducerRecord<String, Object> record = new ProducerRecord<>(requestTopic, correlationId, request);
        record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, responseTopic.getBytes()));
        record.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Failed to send RPC request for correlationId: {}", correlationId, ex);
            cleanupPendingRequest(pendingKey);
          }
        });

        // Wait for response
        return waitForResponse(correlationId, responseType);
      } catch (Exception e) {
        log.error("RPC request failed for topic {}: {}", requestTopic, e.getMessage(), e);
        throw new RpcException("RPC request failed: " + e.getMessage(), e);
      }
    }, scheduledExecutorService);
  }

  /**
   * Synchronous version for backward compatibility.
   */
  public <T> T sendRpcRequest(String requestTopic, String responseTopic, Object request, Class<T> responseType) {
    try {
      return sendRpcRequestAsync(requestTopic, responseTopic, request, responseType)
          .get(appProperties.getRpcTimeoutSeconds(), TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("Synchronous RPC request failed for topic {}: {}", requestTopic, e.getMessage(), e);
      throw new RpcException("RPC request failed: " + e.getMessage(), e);
    }
  }

  /**
   * Handle incoming response from Kafka.
   */
  public void handleResponse(String correlationId, Object response) {
    String pendingKey = PENDING_REPLIES_KEY_PREFIX + correlationId;
    String responseKey = getResponseKey(correlationId);

    try {
      // Check if there's a pending request
      PendingRpcRequest pendingRequest = getPendingRequest(pendingKey);
      if (pendingRequest == null) {
        log.warn("No pending request found for correlationId: {}", correlationId);
        return;
      }

      // Store response in Redis with TTL
      storeResponse(responseKey, response);

      // Remove pending request
      cleanupPendingRequest(pendingKey);

      log.debug("Stored response for correlationId: {}", correlationId);
    } catch (Exception e) {
      log.error("Failed to handle response for correlationId: {}", correlationId, e);
    }
  }

  /**
   * Fallback method for circuit breaker.
   */
  public <T> CompletableFuture<T> fallbackRpcRequest(String requestTopic, String responseTopic,
      Object request, Class<T> responseType, Exception ex) {
    log.error("Circuit breaker fallback triggered for topic: {}", requestTopic, ex);
    return CompletableFuture.failedFuture(
        new RpcException("RPC service temporarily unavailable: " + ex.getMessage(), ex));
  }

  /**
   * Cleanup expired pending requests periodically.
   */
  @Scheduled(fixedRate = 60000) // Run every minute
  public void cleanupExpiredRequests() {
    try {
      String pattern = PENDING_REPLIES_KEY_PREFIX + "*";
      var keys = redisTemplate.keys(pattern);
      if (keys != null) {
        for (String key : keys) {
          try {
            PendingRpcRequest pendingRequest = getPendingRequest(key);
            if (pendingRequest != null && isExpired(pendingRequest)) {
              redisTemplate.delete(key);
              log.debug("Cleaned up expired pending request: {}", key);
            }
          } catch (Exception e) {
            log.warn("Failed to cleanup key: {}", key, e);
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to cleanup expired requests", e);
    }
  }

  private void storePendingRequest(String key, PendingRpcRequest request) {
    try {
      String json = objectMapper.writeValueAsString(request);
      redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(appProperties.getRpcTimeoutSeconds() + 10));
    } catch (JsonProcessingException e) {
      throw new RpcException("Failed to serialize pending request", e);
    }
  }

  private PendingRpcRequest getPendingRequest(String key) {
    try {
      Object value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return null;
      }
      return objectMapper.readValue(value.toString(), PendingRpcRequest.class);
    } catch (Exception e) {
      log.error("Failed to deserialize pending request for key: {}", key, e);
      return null;
    }
  }

  private void storeResponse(String key, Object response) {
    try {
      String json = objectMapper.writeValueAsString(response);
      redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(30)); // Short TTL for responses
    } catch (JsonProcessingException e) {
      throw new RpcException("Failed to serialize response", e);
    }
  }

  private <T> T waitForResponse(String correlationId, Class<T> responseType) {
    String responseKey = getResponseKey(correlationId);
    long startTime = System.currentTimeMillis();
    long timeoutMs = appProperties.getRpcTimeoutSeconds() * 1000L;

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      try {
        Object value = redisTemplate.opsForValue().get(responseKey);
        if (value != null) {
          redisTemplate.delete(responseKey);
          T response = objectMapper.readValue(value.toString(), responseType);
          return response;
        }
        Thread.sleep(50); // Poll every 50ms
      } catch (Exception e) {
        log.error("Error waiting for response with correlationId: {}", correlationId, e);
        throw new RpcException("Failed to wait for response", e);
      }
    }

    // Cleanup on timeout
    cleanupPendingRequest(PENDING_REPLIES_KEY_PREFIX + correlationId);
    throw new RpcException("RPC request timeout after " + appProperties.getRpcTimeoutSeconds() + " seconds");
  }

  private void cleanupPendingRequest(String key) {
    redisTemplate.delete(key);
  }

  private String getResponseKey(String correlationId) {
    return "rpc:response:" + correlationId;
  }

  private boolean isExpired(PendingRpcRequest request) {
    long ageMs = System.currentTimeMillis() - request.getCreatedAt();
    return ageMs > (appProperties.getRpcTimeoutSeconds() * 1000L);
  }

  /**
   * Data class for pending RPC requests.
   */
  public static class PendingRpcRequest {
    private String correlationId;
    private String responseType;
    private long createdAt;

    public PendingRpcRequest() {
    }

    public PendingRpcRequest(String correlationId, String responseType, long createdAt) {
      this.correlationId = correlationId;
      this.responseType = responseType;
      this.createdAt = createdAt;
    }

    // Getters and setters
    public String getCorrelationId() {
      return correlationId;
    }

    public void setCorrelationId(String correlationId) {
      this.correlationId = correlationId;
    }

    public String getResponseType() {
      return responseType;
    }

    public void setResponseType(String responseType) {
      this.responseType = responseType;
    }

    public long getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
    }
  }

  /**
   * Custom exception for RPC operations.
   */
  public static class RpcException extends RuntimeException {
    public RpcException(String message) {
      super(message);
    }

    public RpcException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
