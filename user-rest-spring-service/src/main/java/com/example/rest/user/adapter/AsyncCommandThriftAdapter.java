package com.example.rest.user.adapter;

import com.example.rest.user.dto.CreateUserRequest;
import com.example.rest.user.dto.UpdateUserRequest;
import com.example.rest.user.port.AsyncCommandPort;
import com.example.rest.user.config.ConsulThriftClientConfig.ThriftClientFactory;
import com.example.user.thrift.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

/**
 * Adapter for AsyncCommandPort that interfaces with Thrift server
 * for submitting asynchronous commands using Consul service discovery
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncCommandThriftAdapter implements AsyncCommandPort {

  private final ThriftClientFactory thriftClientFactory;

  @Override
  public void submitCreateCommand(String operationId, String correlationId, CreateUserRequest request) {
    log.info("Submitting create command - operationId: {}, correlationId: {}", operationId, correlationId);

    UserService.Client client = null;
    try {
      // Create client using Consul service discovery
      client = thriftClientFactory.createClient();

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
      TAsyncOperationResponse response = client.submitCreateUserCommand(asyncRequest);

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
    } catch (Exception e) {
      log.error("Error while submitting create command - operationId: {}", operationId, e);
      throw new RuntimeException("Failed to submit create command", e);
    } finally {
      // Always close the client connection
      if (client != null) {
        thriftClientFactory.closeClient(client);
      }
    }
  }

  @Override
  public void submitUpdateCommand(String operationId, String correlationId, String userId, UpdateUserRequest request) {
    log.info("Submitting update command - operationId: {}, correlationId: {}, userId: {}", operationId, correlationId,
        userId);

    UserService.Client client = null;
    try {
      // Create client using Consul service discovery
      client = thriftClientFactory.createClient();

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
      TAsyncOperationResponse response = client.submitUpdateUserCommand(asyncRequest);

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
    } catch (Exception e) {
      log.error("Error while submitting update command - operationId: {}", operationId, e);
      throw new RuntimeException("Failed to submit update command", e);
    } finally {
      // Always close the client connection
      if (client != null) {
        thriftClientFactory.closeClient(client);
      }
    }
  }

  @Override
  public void submitDeleteCommand(String operationId, String correlationId, String userId) {
    log.info("Submitting delete command - operationId: {}, correlationId: {}, userId: {}", operationId, correlationId,
        userId);

    UserService.Client client = null;
    try {
      // Create client using Consul service discovery
      client = thriftClientFactory.createClient();

      // Create async request wrapper
      TAsyncDeleteUserRequest asyncRequest = new TAsyncDeleteUserRequest();
      asyncRequest.setOperationId(operationId);
      asyncRequest.setCorrelationId(correlationId);
      asyncRequest.setUserId(userId);

      // Submit to Thrift server
      TAsyncOperationResponse response = client.submitDeleteUserCommand(asyncRequest);

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
    } catch (Exception e) {
      log.error("Error while submitting delete command - operationId: {}", operationId, e);
      throw new RuntimeException("Failed to submit delete command", e);
    } finally {
      // Always close the client connection
      if (client != null) {
        thriftClientFactory.closeClient(client);
      }
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