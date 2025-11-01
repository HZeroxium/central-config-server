package com.example.control.infrastructure.cache;

import com.example.control.infrastructure.config.cache.CacheProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Executor for cache operations with resilience patterns (retry + circuit
 * breaker).
 * <p>
 * Wraps L2 (Redis) cache operations with:
 * <ul>
 * <li>Retry with exponential backoff for transient failures</li>
 * <li>Circuit breaker to prevent cascading failures</li>
 * <li>Graceful degradation (return null/empty on failure, continue with
 * L1-only)</li>
 * </ul>
 * <p>
 * L1 (Caffeine) operations are not wrapped as they are local and fast.
 *
 * @since 1.0.0
 */
@Slf4j
public class CacheOperationExecutor {

  private static final String CIRCUIT_BREAKER_NAME = "cache-redis";
  private static final String RETRY_NAME = "cache-redis";

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final RetryRegistry retryRegistry;
  private final CacheProperties cacheProperties;
  private final Optional<CacheMetrics> cacheMetrics;

  /**
   * Constructor with optional cache metrics to break circular dependency.
   */
  public CacheOperationExecutor(CircuitBreakerRegistry circuitBreakerRegistry,
      RetryRegistry retryRegistry,
      CacheProperties cacheProperties,
      Optional<CacheMetrics> cacheMetrics) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.retryRegistry = retryRegistry;
    this.cacheProperties = cacheProperties;
    this.cacheMetrics = cacheMetrics;
  }

  /**
   * Execute a cache operation with resilience patterns.
   * <p>
   * Applies retry and circuit breaker. On failure, logs error and returns null
   * (graceful degradation).
   *
   * @param cacheName cache name for metrics
   * @param operation the cache operation to execute
   * @param <T>       return type
   * @return result of operation, or null on failure
   */
  public <T> T execute(String cacheName, Supplier<T> operation) {
    if (!cacheProperties.getErrorHandling().isEnableRetry()) {
      // No retry, just execute directly
      return executeDirect(cacheName, operation);
    }

    try {
      CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
      Retry retry = getOrCreateRetry();

      Supplier<T> decoratedOperation = Retry.decorateSupplier(retry,
          CircuitBreaker.decorateSupplier(circuitBreaker, operation));

      return decoratedOperation.get();
    } catch (Exception e) {
      log.error("Cache operation failed for cache: {} after retries", cacheName, e);
      cacheMetrics.ifPresent(metrics -> metrics.recordError(cacheName, e.getClass().getSimpleName()));
      return null; // Graceful degradation
    }
  }

  /**
   * Execute a cache operation without retry (for non-idempotent operations).
   */
  public <T> T executeDirect(String cacheName, Supplier<T> operation) {
    try {
      CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
      return CircuitBreaker.decorateSupplier(circuitBreaker, operation).get();
    } catch (Exception e) {
      log.error("Cache operation failed for cache: {}", cacheName, e);
      cacheMetrics.ifPresent(metrics -> metrics.recordError(cacheName, e.getClass().getSimpleName()));
      return null; // Graceful degradation
    }
  }

  /**
   * Execute a void cache operation (e.g., put, evict).
   */
  public void executeVoid(String cacheName, Runnable operation) {
    execute(cacheName, () -> {
      operation.run();
      return null;
    });
  }

  /**
   * Get or create retry instance with configured parameters.
   */
  private Retry getOrCreateRetry() {
    try {
      return retryRegistry.retry(RETRY_NAME);
    } catch (Exception e) {
      // Create retry if not configured
      CacheProperties.ErrorHandlingConfig config = cacheProperties.getErrorHandling();
      IntervalFunction intervalFunction = IntervalFunction.ofExponentialBackoff(
          config.getInitialDelay(),
          config.getMultiplier());

      RetryConfig retryConfig = RetryConfig.custom()
          .maxAttempts(config.getMaxAttempts())
          .waitDuration(config.getInitialDelay())
          .intervalFunction(intervalFunction)
          .retryOnException(throwable -> {
            // Retry on transient failures
            return throwable instanceof java.net.SocketTimeoutException
                || throwable instanceof java.net.ConnectException
                || throwable instanceof java.io.IOException
                || throwable instanceof org.springframework.data.redis.RedisConnectionFailureException;
          })
          .build();

      return Retry.of(RETRY_NAME, retryConfig);
    }
  }
}
