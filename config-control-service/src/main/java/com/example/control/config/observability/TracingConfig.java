package com.example.control.config.observability;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Simplified tracing configuration using Spring Boot auto-configuration.
 * 
 * This configuration sets up:
 * - HTTP response headers with trace ID for client correlation
 * - Relies on Spring Boot's auto-configured TracingFilter for MDC population
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TracingConfig {

  private final Tracer tracer;

  /**
   * HTTP response header filter that adds trace ID to response headers.
   * MDC population is handled automatically by Spring Boot's TracingFilter.
   */
  @Bean
  @Order(1)
  public Filter traceResponseHeaderFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestId = request.getHeader("X-Request-ID");

        try {
          filterChain.doFilter(request, response);
        } finally {
          // Add trace ID to response headers from current span
          var currentSpan = tracer.currentSpan();
          if (currentSpan != null) {
            response.setHeader("X-Trace-Id", currentSpan.context().traceId());
          }
          
          // Pass through request ID if provided
          if (requestId != null) {
            response.setHeader("X-Request-Id", requestId);
          }
        }
      }
    };
  }
}
