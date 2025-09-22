package com.example.rest.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.rest.user.dto.CreateUserRequest;
import com.example.rest.user.dto.UpdateUserRequest;
import com.example.rest.user.port.AsyncCommandPort;

import java.util.UUID;

/**
 * Service for V2 async user operations
 * Handles command submission and operation tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserV2AsyncService {

  private final AsyncCommandPort commandPort;

  /**
   * Submit a create user command asynchronously
   * 
   * @param request Create user request
   * @return Operation ID for tracking
   */
  public String submitCreateCommand(CreateUserRequest request) {
    var operationId = UUID.randomUUID().toString();
    var correlationId = UUID.randomUUID().toString();
    log.debug("Submitting create command for operation: {} with correlation: {}", operationId, correlationId);

    try {
      commandPort.submitCreateCommand(operationId, correlationId, request);
      log.info("Create command submitted successfully for operation: {}", operationId);
      return operationId;
    } catch (Exception e) {
      log.error("Failed to submit create command for operation: {}", operationId, e);
      throw new RuntimeException("Failed to submit create command", e);
    }
  }

  /**
   * Submit an update user command asynchronously
   * 
   * @param userId  User ID to update
   * @param request Update user request
   * @return Operation ID for tracking
   */
  public String submitUpdateCommand(String userId, UpdateUserRequest request) {
    var operationId = UUID.randomUUID().toString();
    var correlationId = UUID.randomUUID().toString();
    log.debug("Submitting update command for operation: {} and user: {} with correlation: {}", operationId, userId,
        correlationId);

    try {
      commandPort.submitUpdateCommand(operationId, correlationId, userId, request);
      log.info("Update command submitted successfully for operation: {} and user: {}", operationId, userId);
      return operationId;
    } catch (Exception e) {
      log.error("Failed to submit update command for operation: {} and user: {}", operationId, userId, e);
      throw new RuntimeException("Failed to submit update command", e);
    }
  }

  /**
   * Submit a delete user command asynchronously
   * 
   * @param userId User ID to delete
   * @return Operation ID for tracking
   */
  public String submitDeleteCommand(String userId) {
    var operationId = UUID.randomUUID().toString();
    var correlationId = UUID.randomUUID().toString();
    log.debug("Submitting delete command for operation: {} and user: {} with correlation: {}", operationId, userId,
        correlationId);

    try {
      commandPort.submitDeleteCommand(operationId, correlationId, userId);
      log.info("Delete command submitted successfully for operation: {} and user: {}", operationId, userId);
      return operationId;
    } catch (Exception e) {
      log.error("Failed to submit delete command for operation: {} and user: {}", operationId, userId, e);
      throw new RuntimeException("Failed to submit delete command", e);
    }
  }
}
