package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import com.vng.zing.zcm.pingconfig.metrics.PingMetrics;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka messaging implementation of the ping strategy.
 * <p>
 * This strategy sends heartbeat messages using Apache Kafka to the control service's
 * heartbeat topic. It uses serviceName as partition key to ensure ordering per service.
 * <p>
 * Features:
 * <ul>
 *   <li>Fetches Kafka configuration from config-control-service with caching</li>
 *   <li>Async fire-and-forget send with circuit breaker protection</li>
 *   <li>Metrics integration for success/failure and latency tracking</li>
 *   <li>Graceful error handling with logging</li>
 * </ul>
 * <p>
 * This implementation uses async send to avoid blocking the scheduler thread.
 * Circuit breaker provides fail-fast behavior when Kafka is unavailable.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaPingStrategy implements PingStrategy {

    private final KafkaTemplate<String, HeartbeatPayload> kafkaTemplate;
    private final KafkaConfigCache configCache;

    // Optional - only used if MeterRegistry is available
    @Autowired(required = false)
    private PingMetrics pingMetrics;

    // Optional - circuit breaker for Kafka operations
    @Autowired(required = false)
    @Qualifier("kafkaPingCircuitBreaker")
    private CircuitBreaker circuitBreaker;

    @Override
    @Observed(name = "zcm.ping.kafka.send", contextualName = "kafka-ping-send")
    public void sendHeartbeat(String endpoint, HeartbeatPayload payload) throws Exception {
        // Endpoint parameter is ignored for Kafka (not needed)
        Instant start = Instant.now();

        // Get Kafka configuration from cache
        KafkaConfig config = configCache.get();
        if (config == null || !config.isValid()) {
            throw new IllegalStateException(
                    "Cannot send Kafka heartbeat: no valid Kafka configuration available. "
                            + "Ensure config-control-service is accessible or configure zcm.sdk.ping.kafka.bootstrap-servers");
        }

        String topic = config.topic();
        String partitionKey = payload.getServiceName(); // Use serviceName as partition key for ordering

        log.debug("Sending Kafka heartbeat to topic: {}, partitionKey: {}, serviceName: {}, instanceId: {}",
                topic, partitionKey, payload.getServiceName(), payload.getInstanceId());

        // Record metrics (if available)
        String protocol = getProtocol().getValue().toUpperCase();
        if (pingMetrics != null) {
            pingMetrics.recordPingAttempt(protocol);
        }

        // Check circuit breaker state before sending (fail-fast if open)
        if (circuitBreaker != null) {
            if (circuitBreaker.getState() == State.OPEN) {
                log.warn("Kafka circuit breaker is OPEN, skipping heartbeat send for {}:{}",
                        payload.getServiceName(), payload.getInstanceId());
                if (pingMetrics != null) {
                    pingMetrics.recordPingFailure(protocol, payload.getServiceName());
                }
                // Don't throw - fire-and-forget pattern, just log and return
                return;
            }
        }

        // Send to Kafka (async operation, returns immediately)
        CompletableFuture<SendResult<String, HeartbeatPayload>> future = kafkaTemplate.send(topic, partitionKey, payload);

        // Fire-and-forget: handle completion asynchronously
        future.whenComplete((result, exception) -> {
            Duration duration = Duration.between(start, Instant.now());
            
            if (exception != null) {
                // Send failed - record in circuit breaker
                if (circuitBreaker != null) {
                    circuitBreaker.onError(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS, exception);
                }
                log.warn("Kafka heartbeat send failed for {}:{}: {}",
                        payload.getServiceName(), payload.getInstanceId(), exception.getMessage());
                if (pingMetrics != null) {
                    pingMetrics.recordPingFailure(protocol, payload.getServiceName());
                }
            } else {
                // Send succeeded - record in circuit breaker
                if (circuitBreaker != null) {
                    circuitBreaker.onSuccess(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                }
                log.debug("Kafka heartbeat sent successfully to topic: {}, partition: {}, offset: {}, duration: {}ms",
                        topic,
                        result != null ? result.getRecordMetadata().partition() : "unknown",
                        result != null ? result.getRecordMetadata().offset() : "unknown",
                        duration.toMillis());
                if (pingMetrics != null) {
                    pingMetrics.recordPingSuccess(protocol, duration, payload.getServiceName());
                }
            }
        });

        // Return immediately (fire-and-forget pattern)
        // Scheduler thread is not blocked waiting for Kafka acknowledgment
    }

    @Override
    public String getName() {
        return "Kafka";
    }

    @Override
    public PingProtocol getProtocol() {
        return PingProtocol.KAFKA;
    }
}

