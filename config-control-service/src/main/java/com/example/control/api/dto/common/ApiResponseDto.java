package com.example.control.api.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Standard API response DTOs for consistent API responses across all endpoints.
 * <p>
 * Provides generic response wrappers with status, message, data, and metadata
 * for successful operations and error handling.
 * </p>
 */
public class ApiResponseDto {

    private ApiResponseDto() {
        throw new UnsupportedOperationException("Utility class");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Standard API response wrapper with status, message, and data")
    public static class ApiResponse<T> {
        @Schema(description = "Response status", example = "success", allowableValues = {"success", "error"})
        private String status;

        @Schema(description = "Human-readable message", example = "Operation completed successfully")
        private String message;

        @Schema(description = "Response data payload")
        private T data;

        @Schema(description = "Timestamp in milliseconds since epoch", example = "1705312245123")
        private Long timestamp;

        @Schema(description = "Trace ID for correlation", example = "550e8400-e29b-41d4-a716-446655440000")
        private String traceId;

        @Schema(description = "List of error messages (for error responses)")
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
    @Schema(description = "Paginated response wrapper with items and pagination metadata")
    public static class PaginatedResponse<T> {
        @Schema(description = "List of items in current page")
        private List<T> items;

        @Schema(description = "Total number of items across all pages", example = "150")
        private Long totalCount;

        @Schema(description = "Current page number (0-based)", example = "0")
        private Integer page;

        @Schema(description = "Number of items per page", example = "20")
        private Integer size;

        @Schema(description = "Whether there are more pages available", example = "true")
        private Boolean hasNext;

        @Schema(description = "Whether there are previous pages available", example = "false")
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
    public static class ServiceInstancesRegistryResponse {
        private String serviceName;
        private List<ServiceInstanceSummary> instances;
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
