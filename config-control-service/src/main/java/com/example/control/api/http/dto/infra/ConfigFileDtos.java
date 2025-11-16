package com.example.control.api.http.dto.infra;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTOs for config file generation API operations.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
public class ConfigFileDtos {

    /**
     * Request to generate config files for load test services.
     */
    @Data
    @Builder
    @Schema(name = "GenerateLoadTestRequest", description = "Request to generate config files for load test services")
    public static class GenerateLoadTestRequest {
        @Min(value = 1, message = "Number of services must be at least 1")
        @Max(value = 10000, message = "Number of services must not exceed 10000")
        @Schema(description = "Number of services to generate", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
        private int numServices;

        @Min(value = 1, message = "Instances per service must be at least 1")
        @Max(value = 100, message = "Instances per service must not exceed 100")
        @Schema(description = "Number of instances per service (for variation)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        private int instancesPerService;

        public GenerateLoadTestRequest(int numServices, int instancesPerService) {
            this.numServices = numServices;
            this.instancesPerService = instancesPerService;
        }

        public GenerateLoadTestRequest() {
        }
    }

    /**
     * Response for generating config files for a single service.
     */
    @Data
    @Builder
    @Schema(name = "GenerateResponse", description = "Response for config file generation for a single service")
    public static class GenerateResponse {
        @Schema(description = "Service identifier", example = "payment-service")
        private String serviceId;

        @Schema(description = "Number of files generated", example = "4")
        private int filesGenerated;

        @Schema(description = "Number of files skipped (already existed)", example = "0")
        private int filesSkipped;

        @Schema(description = "Error message if generation failed", example = "null")
        private String error;
    }

    /**
     * Generation result for a single service.
     */
    @Data
    @Builder
    @Schema(name = "GenerationResult", description = "Result of config file generation for a single service")
    public static class GenerationResult {
        @Schema(description = "Service identifier", example = "payment-service")
        private String serviceId;

        @Schema(description = "Number of files generated", example = "4")
        private int filesGenerated;

        @Schema(description = "Number of files skipped (already existed)", example = "0")
        private int filesSkipped;

        @Schema(description = "Error message if generation failed", example = "null")
        private String error;
    }

    /**
     * Response for generating config files for all services.
     */
    @Data
    @Builder
    @Schema(name = "GenerateAllResponse", description = "Response for config file generation for all services")
    public static class GenerateAllResponse {
        @Schema(description = "Total number of services processed", example = "100")
        private int totalServices;

        @Schema(description = "Number of services with successful generation", example = "98")
        private int successCount;

        @Schema(description = "Number of services with failed generation", example = "2")
        private int failureCount;

        @Schema(description = "Total number of files generated", example = "392")
        private int totalFilesGenerated;

        @Schema(description = "Detailed results for each service")
        private List<GenerationResult> results;
    }

    /**
     * Response for generating config files for load test services.
     */
    @Data
    @Builder
    @Schema(name = "GenerateLoadTestResponse", description = "Response for config file generation for load test services")
    public static class GenerateLoadTestResponse {
        @Schema(description = "Total number of services generated", example = "1000")
        private int totalServices;

        @Schema(description = "Number of services with successful generation", example = "1000")
        private int successCount;

        @Schema(description = "Number of services with failed generation", example = "0")
        private int failureCount;

        @Schema(description = "Total number of files generated", example = "4000")
        private int totalFilesGenerated;
    }
}

