package com.example.control.domain.exception;

import com.example.control.api.http.exception.exceptions.ConfigControlException;

/**
 * Exception thrown when a config file update conflicts with concurrent modifications.
 * <p>
 * This occurs when the expected SHA (from optimistic locking) does not match
 * the current file SHA in GitHub, indicating the file was modified by another user.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
public class ConfigConflictException extends ConfigControlException {

    private final String expectedSha;
    private final String actualSha;

    public ConfigConflictException(String message) {
        super("CONFIG_CONFLICT", message);
        this.expectedSha = null;
        this.actualSha = null;
    }

    public ConfigConflictException(String message, String expectedSha, String actualSha) {
        super("CONFIG_CONFLICT", message);
        this.expectedSha = expectedSha;
        this.actualSha = actualSha;
    }

    public ConfigConflictException(String serviceId, String profile, String expectedSha, String actualSha) {
        super("CONFIG_CONFLICT",
            String.format("Config file for service %s (profile: %s) was modified by another user. Expected SHA: %s, Actual SHA: %s",
                serviceId, profile, expectedSha, actualSha));
        this.expectedSha = expectedSha;
        this.actualSha = actualSha;
    }

    public String getExpectedSha() {
        return expectedSha;
    }

    public String getActualSha() {
        return actualSha;
    }
}

