package com.example.control.api.http.dto.infra;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTOs for GitHub config file operations.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
public class ConfigGitDtos {

    /**
     * Response containing config file content and metadata.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Config file content response")
    public static class ConfigFileResponse {
        @Schema(description = "File content (YAML)", example = "server:\n  port: 8080")
        private String content;

        @Schema(description = "File SHA for optimistic locking", example = "abc123def456")
        private String sha;

        @Schema(description = "File path in repository", example = "sample-service/application-dev.yml")
        private String path;

        @Schema(description = "Last modified timestamp")
        private Instant lastModified;
    }

    /**
     * Request to update a config file.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to update config file")
    public static class UpdateConfigFileRequest {
        @NotBlank(message = "Content is required")
        @Schema(description = "File content (YAML)", requiredMode = Schema.RequiredMode.REQUIRED, example = "server:\n  port: 8080")
        private String content;

        @Schema(description = "Optional custom commit message", example = "Fix database connection timeout")
        private String customMessage;

        @Schema(description = "Expected SHA for optimistic locking (optional). If provided and doesn't match current SHA, update will fail with conflict.", example = "abc123def456")
        private String expectedSha;
    }

    /**
     * Response containing commit information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Commit information response")
    public static class CommitResponse {
        @Schema(description = "Commit SHA", example = "abc123def456")
        private String sha;

        @Schema(description = "Commit message", example = "Update config for sample-service (dev) by john.doe")
        private String message;

        @Schema(description = "Author name", example = "John Doe")
        private String author;

        @Schema(description = "Commit timestamp")
        private Instant timestamp;

        @Schema(description = "Commit URL", example = "https://github.com/owner/repo/commit/abc123")
        private String url;
    }
}

