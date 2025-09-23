package com.example.thriftserver.service;

import com.example.kafka.thrift.*;
import com.example.thriftserver.config.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Service for publishing async commands to Kafka
 * Handles V2 async operation command publishing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCommandPublishingService {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaTopicsProperties topicsProperties;

  /**
   * Publish a create user command
   */
  public void publishCreateUserCommand(String operationId, TUserCreateRequest request, String correlationId) {
    log.info("Publishing CREATE command - Operation ID: {}", operationId);

    try {
      TUserCommand command = new TUserCommand();
      command.setOperationId(operationId);
      command.setCommandType(TCommandType.CREATE_USER);
      command.setCorrelationId(correlationId);
      command.setTimestamp(System.currentTimeMillis());
      command.setCreateRequest(request);
      command.setRequestedBy("system"); // TODO: Get from security context

      publishCommand(command);
      log.info("CREATE command published successfully for operation: {}", operationId);

    } catch (Exception e) {
      log.error("Failed to publish CREATE command for operation: {}", operationId, e);
      throw new RuntimeException("Failed to publish CREATE command", e);
    }
  }

  /**
   * Publish an update user command
   */
  public void publishUpdateUserCommand(String operationId, TUserUpdateRequest request, String correlationId) {
    log.info("Publishing UPDATE command - Operation ID: {}", operationId);

    try {
      TUserCommand command = new TUserCommand();
      command.setOperationId(operationId);
      command.setCommandType(TCommandType.UPDATE_USER);
      command.setCorrelationId(correlationId);
      command.setTimestamp(System.currentTimeMillis());
      command.setUserId(request.getId());
      command.setUpdateRequest(request);
      command.setRequestedBy("system"); // TODO: Get from security context

      publishCommand(command);
      log.info("UPDATE command published successfully for operation: {}", operationId);

    } catch (Exception e) {
      log.error("Failed to publish UPDATE command for operation: {}", operationId, e);
      throw new RuntimeException("Failed to publish UPDATE command", e);
    }
  }

  /**
   * Publish a delete user command
   */
  public void publishDeleteUserCommand(String operationId, String userId, String correlationId) {
    log.info("Publishing DELETE command - Operation ID: {}, User ID: {}", operationId, userId);

    try {
      TUserDeleteRequest deleteRequest = new TUserDeleteRequest();
      deleteRequest.setId(userId);

      TUserCommand command = new TUserCommand();
      command.setOperationId(operationId);
      command.setCommandType(TCommandType.DELETE_USER);
      command.setCorrelationId(correlationId);
      command.setTimestamp(System.currentTimeMillis());
      command.setUserId(userId);
      command.setDeleteRequest(deleteRequest);
      command.setRequestedBy("system"); // TODO: Get from security context

      publishCommand(command);
      log.info("DELETE command published successfully for operation: {}", operationId);

    } catch (Exception e) {
      log.error("Failed to publish DELETE command for operation: {}", operationId, e);
      throw new RuntimeException("Failed to publish DELETE command", e);
    }
  }

  /**
   * Publish command to Kafka
   */
  private void publishCommand(TUserCommand command) {
    Message<TUserCommand> message = MessageBuilder
        .withPayload(command)
        .setHeader(KafkaHeaders.TOPIC, topicsProperties.getUserCommands())
        .setHeader(KafkaHeaders.KEY, command.getOperationId())
        .setHeader(KafkaHeaders.CORRELATION_ID, command.getCorrelationId().getBytes())
        .build();

    kafkaTemplate.send(message);
    log.debug("Command published to topic: {}", topicsProperties.getUserCommands());
  }
}
