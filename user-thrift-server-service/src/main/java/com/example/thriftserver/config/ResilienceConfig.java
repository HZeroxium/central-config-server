package com.example.thriftserver.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Resilience4j configuration for RPC service patterns.
 * Implements circuit breaker, retry, and time limiter patterns.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResilienceConfig {

  private final AppProperties appProperties;

  @Bean
  public CircuitBreaker rpcServiceCircuitBreaker() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(10)
        .minimumNumberOfCalls(5)
        .failureRateThreshold(50.0f)
        .slowCallRateThreshold(80.0f)
        .slowCallDurationThreshold(Duration.ofSeconds(appProperties.getRpcTimeoutSeconds() / 2))
        .permittedNumberOfCallsInHalfOpenState(3)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .recordExceptions(Exception.class)
        .build();

    CircuitBreaker circuitBreaker = CircuitBreaker.of("rpc-service", config);

    // Register event listeners for monitoring
    circuitBreaker.getEventPublisher()
        .onStateTransition(event -> log.info("Circuit breaker state transition: {} -> {}",
            event.getStateTransition().getFromState(),
            event.getStateTransition().getToState()))
        .onFailureRateExceeded(event -> log.warn("Circuit breaker failure rate exceeded: {}%", event.getFailureRate()))
        .onSlowCallRateExceeded(
            event -> log.warn("Circuit breaker slow call rate exceeded: {}%", event.getSlowCallRate()));

    return circuitBreaker;
  }

  @Bean
  public Retry rpcServiceRetry() {
    RetryConfig config = RetryConfig.custom()
        .maxAttempts(3)
        .waitDuration(Duration.ofMillis(500))
        .retryOnException(throwable -> {
          // Only retry on specific transient exceptions
          return throwable instanceof java.util.concurrent.TimeoutException ||
              throwable instanceof org.springframework.kafka.KafkaException ||
              (throwable instanceof RuntimeException &&
                  throwable.getMessage().contains("connection"));
        })
        .build();

    Retry retry = Retry.of("rpc-service", config);

    // Register event listeners
    retry.getEventPublisher()
        .onRetry(event -> log.debug("Retry attempt {} for operation, last exception: {}",
            event.getNumberOfRetryAttempts(),
            event.getLastThrowable().getMessage()))
        .onError(event -> log.error("Retry failed after {} attempts",
            event.getNumberOfRetryAttempts(), event.getLastThrowable()));

    return retry;
  }

  @Bean
  public TimeLimiter rpcServiceTimeLimiter() {
    TimeLimiterConfig config = TimeLimiterConfig.custom()
        .timeoutDuration(Duration.ofSeconds(appProperties.getRpcTimeoutSeconds()))
        .cancelRunningFuture(true)
        .build();

    TimeLimiter timeLimiter = TimeLimiter.of("rpc-service", config);

    // Register event listeners
    timeLimiter.getEventPublisher()
        .onTimeout(event -> log.warn("Time limiter timeout after {}ms", config.getTimeoutDuration().toMillis()))
        .onError(event -> log.error("Time limiter error", event.getThrowable()));

    return timeLimiter;
  }

  @Bean
  public ScheduledExecutorService rpcScheduledExecutorService() {
    return Executors.newScheduledThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
        r -> {
          Thread t = new Thread(r, "rpc-service-executor");
          t.setDaemon(true);
          return t;
        });
  }
}
