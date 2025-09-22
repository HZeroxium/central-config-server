package com.example.thriftserver.service;

import com.example.kafka.thrift.TOperationStatus;
import com.example.kafka.thrift.TOperationTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for tracking asynchronous operation status using Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationTrackingService {

  private static final String OPERATION_KEY_PREFIX = "user:operation:";
  private static final long OPERATION_TTL_MINUTES = 30; // Operations expire after 30 minutes

  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * Initializes a new operation in Redis with PENDING status.
   * 
   * @param operationId   The unique ID of the operation.
   * @param correlationId The correlation ID for tracing.
   * @return The initialized TOperationTracker.
   */
  public TOperationTracker initializeOperation(String operationId, String correlationId) {
    TOperationTracker tracker = new TOperationTracker();
    tracker.setOperationId(operationId);
    tracker.setStatus(TOperationStatus.PENDING);
    tracker.setCreatedAt(Instant.now().toEpochMilli());
    tracker.setCorrelationId(correlationId);

    redisTemplate.opsForValue().set(OPERATION_KEY_PREFIX + operationId, tracker, OPERATION_TTL_MINUTES,
        TimeUnit.MINUTES);
    log.debug("Initialized operation tracker for operationId: {}", operationId);
    return tracker;
  }

  /**
   * Updates the status of an existing operation.
   * 
   * @param operationId  The unique ID of the operation.
   * @param status       The new status.
   * @param errorMessage Optional error message.
   * @param errorCode    Optional error code.
   */
  public void updateOperationStatus(String operationId, TOperationStatus status, String errorMessage,
      String errorCode) {
    String key = OPERATION_KEY_PREFIX + operationId;
    TOperationTracker tracker = (TOperationTracker) redisTemplate.opsForValue().get(key);
    if (tracker != null) {
      tracker.setStatus(status);
      tracker.setUpdatedAt(Instant.now().toEpochMilli());
      if (status == TOperationStatus.COMPLETED || status == TOperationStatus.FAILED
          || status == TOperationStatus.CANCELLED) {
        tracker.setCompletedAt(Instant.now().toEpochMilli());
      }
      if (errorMessage != null) {
        tracker.setErrorMessage(errorMessage);
      }
      if (errorCode != null) {
        tracker.setErrorCode(errorCode);
      }
      redisTemplate.opsForValue().set(key, tracker, OPERATION_TTL_MINUTES, TimeUnit.MINUTES);
      log.debug("Updated operation status for operationId: {} to {}", operationId, status);
    } else {
      log.warn("Operation tracker not found for operationId: {}", operationId);
    }
  }

  /**
   * Retrieves an operation tracker by its ID.
   * 
   * @param operationId The unique ID of the operation.
   * @return An Optional containing the TOperationTracker if found.
   */
  public Optional<TOperationTracker> getOperationTracker(String operationId) {
    String key = OPERATION_KEY_PREFIX + operationId;
    TOperationTracker tracker = (TOperationTracker) redisTemplate.opsForValue().get(key);
    if (tracker != null) {
      log.debug("Retrieved operation tracker for operationId: {}", operationId);
      return Optional.of(tracker);
    }
    log.debug("Operation tracker not found for operationId: {}", operationId);
    return Optional.empty();
  }
}
