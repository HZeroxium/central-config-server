package com.example.rest.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class ApplicationMetrics {

    private final MeterRegistry meterRegistry;

    // Pre-created Counters / Timers for common metrics without dynamic tags
    private final Counter restApiRequests;
    private final Counter restApiErrors;
    private final Timer restApiDuration;

    private final Counter thriftClientRequests;
    private final Counter thriftClientErrors;
    private final Timer thriftClientDuration;

    private final Counter userOperations;
    private final Counter userOperationsErrors;
    private final Timer userOperationsDuration;

    public ApplicationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.restApiRequests = Counter.builder("rest.api.requests")
                .description("Total number of REST API requests")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);
        this.restApiErrors = Counter.builder("rest.api.errors")
                .description("Total number of REST API errors")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);
        this.restApiDuration = Timer.builder("rest.api.duration")
                .description("REST API request duration")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);

        this.thriftClientRequests = Counter.builder("thrift.client.requests")
                .description("Total number of Thrift client requests")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);
        this.thriftClientErrors = Counter.builder("thrift.client.errors")
                .description("Total number of Thrift client errors")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);
        this.thriftClientDuration = Timer.builder("thrift.client.duration")
                .description("Thrift client request duration")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);

        this.userOperations = Counter.builder("user.operations")
                .description("Total number of user operations")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);
        this.userOperationsErrors = Counter.builder("user.operations.errors")
                .description("Total number of user operation errors")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);
        this.userOperationsDuration = Timer.builder("user.operations.duration")
                .description("User operation duration")
                .tag("service", "user-rest-spring-service")
                .register(meterRegistry);
    }

    // Helper methods to get or create counters/timers with dynamic tags
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

    // REST API Metrics Methods
    public void incrementRestApiRequests(String endpoint, String method) {
        getOrCreateCounter("rest.api.requests",
                "service", "user-rest-spring-service",
                "endpoint", endpoint,
                "method", method)
                .increment();  // default +1.0
        log.debug("REST API request recorded: {} {}", method, endpoint);
    }

    public void incrementRestApiErrors(String endpoint, String method, String errorType) {
        getOrCreateCounter("rest.api.errors",
                "service", "user-rest-spring-service",
                "endpoint", endpoint,
                "method", method,
                "error_type", errorType)
                .increment();
        log.warn("REST API error recorded: {} {} - {}", method, endpoint, errorType);
    }

    public Timer.Sample startRestApiTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordRestApiDuration(Timer.Sample sample, String endpoint, String method) {
        sample.stop(getOrCreateTimer("rest.api.duration",
                "service", "user-rest-spring-service",
                "endpoint", endpoint,
                "method", method));
        log.debug("REST API duration recorded: {} {}", method, endpoint);
    }

    // Thrift Client Metrics Methods
    public void incrementThriftClientRequests(String operation) {
        getOrCreateCounter("thrift.client.requests",
                "service", "user-rest-spring-service",
                "operation", operation)
                .increment();
        log.debug("Thrift client request recorded: {}", operation);
    }

    public void incrementThriftClientErrors(String operation, String errorType) {
        getOrCreateCounter("thrift.client.errors",
                "service", "user-rest-spring-service",
                "operation", operation,
                "error_type", errorType)
                .increment();
        log.warn("Thrift client error recorded: {} - {}", operation, errorType);
    }

    public Timer.Sample startThriftClientTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordThriftClientDuration(Timer.Sample sample, String operation) {
        sample.stop(getOrCreateTimer("thrift.client.duration",
                "service", "user-rest-spring-service",
                "operation", operation));
        log.debug("Thrift client duration recorded: {}", operation);
    }

    // Business Operation Metrics Methods
    public void incrementUserOperations(String operation) {
        getOrCreateCounter("user.operations",
                "service", "user-rest-spring-service",
                "operation", operation)
                .increment();
        log.debug("User operation recorded: {}", operation);
    }

    public void incrementUserOperationsErrors(String operation, String errorType) {
        getOrCreateCounter("user.operations.errors",
                "service", "user-rest-spring-service",
                "operation", operation,
                "error_type", errorType)
                .increment();
        log.warn("User operation error recorded: {} - {}", operation, errorType);
    }

    public Timer.Sample startUserOperationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordUserOperationDuration(Timer.Sample sample, String operation) {
        sample.stop(getOrCreateTimer("user.operations.duration",
                "service", "user-rest-spring-service",
                "operation", operation));
        log.debug("User operation duration recorded: {}", operation);
    }

    // Utility Methods
    public void recordCustomMetric(String name, String description, String... tags) {
        getOrCreateCounter(name, tags)
                .increment();
        log.debug("Custom metric recorded: {} with tags: {}", name, String.join(", ", tags));
    }

    public void recordCustomTimer(String name, String description, Duration duration, String... tags) {
        getOrCreateTimer(name, tags)
                .record(duration);
        log.debug("Custom timer recorded: {} with duration: {}ms", name, duration.toMillis());
    }
}
