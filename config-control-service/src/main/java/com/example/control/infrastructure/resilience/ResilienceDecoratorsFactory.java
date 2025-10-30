package com.example.control.infrastructure.resilience;

import com.example.control.infrastructure.resilience.context.RequestDeadlineContext;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Centralized factory for creating resilience-decorated callables.
 * <p>
 * Composes Circuit Breaker, Retry, Bulkhead, TimeLimiter, and custom
 * deadline checks into a single decorator chain. Integrates with
 * {@link RetryBudgetTracker} to enforce retry budgets.
 * </p>
 * <p>
 * Usage:
 * 
 * <pre>
 * Supplier&lt;String&gt; decoratedCall = factory.decorateSupplier(
 *     "configserver",
 *     () -> restClient.get(url),
 *     fallbackValue);
 * String result = decoratedCall.get();
 * </pre>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilienceDecoratorsFactory {

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final RetryRegistry retryRegistry;
  private final BulkheadRegistry bulkheadRegistry;
  private final TimeLimiterRegistry timeLimiterRegistry;
  private final RetryBudgetTracker retryBudgetTracker;

  /**
   * Decorate a supplier with full resilience patterns.
   *
   * @param serviceName Service name for resilience instance lookup
   * @param supplier    Original supplier
   * @param fallback    Fallback value on failure
   * @param <T>         Return type
   * @return Decorated supplier with resilience patterns
   */
  public <T> Supplier<T> decorateSupplier(
      String serviceName,
      Supplier<T> supplier,
      T fallback) {

    Function<Throwable, T> fallbackFunction = (Throwable t) -> {
      log.warn("Fallback triggered for {} due to: {}", serviceName, t.getMessage());
      return fallback;
    };
    return decorateSupplier(serviceName, supplier, fallbackFunction);
  }

  /**
   * Decorate a supplier with full resilience patterns and custom fallback
   * function.
   *
   * @param serviceName      Service name for resilience instance lookup
   * @param supplier         Original supplier
   * @param fallbackFunction Function to compute fallback from exception
   * @param <T>              Return type
   * @return Decorated supplier with resilience patterns
   */
  public <T> Supplier<T> decorateSupplier(
      String serviceName,
      Supplier<T> supplier,
      Function<Throwable, T> fallbackFunction) {

    // Pre-check deadline before any retry/circuit breaker
    Supplier<T> deadlineCheckedSupplier = () -> {
      RequestDeadlineContext.checkDeadline();
      retryBudgetTracker.recordRequest(serviceName);
      return supplier.get();
    };

    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
    Retry retry = retryRegistry.retry(serviceName);
    Bulkhead bulkhead = bulkheadRegistry.bulkhead(serviceName);
    // Note: TimeLimiter works with CompletableFuture, not Supplier/Callable.
    // Timeouts are handled by RestClient configuration (connectTimeout,
    // readTimeout).

    // Wrap retry to check budget
    RetryConfig baseRetryConfig = retry.getRetryConfig();
    @SuppressWarnings("rawtypes")
    RetryConfig.Builder budgetedRetryConfigBuilder = RetryConfig.from(baseRetryConfig)
        .retryOnException(throwable -> {
          // Original retry predicate
          boolean shouldRetry = baseRetryConfig.getExceptionPredicate().test(throwable);

          if (!shouldRetry) {
            return false;
          }

          // Check retry budget
          boolean budgetAllows = retryBudgetTracker.canRetry(serviceName);
          if (!budgetAllows) {
            log.warn("Retry suppressed for {} due to budget exceeded", serviceName);
          }
          return budgetAllows;
        });
    Retry budgetedRetry = Retry.of(serviceName + "-budgeted", budgetedRetryConfigBuilder.build());

    return Decorators.ofSupplier(deadlineCheckedSupplier)
        .withCircuitBreaker(circuitBreaker)
        .withRetry(budgetedRetry)
        .withBulkhead(bulkhead)
        .withFallback(fallbackFunction)
        .decorate();
  }

  /**
   * Decorate a callable with full resilience patterns.
   *
   * @param serviceName Service name for resilience instance lookup
   * @param callable    Original callable
   * @param fallback    Fallback value on failure
   * @param <T>         Return type
   * @return Decorated callable with resilience patterns
   */
  public <T> Callable<T> decorateCallable(
      String serviceName,
      Callable<T> callable,
      T fallback) {

    Function<Throwable, T> fallbackFunction = (Throwable t) -> {
      log.warn("Fallback triggered for {} due to: {}", serviceName, t.getMessage());
      return fallback;
    };
    return decorateCallable(serviceName, callable, fallbackFunction);
  }

  /**
   * Decorate a callable with full resilience patterns and custom fallback
   * function.
   *
   * @param serviceName      Service name for resilience instance lookup
   * @param callable         Original callable
   * @param fallbackFunction Function to compute fallback from exception
   * @param <T>              Return type
   * @return Decorated callable with resilience patterns
   */
  public <T> Callable<T> decorateCallable(
      String serviceName,
      Callable<T> callable,
      Function<Throwable, T> fallbackFunction) {

    // Pre-check deadline before any retry/circuit breaker
    Callable<T> deadlineCheckedCallable = () -> {
      RequestDeadlineContext.checkDeadline();
      retryBudgetTracker.recordRequest(serviceName);
      return callable.call();
    };

    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
    Retry retry = retryRegistry.retry(serviceName);
    Bulkhead bulkhead = bulkheadRegistry.bulkhead(serviceName);
    // Note: TimeLimiter works with CompletableFuture, not Supplier/Callable.
    // Timeouts are handled by RestClient configuration (connectTimeout,
    // readTimeout).

    // Wrap retry to check budget
    RetryConfig baseRetryConfig = retry.getRetryConfig();
    @SuppressWarnings("rawtypes")
    RetryConfig.Builder budgetedRetryConfigBuilder = RetryConfig.from(baseRetryConfig)
        .retryOnException(throwable -> {
          boolean shouldRetry = baseRetryConfig.getExceptionPredicate().test(throwable);

          if (!shouldRetry) {
            return false;
          }

          boolean budgetAllows = retryBudgetTracker.canRetry(serviceName);
          if (!budgetAllows) {
            log.warn("Retry suppressed for {} due to budget exceeded", serviceName);
          }
          return budgetAllows;
        });
    Retry budgetedRetry = Retry.of(serviceName + "-budgeted", budgetedRetryConfigBuilder.build());

    return Decorators.ofCallable(deadlineCheckedCallable)
        .withCircuitBreaker(circuitBreaker)
        .withRetry(budgetedRetry)
        .withBulkhead(bulkhead)
        .withFallback(fallbackFunction)
        .decorate();
  }

  /**
   * Decorate a supplier with just circuit breaker and bulkhead (no retry).
   * <p>
   * Use for non-idempotent operations (POST, PUT, DELETE).
   * </p>
   *
   * @param serviceName Service name
   * @param supplier    Original supplier
   * @param fallback    Fallback value
   * @param <T>         Return type
   * @return Decorated supplier without retry
   */
  public <T> Supplier<T> decorateSupplierWithoutRetry(
      String serviceName,
      Supplier<T> supplier,
      T fallback) {

    Supplier<T> deadlineCheckedSupplier = () -> {
      RequestDeadlineContext.checkDeadline();
      retryBudgetTracker.recordRequest(serviceName);
      return supplier.get();
    };

    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
    Bulkhead bulkhead = bulkheadRegistry.bulkhead(serviceName);
    // Note: TimeLimiter works with CompletableFuture, not Supplier.
    // Timeouts are handled by RestClient configuration (connectTimeout,
    // readTimeout).

    return Decorators.ofSupplier(deadlineCheckedSupplier)
        .withCircuitBreaker(circuitBreaker)
        .withBulkhead(bulkhead)
        .withFallback((Throwable t) -> {
          log.warn("Fallback triggered for {} (no retry) due to: {}", serviceName, t.getMessage());
          return fallback;
        })
        .decorate();
  }

  /**
   * Get circuit breaker for a service.
   *
   * @param serviceName Service name
   * @return Circuit breaker instance
   */
  public CircuitBreaker getCircuitBreaker(String serviceName) {
    return circuitBreakerRegistry.circuitBreaker(serviceName);
  }

  /**
   * Get retry instance for a service.
   *
   * @param serviceName Service name
   * @return Retry instance
   */
  public Retry getRetry(String serviceName) {
    return retryRegistry.retry(serviceName);
  }

  /**
   * Get bulkhead for a service.
   *
   * @param serviceName Service name
   * @return Bulkhead instance
   */
  public Bulkhead getBulkhead(String serviceName) {
    return bulkheadRegistry.bulkhead(serviceName);
  }

  /**
   * Get time limiter for a service.
   *
   * @param serviceName Service name
   * @return TimeLimiter instance
   */
  public TimeLimiter getTimeLimiter(String serviceName) {
    return timeLimiterRegistry.timeLimiter(serviceName);
  }
}
