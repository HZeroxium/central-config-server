package com.example.control.infrastructure.external.github;

import com.example.control.api.http.exception.exceptions.ExternalServiceException;
import com.example.control.domain.exception.ConfigConflictException;
import com.example.control.domain.exception.ConfigFileNotFoundException;
import com.example.control.domain.exception.GitHubRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.HttpException;

import java.io.IOException;

/**
 * Maps GitHub API exceptions to domain exceptions.
 * <p>
 * Provides centralized exception mapping for GitHub API operations,
 * converting low-level GitHub API exceptions to domain-specific exceptions
 * that can be handled appropriately by the application layer.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
public class GitHubExceptionMapper {

    private static final String SERVICE_NAME = "github";

    /**
     * Maps GitHub API exceptions to domain exceptions.
     *
     * @param path    the file path that caused the exception
     * @param e       the GitHub API exception
     * @return mapped domain exception
     */
    public static RuntimeException mapException(String path, Exception e) {
        log.debug("Mapping GitHub exception for path: {}", path, e);

        if (e instanceof GHFileNotFoundException) {
            return new ConfigFileNotFoundException(
                    String.format("File not found in GitHub repository: %s", path), e);
        }

        if (e instanceof HttpException httpException) {
            int statusCode = httpException.getResponseCode();
            String message = httpException.getMessage();

            // Rate limit (429)
            if (statusCode == 429) {
                // Try to extract reset time from headers if available
                int resetTime = extractResetTime(httpException);
                return new GitHubRateLimitException(
                        String.format("GitHub API rate limit exceeded for path: %s. %s", path, message),
                        resetTime);
            }

            // Conflict (409) - file was modified
            if (statusCode == 409) {
                return new ConfigConflictException(
                        String.format("File conflict for path: %s. File was modified by another user.", path));
            }

            // Not Found (404)
            if (statusCode == 404) {
                return new ConfigFileNotFoundException(
                        String.format("File not found in GitHub repository: %s", path), e);
            }

            // Other HTTP errors
            return new ExternalServiceException(SERVICE_NAME,
                    String.format("GitHub API error for path %s: %s", path, message), statusCode, e);
        }

        if (e instanceof IOException) {
            // Check if it's a rate limit error in the message
            String message = e.getMessage();
            if (message != null && (message.contains("rate limit") || message.contains("403"))) {
                return new GitHubRateLimitException(
                        String.format("GitHub API rate limit exceeded for path: %s", path), e);
            }

            // Generic IO exception
            return new ExternalServiceException(SERVICE_NAME,
                    String.format("GitHub API IO error for path %s: %s", path, message), e);
        }

        // Unknown exception
        log.error("Unknown GitHub exception type: {}", e.getClass().getName(), e);
        return new ExternalServiceException(SERVICE_NAME,
                String.format("Unexpected GitHub API error for path %s: %s", path, e.getMessage()), e);
    }

    /**
     * Extracts rate limit reset time from HTTP exception headers.
     *
     * @param httpException the HTTP exception
     * @return reset time in seconds, or 0 if not available
     */
    private static int extractResetTime(HttpException httpException) {
        try {
            // GitHub API includes X-RateLimit-Reset header
            // This is a timestamp in Unix epoch seconds
            // For now, return 0 as we don't have direct access to headers in HttpException
            // Could be enhanced if needed
            return 0;
        } catch (Exception e) {
            log.debug("Could not extract reset time from HTTP exception", e);
            return 0;
        }
    }
}

