package com.example.control.infrastructure.resilience.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter for rate limiting public endpoints.
 * <p>
 * Applies rate limiting to prevent abuse of public endpoints like
 * /api/heartbeat.
 * Uses Resilience4j RateLimiter with per-IP tracking.
 * </p>
 * <p>
 * Returns HTTP 429 (Too Many Requests) when rate limit is exceeded.
 * </p>
 */
@Slf4j
@Component
@Order(2) // Run after deadline propagation filter
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

  private final RateLimiter heartbeatRateLimiter;
  private final MeterRegistry meterRegistry;

  private Counter allowedCounter;
  private Counter rejectedCounter;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String path = request.getRequestURI();

    // Only apply rate limiting to heartbeat endpoint
    if (!path.startsWith("/api/heartbeat")) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = getClientIp(request);
    String rateLimitKey = "heartbeat:" + clientIp;

    try {
      // Acquire permission from rate limiter
      heartbeatRateLimiter.acquirePermission();

      // Permission granted
      incrementAllowedCounter();
      log.trace("Rate limit OK for {} from IP: {}", path, clientIp);
      filterChain.doFilter(request, response);

    } catch (RequestNotPermitted e) {
      // Rate limit exceeded
      incrementRejectedCounter();
      log.warn("Rate limit exceeded for {} from IP: {}", path, clientIp);

      response.setStatus(429); // 429 Too Many Requests
      response.setContentType("application/json");
      response.getWriter().write(String.format(
          "{\"error\":\"Rate limit exceeded\",\"status\":429,\"message\":\"Too many requests from IP: %s\"}",
          clientIp));
    }
  }

  /**
   * Extract client IP address from request.
   * <p>
   * Checks X-Forwarded-For header first (for proxy/load balancer scenarios),
   * then falls back to remote address.
   * </p>
   */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      // X-Forwarded-For can contain multiple IPs, take the first one
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private void incrementAllowedCounter() {
    if (allowedCounter == null) {
      allowedCounter = Counter.builder("ratelimit.allowed")
          .tag("endpoint", "heartbeat")
          .description("Number of requests allowed by rate limiter")
          .register(meterRegistry);
    }
    allowedCounter.increment();
  }

  private void incrementRejectedCounter() {
    if (rejectedCounter == null) {
      rejectedCounter = Counter.builder("ratelimit.rejected")
          .tag("endpoint", "heartbeat")
          .description("Number of requests rejected by rate limiter")
          .register(meterRegistry);
    }
    rejectedCounter.increment();
  }
}
