package com.example.control.infrastructure.config.misc;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration properties for ConfigProxyService behavior.
 * <p>
 * Supports mock mode for testing and rate limit protection.
 * <p>
 * When mock mode is enabled, services not in the whitelist will receive
 * mock config hashes instead of fetching from Config Server (which calls GitHub).
 * This helps avoid GitHub rate limits during testing and load testing.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "config.proxy")
public class ConfigProxyProperties {

    /**
     * Enable mock mode: return mock config hashes instead of calling Config Server.
     * <p>
     * Useful for:
     * <ul>
     * <li>Testing without hitting GitHub rate limits</li>
     * <li>Development environments</li>
     * <li>Load testing</li>
     * </ul>
     * <p>
     * When enabled, services in {@link #whitelistServices} will still fetch
     * real config from Config Server.
     */
    private boolean mockModeEnabled = false;

    /**
     * Mock strategy: DETERMINISTIC, RANDOM, or STATIC.
     * <ul>
     * <li>DETERMINISTIC: Same hash for same service:env (stable for testing)</li>
     * <li>RANDOM: Different hash each time (test drift detection)</li>
     * <li>STATIC: Fixed hash value (test steady state)</li>
     * </ul>
     */
    @NotNull
    private MockStrategy mockStrategy = MockStrategy.DETERMINISTIC;

    /**
     * Whitelist of service names that always fetch real config from Config Server,
     * even when mock mode is enabled.
     * <p>
     * Example: ["sample-service", "payment-service"]
     * <p>
     * Services in this list will bypass mock mode and always call Config Server.
     */
    private Set<String> whitelistServices = new HashSet<>();

    /**
     * Static hash value used when {@link #mockStrategy} is STATIC.
     */
    private String staticMockHash = "mock-hash-static-12345";

    /**
     * Whether to log when mock hash is returned (for observability).
     * <p>
     * When enabled, logs at DEBUG level when a mock hash is generated.
     */
    private boolean logMockUsage = true;

    /**
     * Mock strategy enumeration.
     */
    public enum MockStrategy {
        /**
         * Generate stable hash from serviceName + profile.
         * Same service:env combination always returns the same hash.
         */
        DETERMINISTIC,

        /**
         * Generate random hash each time (includes timestamp).
         * Useful for testing drift detection scenarios.
         */
        RANDOM,

        /**
         * Return fixed static hash value.
         * Useful for testing steady state scenarios.
         */
        STATIC
    }

    /**
     * Check if a service is whitelisted (should fetch real config).
     *
     * @param serviceName service name to check
     * @return true if service is in whitelist, false otherwise
     */
    public boolean isWhitelisted(String serviceName) {
        return whitelistServices != null && whitelistServices.contains(serviceName);
    }
}

