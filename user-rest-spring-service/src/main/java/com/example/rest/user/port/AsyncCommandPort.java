package com.example.rest.user.port;

import com.example.rest.user.dto.CreateUserRequest;
import com.example.rest.user.dto.UpdateUserRequest;

/**
 * Port for async command operations (V2 APIs)
 * Handles command submission to Kafka
 */
public interface AsyncCommandPort {

  /**
   * Submit a create user command asynchronously
   * 
   * @param operationId   Unique operation identifier
   * @param correlationId Correlation ID for tracing
   * @param request       Create user request
   */
  void submitCreateCommand(String operationId, String correlationId, CreateUserRequest request);

  /**
   * Submit an update user command asynchronously
   * 
   * @param operationId   Unique operation identifier
   * @param correlationId Correlation ID for tracing
   * @param userId        User ID to update
   * @param request       Update user request
   */
  void submitUpdateCommand(String operationId, String correlationId, String userId, UpdateUserRequest request);

  /**
   * Submit a delete user command asynchronously
   * 
   * @param operationId   Unique operation identifier
   * @param correlationId Correlation ID for tracing
   * @param userId        User ID to delete
   */
  void submitDeleteCommand(String operationId, String correlationId, String userId);
}
