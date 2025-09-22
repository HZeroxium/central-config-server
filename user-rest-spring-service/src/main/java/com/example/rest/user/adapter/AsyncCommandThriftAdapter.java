package com.example.rest.user.adapter;

import com.example.rest.user.dto.CreateUserRequest;
import com.example.rest.user.dto.UpdateUserRequest;
import com.example.rest.user.port.AsyncCommandPort;
import com.example.user.thrift.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

/**
 * Adapter for AsyncCommandPort that interfaces with Thrift server
 * for submitting asynchronous commands
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncCommandThriftAdapter implements AsyncCommandPort {

  private final UserService.Client userServiceClient;

  @Override
  public void submitCreateCommand(String operationId, String correlationId, CreateUserRequest request) {
    log.info("Submitting create command - operationId: {}, correlationId: {}", operationId, correlationId);

    try {
      // Ensure connection is open
      if (!userServiceClient.getInputProtocol().getTransport().isOpen()) {
        userServiceClient.getInputProtocol().getTransport().open();
      }

      // Create Thrift request
      TCreateUserRequest thriftRequest = new TCreateUserRequest();
      thriftRequest.setName(request.getName());
      thriftRequest.setPhone(request.getPhone());
      thriftRequest.setAddress(request.getAddress());
      thriftRequest.setStatus(convertToThriftStatus(request.getStatus()));
      thriftRequest.setRole(convertToThriftRole(request.getRole()));

      // Create async request wrapper
      TAsyncCreateUserRequest asyncRequest = new TAsyncCreateUserRequest();
      asyncRequest.setOperationId(operationId);
      asyncRequest.setCorrelationId(correlationId);
      asyncRequest.setCreateRequest(thriftRequest);

      // Submit to Thrift server
      TAsyncOperationResponse response = userServiceClient.submitCreateUserCommand(asyncRequest);

      if (response.getStatus() == 0) {
        log.debug("Create command submitted successfully - operationId: {}", operationId);
      } else {
        log.error("Failed to submit create command - operationId: {}, message: {}",
            operationId, response.getMessage());
        throw new RuntimeException("Thrift server rejected create command: " + response.getMessage());
      }

    } catch (TException e) {
      log.error("Thrift error while submitting create command - operationId: {}", operationId, e);
      throw new RuntimeException("Failed to submit create command", e);
    }
  }

  @Override
  public void submitUpdateCommand(String operationId, String correlationId, String userId, UpdateUserRequest request) {
    log.info("Submitting update command - operationId: {}, correlationId: {}, userId: {}", operationId, correlationId,
        userId);

    try {
      // Ensure connection is open
      if (!userServiceClient.getInputProtocol().getTransport().isOpen()) {
        userServiceClient.getInputProtocol().getTransport().open();
      }

      // Create Thrift request
      TUpdateUserRequest thriftRequest = new TUpdateUserRequest();
      thriftRequest.setId(userId);
      thriftRequest.setName(request.getName());
      thriftRequest.setPhone(request.getPhone());
      thriftRequest.setAddress(request.getAddress());
      thriftRequest.setStatus(convertToThriftStatus(request.getStatus()));
      thriftRequest.setRole(convertToThriftRole(request.getRole()));
      thriftRequest.setVersion(request.getVersion());

      // Create async request wrapper
      TAsyncUpdateUserRequest asyncRequest = new TAsyncUpdateUserRequest();
      asyncRequest.setOperationId(operationId);
      asyncRequest.setCorrelationId(correlationId);
      asyncRequest.setUpdateRequest(thriftRequest);

      // Submit to Thrift server
      TAsyncOperationResponse response = userServiceClient.submitUpdateUserCommand(asyncRequest);

      if (response.getStatus() == 0) {
        log.debug("Update command submitted successfully - operationId: {}", operationId);
      } else {
        log.error("Failed to submit update command - operationId: {}, message: {}",
            operationId, response.getMessage());
        throw new RuntimeException("Thrift server rejected update command: " + response.getMessage());
      }

    } catch (TException e) {
      log.error("Thrift error while submitting update command - operationId: {}", operationId, e);
      throw new RuntimeException("Failed to submit update command", e);
    }
  }

  @Override
  public void submitDeleteCommand(String operationId, String correlationId, String userId) {
    log.info("Submitting delete command - operationId: {}, correlationId: {}, userId: {}", operationId, correlationId,
        userId);

    try {
      // Ensure connection is open
      if (!userServiceClient.getInputProtocol().getTransport().isOpen()) {
        userServiceClient.getInputProtocol().getTransport().open();
      }

      // Create async request wrapper
      TAsyncDeleteUserRequest asyncRequest = new TAsyncDeleteUserRequest();
      asyncRequest.setOperationId(operationId);
      asyncRequest.setCorrelationId(correlationId);
      asyncRequest.setUserId(userId);

      // Submit to Thrift server
      TAsyncOperationResponse response = userServiceClient.submitDeleteUserCommand(asyncRequest);

      if (response.getStatus() == 0) {
        log.debug("Delete command submitted successfully - operationId: {}", operationId);
      } else {
        log.error("Failed to submit delete command - operationId: {}, message: {}",
            operationId, response.getMessage());
        throw new RuntimeException("Thrift server rejected delete command: " + response.getMessage());
      }

    } catch (TException e) {
      log.error("Thrift error while submitting delete command - operationId: {}", operationId, e);
      throw new RuntimeException("Failed to submit delete command", e);
    }
  }

  private TUserStatus convertToThriftStatus(com.example.common.domain.User.UserStatus status) {
    if (status == null)
      return TUserStatus.ACTIVE;
    return switch (status) {
      case ACTIVE -> TUserStatus.ACTIVE;
      case INACTIVE -> TUserStatus.INACTIVE;
      case SUSPENDED -> TUserStatus.SUSPENDED;
    };
  }

  private TUserRole convertToThriftRole(com.example.common.domain.User.UserRole role) {
    if (role == null)
      return TUserRole.USER;
    return switch (role) {
      case ADMIN -> TUserRole.ADMIN;
      case USER -> TUserRole.USER;
      case MODERATOR -> TUserRole.MODERATOR;
      case GUEST -> TUserRole.GUEST;
    };
  }
}