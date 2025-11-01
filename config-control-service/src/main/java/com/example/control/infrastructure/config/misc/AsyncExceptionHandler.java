package com.example.control.infrastructure.config.misc;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Global exception handler for uncaught async task exceptions.
 * <p>
 * This handler:
 * <ul>
 *   <li>Logs exceptions with full context (method name, parameters, stack trace)</li>
 *   <li>Emits Micrometer metrics for async failures</li>
 *   <li>Preserves MDC context if available for structured logging</li>
 *   <li>Does not rethrow exceptions (best-effort execution model)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  private final MeterRegistry meterRegistry;

  public AsyncExceptionHandler(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    String executorName = extractExecutorName(method);
    String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
    String exceptionClass = ex.getClass().getSimpleName();

    // Log exception with full context
    log.error(
        "Uncaught async exception in executor '{}', method '{}' with params: {}. Exception: {}",
        executorName,
        methodName,
        Arrays.toString(params),
        ex.getMessage(),
        ex);

    // Emit metrics
    Counter.builder("async.tasks.failed")
        .description("Number of async tasks that failed with uncaught exceptions")
        .tag("executor", executorName)
        .tag("method", methodName)
        .tag("exception", exceptionClass)
        .register(meterRegistry)
        .increment();
  }

  /**
   * Extracts executor name from method annotations or uses default.
   * <p>
   * Attempts to read @Async annotation value. Falls back to "default" if not available.
   * </p>
   *
   * @param method the method that threw the exception
   * @return executor name or "default"
   */
  private String extractExecutorName(Method method) {
    // Try to extract from @Async annotation value
    try {
      org.springframework.scheduling.annotation.Async asyncAnnotation =
          method.getAnnotation(org.springframework.scheduling.annotation.Async.class);
      if (asyncAnnotation != null) {
        // Access annotation value using reflection to avoid compilation issues
        Method valueMethod = asyncAnnotation.getClass().getMethod("value");
        Object valueResult = valueMethod.invoke(asyncAnnotation);
        if (valueResult instanceof String[] executorNames
            && executorNames.length > 0 && !executorNames[0].isEmpty()) {
          return executorNames[0];
        }
      }
    } catch (Exception e) {
      log.debug("Could not extract executor name from @Async annotation", e);
    }
    return "default";
  }
}

