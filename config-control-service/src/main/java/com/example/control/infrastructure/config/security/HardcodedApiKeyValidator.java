package com.example.control.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Hard-coded API key validator implementation.
 * <p>
 * This initial implementation validates against a single hard-coded API key
 * configured via properties or environment variable.
 * </p>
 * <p>
 * <strong>Security</strong>:
 * Uses constant-time string comparison to prevent timing attacks.
 * </p>
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HardcodedApiKeyValidator implements ApiKeyValidator {

    private final SecurityProperties securityProperties;
    private final Environment environment;

    /**
     * Validates an API key against the configured hard-coded key.
     * <p>
     * Uses constant-time comparison to prevent timing attacks.
     * </p>
     *
     * @param apiKey the API key to validate
     * @return {@code true} if the API key matches the configured key; {@code false} otherwise
     */
    @Override
    public boolean validate(String apiKey) {
        if (!isEnabled()) {
            return false;
        }

        if (!StringUtils.hasText(apiKey)) {
            return false;
        }

        String configuredKey = getConfiguredApiKey();
        if (!StringUtils.hasText(configuredKey)) {
            log.warn("API key validation enabled but no key configured");
            return false;
        }

        // Constant-time comparison to prevent timing attacks
        return constantTimeEquals(apiKey, configuredKey);
    }

    @Override
    public boolean isEnabled() {
        return securityProperties.getApiKey() != null
                && securityProperties.getApiKey().isEnabled();
    }

    /**
     * Gets the configured API key from properties or environment variable.
     * <p>
     * Environment variable {@code ZCM_API_KEY} takes precedence over configuration file.
     * </p>
     *
     * @return the configured API key, or null if not configured
     */
    private String getConfiguredApiKey() {
        // Environment variable takes precedence
        String envKey = environment.getProperty("ZCM_API_KEY");
        if (StringUtils.hasText(envKey)) {
            return envKey;
        }

        // Fall back to configuration property
        if (securityProperties.getApiKey() != null) {
            return securityProperties.getApiKey().getKey();
        }

        return null;
    }

    /**
     * Performs constant-time string comparison to prevent timing attacks.
     * <p>
     * This method compares two strings in constant time regardless of where
     * the first difference occurs, preventing attackers from inferring the
     * correct API key through timing analysis.
     * </p>
     *
     * @param a the first string
     * @param b the second string
     * @return {@code true} if strings are equal; {@code false} otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }
}

