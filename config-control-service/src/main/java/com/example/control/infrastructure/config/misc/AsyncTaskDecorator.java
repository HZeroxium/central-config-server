package com.example.control.infrastructure.config.misc;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * Task decorator for propagating MDC and SecurityContext to async threads.
 * <p>
 * Ensures that logging context (traceId, spanId) and security context
 * are available in async execution threads for proper observability and
 * audit trails.
 * </p>
 * <p>
 * This decorator:
 * <ul>
 *   <li>Captures MDC context map before task execution</li>
 *   <li>Captures SecurityContext from current thread</li>
 *   <li>Sets both contexts in the async thread</li>
 *   <li>Clears contexts after execution to prevent thread leak</li>
 * </ul>
 * </p>
 */
@Slf4j
public class AsyncTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    // Capture contexts from current thread
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();
    SecurityContext securityContext = SecurityContextHolder.getContext();

    return () -> {
      try {
        // Set MDC context in async thread
        if (mdcContext != null && !mdcContext.isEmpty()) {
          MDC.setContextMap(mdcContext);
        }

        // Set SecurityContext in async thread
        if (securityContext != null) {
          SecurityContextHolder.setContext(securityContext);
        }

        // Execute the actual task
        runnable.run();

      } finally {
        // Clear contexts to prevent thread leak
        MDC.clear();
        SecurityContextHolder.clearContext();
      }
    };
  }
}

