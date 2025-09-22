package com.example.thriftserver.service;

import com.example.kafka.thrift.*;
import com.example.kafka.util.ThriftKafkaMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

/**
 * Service for consuming async events and updating operation status
 * Handles completion and failure events for async operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEventConsumerService {

  private final OperationTrackingService operationTrackingService;

  @KafkaListener(topics = "user.events", groupId = "user-thrift-server-events", containerFactory = "kafkaListenerContainerFactory")
  public void handleUserEvent(ConsumerRecord<String, byte[]> record) {
    String correlationId = "unknown";
    var correlationHeader = record.headers().lastHeader(KafkaHeaders.CORRELATION_ID);
    if (correlationHeader != null) {
      correlationId = new String(correlationHeader.value());
    }
    TUserEvent event = ThriftKafkaMessageHandler.deserializeMessage(record, TUserEvent.class);
    log.info("Received event - Type: {}, Operation ID: {}, Event ID: {} with correlationId: {}",
        event.getEventType(), event.getOperationId(), event.getEventId(), correlationId);

    try {
      switch (event.getEventType()) {
        case USER_CREATED:
          handleUserCreatedEvent(event);
          break;
        case USER_UPDATED:
          handleUserUpdatedEvent(event);
          break;
        case USER_DELETED:
          handleUserDeletedEvent(event);
          break;
        case USER_OPERATION_FAILED:
          handleOperationFailedEvent(event);
          break;
        default:
          log.warn("Unknown event type received: {}", event.getEventType());
          break;
      }
    } catch (Exception e) {
      log.error("Error processing user event for operationId: {}", event.getOperationId(), e);
      operationTrackingService.updateOperationStatus(event.getOperationId(), TOperationStatus.FAILED,
          "Error processing event: " + e.getMessage(), "EVENT_PROCESSING_ERROR");
    }
  }

  private void handleUserCreatedEvent(TUserEvent event) {
    log.info("User created event received for operationId: {}", event.getOperationId());
    operationTrackingService.updateOperationStatus(event.getOperationId(), TOperationStatus.COMPLETED, null, null);
  }

  private void handleUserUpdatedEvent(TUserEvent event) {
    log.info("User updated event received for operationId: {}", event.getOperationId());
    operationTrackingService.updateOperationStatus(event.getOperationId(), TOperationStatus.COMPLETED, null, null);
  }

  private void handleUserDeletedEvent(TUserEvent event) {
    log.info("User deleted event received for operationId: {}", event.getOperationId());
    operationTrackingService.updateOperationStatus(event.getOperationId(), TOperationStatus.COMPLETED, null, null);
  }

  private void handleOperationFailedEvent(TUserEvent event) {
    log.warn("User operation failed event received for operationId: {}. Error: {}", event.getOperationId(),
        event.getErrorMessage());
    operationTrackingService.updateOperationStatus(event.getOperationId(), TOperationStatus.FAILED,
        event.getErrorMessage(), event.getErrorCode());
  }
}
