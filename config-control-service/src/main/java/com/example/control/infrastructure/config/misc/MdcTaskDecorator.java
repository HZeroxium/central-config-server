package com.example.control.infrastructure.config.misc;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Task decorator for propagating MDC (Mapped Diagnostic Context) to async
 * threads.
 * <p>
 * Ensures that logging context (traceId, spanId, etc.) is available in async
 * execution threads for proper observability and distributed tracing.
 * </p>
 * <p>
 * Note: SecurityContext propagation is handled separately by Spring Security's
 * {@link org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor}.
 * </p>
 * <p>
 * This decorator:
 * <ul>
 * <li>Captures MDC context map before task execution</li>
 * <li>Sets MDC context in the async thread</li>
 * <li>Clears MDC context after execution to prevent thread leak</li>
 * </ul>
 * </p>
 */
@Slf4j
public class MdcTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    // Capture MDC context from current thread
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    return () -> {
      try {
        // Set MDC context in async thread
        if (mdcContext != null && !mdcContext.isEmpty()) {
          MDC.setContextMap(mdcContext);
        }

        // Execute the actual task
        runnable.run();

      } finally {
        // Clear MDC context to prevent thread leak
        MDC.clear();
      }
    };
  }
}
