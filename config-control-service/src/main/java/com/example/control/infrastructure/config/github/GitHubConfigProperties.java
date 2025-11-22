package com.example.control.infrastructure.config.github;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for GitHub integration.
 * <p>
 * Maps configuration from application.yml under the {@code github} prefix.
 * Used to configure GitHub API client for config file operations.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "github")
public class GitHubConfigProperties {

    /**
     * GitHub Personal Access Token (PAT) with repo scope.
     * Required for authentication with GitHub API.
     */
    @NotBlank(message = "GitHub token is required")
    private String token;

    /**
     * GitHub repository owner (organization or username).
     * Default: "HZeroxium"
     */
    private String owner = "HZeroxium";

    /**
     * GitHub repository name.
     * Default: "ztf-spring-cloud-config-server"
     */
    private String repo = "ztf-spring-cloud-config-server";

    /**
     * Git branch name to operate on.
     * Default: "master"
     */
    private String branch = "master";

    /**
     * Cache configuration for GitHub content.
     */
    private Cache cache = new Cache();

    /**
     * Cache configuration properties.
     */
    @Data
    public static class Cache {
        /**
         * Time-to-live for cached file content in seconds.
         * Default: 60 seconds
         */
        @Positive(message = "Cache TTL must be positive")
        private int ttlSeconds = 60;
    }
}

