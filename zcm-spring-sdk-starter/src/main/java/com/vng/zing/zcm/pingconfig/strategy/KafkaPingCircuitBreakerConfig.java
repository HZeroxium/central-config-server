package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.config.SdkProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.common.KafkaException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j circuit breaker for Kafka ping operations.
 * <p>
 * Provides circuit breaker protection to fail-fast when Kafka is unavailable,
 * preventing retry storms and reducing scheduler thread blocking.
 * <p>
 * Circuit breaker instance name: {@code kafka-ping-producer}
 * <p>
 * Configuration is loaded from {@code zcm.sdk.ping.circuit-breaker.*} properties.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "zcm.sdk.ping.circuit-breaker", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaPingCircuitBreakerConfig {

    private final SdkProperties sdkProperties;

    /**
     * Creates a circuit breaker registry with a configured instance for Kafka ping.
     * <p>
     * The circuit breaker will:
     * <ul>
     *   <li>Open when failure rate exceeds threshold (default: 50%)</li>
     *   <li>Stay open for configured duration (default: 30s)</li>
     *   <li>Transition to half-open and allow limited calls to test recovery</li>
     * </ul>
     *
     * @return CircuitBreakerRegistry with kafka-ping-producer instance configured
     */
    @Bean
    public CircuitBreakerRegistry kafkaPingCircuitBreakerRegistry() {
        SdkProperties.Ping.CircuitBreaker cbConfig = sdkProperties.getPing().getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(cbConfig.getSlidingWindowSize())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .failureRateThreshold(cbConfig.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(cbConfig.getWaitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(cbConfig.getPermittedNumberOfCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                        KafkaException.class,
                        org.apache.kafka.common.errors.TimeoutException.class,
                        java.util.concurrent.TimeoutException.class,
                        java.io.IOException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        
        // Pre-register the instance
        CircuitBreaker circuitBreaker = registry.circuitBreaker("kafka-ping-producer", config);
        
        log.info("Configured Kafka ping circuit breaker: failureRateThreshold={}%, waitDuration={}ms, slidingWindowSize={}",
                cbConfig.getFailureRateThreshold(),
                cbConfig.getWaitDurationInOpenState(),
                cbConfig.getSlidingWindowSize());

        return registry;
    }

    /**
     * Creates the circuit breaker instance for Kafka ping operations.
     *
     * @param registry the circuit breaker registry
     * @return configured CircuitBreaker instance
     */
    @Bean(name = "kafkaPingCircuitBreaker")
    public CircuitBreaker kafkaPingCircuitBreaker(CircuitBreakerRegistry kafkaPingCircuitBreakerRegistry) {
        return kafkaPingCircuitBreakerRegistry.circuitBreaker("kafka-ping-producer");
    }
}

