package com.example.rest.user.service;

import com.example.rest.user.dto.OperationStatusResponse;
import com.example.user.thrift.UserService;
import com.example.user.thrift.TOperationStatusRequest;
import com.example.user.thrift.TOperationStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Service for tracking async operation status
 * Interfaces with Thrift server for operation status queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationTrackingService {

  private final UserService.Client userServiceClient;

  /**
   * Get operation status by operation ID
   * 
   * @param operationId Operation ID to query
   * @return Operation status response if found
   */
  public Optional<OperationStatusResponse> getOperationStatus(String operationId) {
    log.debug("Getting status for operation: {}", operationId);

    try {
      // Call thrift server to get actual operation status
      TOperationStatusRequest request = new TOperationStatusRequest();
      request.setOperationId(operationId);
      
      TOperationStatusResponse thriftResponse = userServiceClient.getOperationStatus(request);
      
      if (thriftResponse.getStatus() != 0) {
        log.warn("Thrift service returned error for operation {}: {}", operationId, thriftResponse.getMessage());
        return Optional.empty();
      }
      
      // Convert to REST response
      var responseBuilder = OperationStatusResponse.builder()
          .operationId(thriftResponse.getOperationId())
          .status(thriftResponse.getOperationStatus())
          .correlationId("trace-" + operationId.substring(0, 8));
      
      if (thriftResponse.isSetResult()) {
        responseBuilder.result(thriftResponse.getResult());
      }
      if (thriftResponse.isSetErrorMessage()) {
        responseBuilder.errorMessage(thriftResponse.getErrorMessage());
      }
      if (thriftResponse.isSetErrorCode()) {
        responseBuilder.errorCode(thriftResponse.getErrorCode());
      }
      if (thriftResponse.isSetCreatedAt()) {
        responseBuilder.createdAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(thriftResponse.getCreatedAt()), ZoneOffset.UTC));
      }
      if (thriftResponse.isSetUpdatedAt()) {
        responseBuilder.updatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(thriftResponse.getUpdatedAt()), ZoneOffset.UTC));
      }
      if (thriftResponse.isSetCompletedAt()) {
        responseBuilder.completedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(thriftResponse.getCompletedAt()), ZoneOffset.UTC));
      }
      
      // Set status description and progress based on status
      String status = thriftResponse.getOperationStatus();
      switch (status) {
        case "PENDING" -> {
          responseBuilder.statusDescription("Operation is queued for processing");
          responseBuilder.progressPercentage(10);
        }
        case "IN_PROGRESS" -> {
          responseBuilder.statusDescription("Operation is being processed");
          responseBuilder.progressPercentage(50);
        }
        case "COMPLETED" -> {
          responseBuilder.statusDescription("Operation completed successfully");
          responseBuilder.progressPercentage(100);
        }
        case "FAILED" -> {
          responseBuilder.statusDescription("Operation failed");
          responseBuilder.progressPercentage(0);
        }
        case "CANCELLED" -> {
          responseBuilder.statusDescription("Operation was cancelled");
          responseBuilder.progressPercentage(0);
        }
        default -> {
          responseBuilder.statusDescription("Unknown status");
          responseBuilder.progressPercentage(0);
        }
      }
      
      var response = responseBuilder.build();
      log.debug("Operation status retrieved for: {} - Status: {}", operationId, status);
      return Optional.of(response);
      
    } catch (Exception e) {
      log.error("Failed to get operation status for: {}", operationId, e);
      return Optional.empty();
    }
  }
}
