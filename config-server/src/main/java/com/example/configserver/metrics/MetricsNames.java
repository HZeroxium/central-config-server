package com.example.configserver.metrics;

/**
 * Centralized metric name constants for config-server.
 * <p>
 * This class eliminates magic strings and ensures consistent metric naming
 * across the application.
 * All metric names follow the pattern: {@code config_server.<operation>}.
 * </p>
 * <p>
 * <b>Naming conventions:</b>
 * <ul>
 * <li>Use dot notation (e.g., {@code config_server.config.fetch.count})</li>
 * <li>Use lowercase with underscores for readability</li>
 * <li>Keep names stable to avoid breaking Prometheus queries</li>
 * <li>Group by module/domain (config, git, etc.)</li>
 * </ul>
 * </p>
 *
 * @author Config Server Team
 * @since 1.0.0
 */
public final class MetricsNames {

    private MetricsNames() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Config fetch operation metrics.
     */
    public static final class Config {
        public static final String PREFIX = "config_server.config";

        /**
         * Counter: Total number of config fetch operations.
         * Tags: application, profile, label
         */
        public static final String FETCH_COUNT = PREFIX + ".fetch.count";

        /**
         * Timer: Time taken to fetch configuration.
         * Tags: application, profile, label
         */
        public static final String FETCH_TIME = PREFIX + ".fetch.time";

        private Config() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    /**
     * Git repository operation metrics.
     */
    public static final class Git {
        public static final String PREFIX = "config_server.git";

        /**
         * Counter: Total number of Git refresh operations.
         * Tags: none
         */
        public static final String REFRESH_COUNT = PREFIX + ".refresh.count";

        /**
         * Timer: Time taken for Git operations (clone, pull, fetch).
         * Tags: operation (clone|pull|fetch)
         */
        public static final String OPERATION_TIME = PREFIX + ".operation.time";

        private Git() {
            throw new UnsupportedOperationException("Utility class");
        }
    }
}

