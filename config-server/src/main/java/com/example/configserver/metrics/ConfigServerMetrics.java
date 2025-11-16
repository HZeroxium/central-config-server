package com.example.configserver.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Custom metrics component for config-server operations.
 * <p>
 * Tracks config fetch operations and Git repository operations:
 * <ul>
 * <li>Config fetch count (by application/profile/label)</li>
 * <li>Config fetch latency</li>
 * <li>Git refresh operations</li>
 * <li>Git operation latency (clone, pull, fetch)</li>
 * </ul>
 * <p>
 * All metrics are exported to Prometheus and can be visualized in Grafana.
 *
 * @author Config Server Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ConfigServerMetrics {

    private final MeterRegistry meterRegistry;

    // Config fetch counters (with tags, created dynamically)
    // Counters are lightweight, so we create them on-demand

    // Git refresh counter (no dynamic tags, can be cached)
    private final Counter gitRefreshCount;

    // Git operation timer (with operation tag, created dynamically)

    /**
     * Constructor that initializes cached metrics.
     *
     * @param meterRegistry the Micrometer meter registry
     */
    public ConfigServerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize Git refresh counter (no dynamic tags)
        this.gitRefreshCount = Counter.builder(MetricsNames.Git.REFRESH_COUNT)
                .description("Total number of Git repository refresh operations")
                .register(meterRegistry);

        log.info("ConfigServerMetrics initialized successfully");
    }

    /**
     * Records a config fetch operation.
     *
     * @param application the application name
     * @param profile     the profile name (can be null)
     * @param label       the label/branch (can be null)
     * @param duration    the fetch operation duration
     */
    public void recordConfigFetch(String application, String profile, String label, Duration duration) {
        String appTag = application != null ? application : "unknown";
        String profileTag = profile != null ? profile : "default";
        String labelTag = label != null ? label : "master";

        // Counter with tags
        Counter.builder(MetricsNames.Config.FETCH_COUNT)
                .description("Total number of config fetch operations")
                .tag("application", appTag)
                .tag("profile", profileTag)
                .tag("label", labelTag)
                .register(meterRegistry)
                .increment();

        // Timer with tags - record duration
        Timer.builder(MetricsNames.Config.FETCH_TIME)
                .description("Time taken to fetch configuration")
                .tag("application", appTag)
                .tag("profile", profileTag)
                .tag("label", labelTag)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(duration);
    }

    /**
     * Records a Git refresh operation.
     */
    public void recordGitRefresh() {
        gitRefreshCount.increment();
    }

    /**
     * Records a Git operation (clone, pull, fetch).
     *
     * @param operation the operation type (clone, pull, fetch)
     * @param duration  the operation duration
     */
    public void recordGitOperation(String operation, Duration duration) {
        Timer.builder(MetricsNames.Git.OPERATION_TIME)
                .description("Time taken for Git operations")
                .tag("operation", operation != null ? operation : "unknown")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(duration);
    }
}

