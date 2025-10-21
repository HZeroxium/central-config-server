package com.example.control.api.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Standard API response DTOs
 */
public class ApiResponseDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private Long timestamp;
    private String traceId;
    private List<String> errors;

    public static <T> ApiResponse<T> success(T data) {
      return ApiResponse.<T>builder()
          .status("success")
          .message("Operation completed successfully")
          .data(data)
          .timestamp(Instant.now().toEpochMilli())
          .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
      return ApiResponse.<T>builder()
          .status("success")
          .message(message)
          .data(data)
          .timestamp(Instant.now().toEpochMilli())
          .build();
    }

    public static <T> ApiResponse<T> error(String message) {
      return ApiResponse.<T>builder()
          .status("error")
          .message(message)
          .timestamp(Instant.now().toEpochMilli())
          .build();
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
      return ApiResponse.<T>builder()
          .status("error")
          .message(message)
          .errors(errors)
          .timestamp(Instant.now().toEpochMilli())
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaginatedResponse<T> {
    private List<T> items;
    private Long totalCount;
    private Integer page;
    private Integer size;
    private Boolean hasNext;
    private Boolean hasPrevious;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServiceInstanceSummary {
    private String serviceName;
    private String instanceId;
    private String host;
    private Integer port;
    private String status;
    private String scheme;
    private String uri;
    private Boolean healthy;
    private Long lastSeenAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServiceSummary {
    private String serviceName;
    private Integer instanceCount;
    private Integer healthyInstanceCount;
    private String status;
    private Long lastUpdated;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConfigSummary {
    private String application;
    private String profile;
    private String label;
    private String version;
    private Integer propertySourceCount;
    private Long lastUpdated;
  }
}
