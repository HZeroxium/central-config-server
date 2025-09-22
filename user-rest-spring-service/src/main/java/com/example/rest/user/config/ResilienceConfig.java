package com.example.rest.user.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.transport.TTransportException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for Thrift client patterns.
 * Implements circuit breaker, retry, and time limiter patterns for Thrift
 * service calls.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResilienceConfig {

  private final ThriftClientProperties thriftClientProperties;

  @Bean
  public CircuitBreaker thriftServiceCircuitBreaker() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(10)
        .minimumNumberOfCalls(5)
        .failureRateThreshold(50.0f)
        .slowCallRateThreshold(80.0f)
        .slowCallDurationThreshold(Duration.ofMillis(thriftClientProperties.getTimeout() / 2))
        .permittedNumberOfCallsInHalfOpenState(3)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .recordExceptions(TTransportException.class, RuntimeException.class)
        .build();

    CircuitBreaker circuitBreaker = CircuitBreaker.of("thrift-service", config);

    // Register event listeners for monitoring
    circuitBreaker.getEventPublisher()
        .onStateTransition(event -> log.info("Thrift circuit breaker state transition: {} -> {}",
            event.getStateTransition().getFromState(),
            event.getStateTransition().getToState()))
        .onFailureRateExceeded(
            event -> log.warn("Thrift circuit breaker failure rate exceeded: {}%", event.getFailureRate()))
        .onSlowCallRateExceeded(
            event -> log.warn("Thrift circuit breaker slow call rate exceeded: {}%", event.getSlowCallRate()));

    return circuitBreaker;
  }

  @Bean
  public Retry thriftServiceRetry() {
    RetryConfig config = RetryConfig.custom()
        .maxAttempts(thriftClientProperties.getRetryAttempts())
        .waitDuration(Duration.ofMillis(500))
        .retryOnException(throwable -> {
          // Only retry on specific transient exceptions
          return throwable instanceof TTransportException ||
              (throwable instanceof RuntimeException &&
                  (throwable.getMessage().contains("connection") ||
                      throwable.getMessage().contains("timeout")));
        })
        .build();

    Retry retry = Retry.of("thrift-service", config);

    // Register event listeners
    retry.getEventPublisher()
        .onRetry(event -> log.debug("Retry attempt {} for Thrift operation, last exception: {}",
            event.getNumberOfRetryAttempts(),
            event.getLastThrowable().getMessage()))
        .onError(event -> log.error("Thrift retry failed after {} attempts",
            event.getNumberOfRetryAttempts(), event.getLastThrowable()));

    return retry;
  }

  @Bean
  public TimeLimiter thriftServiceTimeLimiter() {
    TimeLimiterConfig config = TimeLimiterConfig.custom()
        .timeoutDuration(Duration.ofMillis(thriftClientProperties.getTimeout()))
        .cancelRunningFuture(true)
        .build();

    TimeLimiter timeLimiter = TimeLimiter.of("thrift-service", config);

    // Register event listeners
    timeLimiter.getEventPublisher()
        .onTimeout(event -> log.warn("Thrift time limiter timeout after {}ms", config.getTimeoutDuration().toMillis()))
        .onError(event -> log.error("Thrift time limiter error", event.getThrowable()));

    return timeLimiter;
  }
}
