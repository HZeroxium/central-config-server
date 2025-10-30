package com.example.control.infrastructure.config;

import com.example.control.infrastructure.resilience.context.RequestDeadlineContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring RestClient interceptor that propagates request deadlines to downstream
 * services.
 * <p>
 * Checks RequestDeadlineContext for a deadline and adds X-Request-Deadline
 * header
 * when a deadline exists. This enables coordinated timeouts across service call
 * chains.
 * </p>
 * <p>
 * Only adds header when deadline exists (as per requirement - no default
 * timeout
 * propagation).
 * </p>
 */
@Slf4j
@Component
public class DeadlinePropagationClientRequestFilter implements ClientHttpRequestInterceptor {

  private static final String DEADLINE_HEADER = "X-Request-Deadline";

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request,
      byte[] body,
      ClientHttpRequestExecution execution) throws IOException {

    RequestDeadlineContext.getDeadline().ifPresent(deadline -> {
      request.getHeaders().add(DEADLINE_HEADER, deadline.toString());
      log.debug("Propagated deadline header {}: {}", DEADLINE_HEADER, deadline);
    });

    return execution.execute(request, body);
  }
}
