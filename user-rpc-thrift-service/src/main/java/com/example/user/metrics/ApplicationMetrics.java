package com.example.user.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Custom metrics service for application-level profiling.
 * Provides business-specific metrics for monitoring and alerting.
 */
@Slf4j
@Component
public class ApplicationMetrics {

    private final MeterRegistry meterRegistry;

    // Predefined metrics without dynamic tags (common)
    private final Counter thriftServerRequests;
    private final Counter thriftServerErrors;
    private final Timer thriftServerDuration;

    private final Counter databaseOperations;
    private final Counter databaseErrors;
    private final Timer databaseDuration;

    private final Counter userOperations;
    private final Counter userOperationsErrors;
    private final Timer userOperationsDuration;

    public ApplicationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters/timers without dynamic tags
        this.thriftServerRequests = Counter.builder("thrift.server.requests")
                .description("Total number of Thrift server requests")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);

        this.thriftServerErrors = Counter.builder("thrift.server.errors")
                .description("Total number of Thrift server errors")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);

        this.thriftServerDuration = Timer.builder("thrift.server.duration")
                .description("Thrift server request duration")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);

        this.databaseOperations = Counter.builder("database.operations")
                .description("Total number of database operations")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);

        this.databaseErrors = Counter.builder("database.errors")
                .description("Total number of database errors")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);

        this.databaseDuration = Timer.builder("database.duration")
                .description("Database operation duration")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);

        this.userOperations = Counter.builder("user.operations")
                .description("Total number of user operations")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);

        this.userOperationsErrors = Counter.builder("user.operations.errors")
                .description("Total number of user operation errors")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);

        this.userOperationsDuration = Timer.builder("user.operations.duration")
                .description("User operation duration")
                .tag("service", "user-rpc-thrift-service")
                .register(meterRegistry);
    }

    // Helper methods to get or create meters with dynamic tags
    private Counter getOrCreateCounter(String name, String... tags) {
        Counter c = meterRegistry.find(name).tags(tags).counter();
        if (c == null) {
            c = Counter.builder(name)
                    .tags(tags)
                    .register(meterRegistry);
        }
        return c;
    }

    private Timer getOrCreateTimer(String name, String... tags) {
        Timer t = meterRegistry.find(name).tags(tags).timer();
        if (t == null) {
            t = Timer.builder(name)
                    .tags(tags)
                    .register(meterRegistry);
        }
        return t;
    }

    // Thrift Server Metrics Methods

    // Backward compatible signature
    public void incrementThriftServerRequests(String operation) {
        // dynamic tag “operation”
        Counter c = getOrCreateCounter("thrift.server.requests",
                "service", "user-rpc-thrift-service",
                "operation", operation);
        c.increment();
        log.debug("Thrift server request recorded: {}", operation);
    }

    public void incrementThriftServerErrors(String operation, String errorType) {
        Counter c = getOrCreateCounter("thrift.server.errors",
                "service", "user-rpc-thrift-service",
                "operation", operation,
                "error_type", errorType);
        c.increment();
        log.warn("Thrift server error recorded: {} - {}", operation, errorType);
    }

    public Timer.Sample startThriftServerTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordThriftServerDuration(Timer.Sample sample, String operation) {
        Timer t = getOrCreateTimer("thrift.server.duration",
                "service", "user-rpc-thrift-service",
                "operation", operation);
        sample.stop(t);
        log.debug("Thrift server duration recorded: {}", operation);
    }

    // Database Metrics Methods
    public void incrementDatabaseOperations(String operation, String table) {
        Counter c = getOrCreateCounter("database.operations",
                "service", "user-rpc-thrift-service",
                "operation", operation,
                "table", table);
        c.increment();
        log.debug("Database operation recorded: {} on {}", operation, table);
    }

    public void incrementDatabaseErrors(String operation, String table, String errorType) {
        Counter c = getOrCreateCounter("database.errors",
                "service", "user-rpc-thrift-service",
                "operation", operation,
                "table", table,
                "error_type", errorType);
        c.increment();
        log.warn("Database error recorded: {} on {} - {}", operation, table, errorType);
    }

    public Timer.Sample startDatabaseTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDatabaseDuration(Timer.Sample sample, String operation, String table) {
        Timer t = getOrCreateTimer("database.duration",
                "service", "user-rpc-thrift-service",
                "operation", operation,
                "table", table);
        sample.stop(t);
        log.debug("Database duration recorded: {} on {}", operation, table);
    }

    // Business Operation Metrics Methods

    public void incrementUserOperations(String operation) {
        userOperations.increment();
        log.debug("User operation recorded: {}", operation);
    }

    public void incrementUserOperationsErrors(String operation, String errorType) {
        userOperationsErrors.increment();
        log.warn("User operation error recorded: {} - {}", operation, errorType);
    }

    public Timer.Sample startUserOperationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordUserOperationDuration(Timer.Sample sample, String operation) {
        sample.stop(userOperationsDuration);
        log.debug("User operation duration recorded: {}", operation);
    }

    // Utility Methods

    public void recordCustomMetric(String name, String description, String... tags) {
        Counter c = getOrCreateCounter(name, tags);
        c.increment();
        log.debug("Custom metric recorded: {} with tags: {}", name, String.join(", ", tags));
    }

    public void recordCustomTimer(String name, String description, Duration duration, String... tags) {
        Timer t = getOrCreateTimer(name, tags);
        t.record(duration);
        log.debug("Custom timer recorded: {} with duration: {}ms", name, duration.toMillis());
    }

}
