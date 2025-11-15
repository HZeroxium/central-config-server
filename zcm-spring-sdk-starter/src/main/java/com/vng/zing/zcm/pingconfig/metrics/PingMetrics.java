package com.vng.zing.zcm.pingconfig.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Metrics component for tracking ping operations across all protocols.
 * <p>
 * Provides Micrometer metrics for monitoring heartbeat ping operations,
 * including success/failure counts, latency, and Kafka config fetch operations.
 * <p>
 * All metrics are exported to Prometheus and can be visualized in Grafana.
 */
@Slf4j
@Component
@ConditionalOnBean(MeterRegistry.class)
public class PingMetrics {

    private final MeterRegistry meterRegistry;

    // Kafka config fetch counters (no dynamic tags, can be cached)
    private final Counter kafkaConfigFetch;
    private final Counter kafkaConfigFetchFailure;

    /**
     * Creates a new PingMetrics component.
     *
     * @param meterRegistry Micrometer meter registry
     */
    public PingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize Kafka config fetch counters (no dynamic tags, can be cached)
        this.kafkaConfigFetch = Counter.builder("zcm.ping.kafka.config.fetch")
                .description("Total number of Kafka config fetch attempts from config-control-service")
                .tag("component", "kafka-config")
                .register(meterRegistry);

        this.kafkaConfigFetchFailure = Counter.builder("zcm.ping.kafka.config.fetch.failure")
                .description("Total number of failed Kafka config fetch attempts")
                .tag("component", "kafka-config")
                .register(meterRegistry);

        log.info("PingMetrics initialized successfully");
    }

    /**
     * Records a ping attempt.
     *
     * @param protocol the ping protocol (HTTP, THRIFT, GRPC, KAFKA)
     */
    public void recordPingAttempt(String protocol) {
        Counter.builder("zcm.ping.send.total")
                .description("Total number of ping attempts across all protocols")
                .tags(Tags.of("component", "ping", "protocol", protocol))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records a successful ping.
     *
     * @param protocol  the ping protocol
     * @param duration  the ping operation duration
     * @param serviceName the service name (optional, for additional tagging)
     */
    public void recordPingSuccess(String protocol, Duration duration, String serviceName) {
        Counter.builder("zcm.ping.send.success")
                .description("Total number of successful pings")
                .tags(Tags.of("component", "ping", "protocol", protocol))
                .register(meterRegistry)
                .increment();

        Timer.builder("zcm.ping.send.latency")
                .description("Ping operation latency by protocol")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .tags(Tags.of("component", "ping", "protocol", protocol, "service", serviceName != null ? serviceName : "unknown"))
                .register(meterRegistry)
                .record(duration);
    }

    /**
     * Records a failed ping.
     *
     * @param protocol  the ping protocol
     * @param serviceName the service name (optional, for additional tagging)
     */
    public void recordPingFailure(String protocol, String serviceName) {
        Counter.builder("zcm.ping.send.failure")
                .description("Total number of failed pings")
                .tags(Tags.of("component", "ping", "protocol", protocol))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records a Kafka config fetch attempt.
     */
    public void recordKafkaConfigFetch() {
        kafkaConfigFetch.increment();
    }

    /**
     * Records a failed Kafka config fetch.
     */
    public void recordKafkaConfigFetchFailure() {
        kafkaConfigFetchFailure.increment();
    }

    /**
     * Creates a timer sample for measuring ping latency.
     *
     * @return a timer sample that can be stopped to record duration
     */
    public Timer.Sample startPingTimer() {
        return Timer.start(meterRegistry);
    }
}

