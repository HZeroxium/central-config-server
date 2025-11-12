package com.example.control.infrastructure.config.security;

/**
 * Interface for validating API keys used for SDK client authentication.
 * <p>
 * This interface provides extensibility for different API key validation strategies:
 * <ul>
 *   <li>Hard-coded keys (initial implementation)</li>
 *   <li>Database-backed keys with expiration</li>
 *   <li>Per-service keys with different permissions</li>
 *   <li>Key rotation and revocation</li>
 * </ul>
 * </p>
 *
 * <p><strong>Thread-safety</strong>:
 * Implementations must be thread-safe as they may be called concurrently from multiple requests.</p>
 *
 * @since 1.0
 */
public interface ApiKeyValidator {

    /**
     * Validates an API key.
     * <p>
     * Implementations should use constant-time comparison to prevent timing attacks.
     * </p>
     *
     * @param apiKey the API key to validate (may be null or empty)
     * @return {@code true} if the API key is valid; {@code false} otherwise
     */
    boolean validate(String apiKey);

    /**
     * Checks if API key validation is enabled.
     *
     * @return {@code true} if validation is enabled; {@code false} otherwise
     */
    boolean isEnabled();
}

