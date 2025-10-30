package com.example.control.infrastructure.resilience.filter;

import com.example.control.infrastructure.config.resilience.ResilienceProperties;
import com.example.control.infrastructure.resilience.context.RequestDeadlineContext;
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
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Servlet filter for deadline propagation.
 * <p>
 * Extracts deadline from HTTP header (X-Request-Deadline) or calculates
 * from default timeout. Sets the deadline in {@link RequestDeadlineContext}
 * for downstream components to check remaining time.
 * </p>
 * <p>
 * Ensures cleanup in finally block to prevent thread pool pollution.
 * </p>
 */
@Slf4j
@Component
@Order(1) // Run early in filter chain
@RequiredArgsConstructor
public class DeadlinePropagationFilter extends OncePerRequestFilter {

    private final ResilienceProperties resilienceProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!resilienceProperties.getDeadlinePropagation().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String headerName = resilienceProperties.getDeadlinePropagation().getHeaderName();
            String deadlineHeader = request.getHeader(headerName);

            if (deadlineHeader != null && !deadlineHeader.isBlank()) {
                // Parse deadline from header
                try {
                    Instant deadline = Instant.parse(deadlineHeader);
                    RequestDeadlineContext.setDeadline(deadline);
                    log.debug("Set deadline from header: {} = {}", headerName, deadline);
                } catch (DateTimeParseException e) {
                    log.warn("Invalid deadline header format: {}, using default timeout", deadlineHeader);
                    // Only set default if feature is enabled
                    if (resilienceProperties.getDeadlinePropagation().isEnabled()) {
                        setDefaultDeadline();
                    }
                }
            } else {
                // No header, use default timeout only if feature is enabled
                if (resilienceProperties.getDeadlinePropagation().isEnabled()) {
                    setDefaultDeadline();
                }
            }

            // Check if already expired before processing
            if (RequestDeadlineContext.isExpired()) {
                log.warn("Request deadline already expired before processing: {}",
                        RequestDeadlineContext.getDeadline().orElse(null));
                response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Request deadline exceeded\",\"status\":408}");
                return;
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clean up to prevent thread pool pollution
            RequestDeadlineContext.clear();
        }
    }

    private void setDefaultDeadline() {
        Duration defaultTimeout = resilienceProperties.getDeadlinePropagation().getDefaultTimeout();
        RequestDeadlineContext.setDeadlineFromTimeout(defaultTimeout);
        log.trace("Set default deadline with timeout: {}", defaultTimeout);
    }
}
