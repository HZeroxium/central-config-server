package com.example.control.domain.exception;

import com.example.control.api.http.exception.exceptions.ConfigControlException;

/**
 * Exception thrown when GitHub API rate limit is exceeded.
 * <p>
 * GitHub API has rate limits (5000 requests/hour for authenticated requests).
 * This exception is thrown when the rate limit is exceeded.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
public class GitHubRateLimitException extends ConfigControlException {

    private final int resetTimeSeconds;

    public GitHubRateLimitException(String message) {
        super("GITHUB_RATE_LIMIT", message);
        this.resetTimeSeconds = 0;
    }

    public GitHubRateLimitException(String message, int resetTimeSeconds) {
        super("GITHUB_RATE_LIMIT", message);
        this.resetTimeSeconds = resetTimeSeconds;
    }

    public GitHubRateLimitException(String message, Throwable cause) {
        super("GITHUB_RATE_LIMIT", message, cause);
        this.resetTimeSeconds = 0;
    }

    public int getResetTimeSeconds() {
        return resetTimeSeconds;
    }
}

