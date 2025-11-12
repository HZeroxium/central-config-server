package com.example.control.infrastructure.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Servlet filter that authenticates requests using API key from X-API-Key header.
 * <p>
 * This filter runs before JWT authentication and provides an alternative authentication
 * mechanism for SDK clients. If a valid API key is provided, it creates an Authentication
 * object with SYS_ADMIN role and sets it in SecurityContext, bypassing JWT validation.
 * </p>
 * <p>
 * <strong>Behavior</strong>:
 * <ul>
 *   <li>Extracts {@code X-API-Key} header from request</li>
 *   <li>If present and valid, creates Authentication with SYS_ADMIN role</li>
 *   <li>If invalid or missing, continues to JWT authentication (doesn't fail)</li>
 *   <li>Skips processing for public endpoints like {@code /api/heartbeat/**}</li>
 * </ul>
 * </p>
 *
 * <p><strong>Order</strong>:
 * This filter should run early (before JWT authentication) to allow API key
 * authentication to take precedence when present.</p>
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "security.api-key", name = "enabled", havingValue = "true", matchIfMissing = false)
@Order(0)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyValidator apiKeyValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip API key authentication for public endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/api/heartbeat/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if API key validation is disabled
        if (!apiKeyValidator.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API key from header
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (StringUtils.hasText(apiKey)) {
            // Validate API key
            if (apiKeyValidator.validate(apiKey)) {
                log.debug("Valid API key provided, creating SYS_ADMIN authentication");
                
                // Create authentication with SYS_ADMIN role
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_SYS_ADMIN"));
                
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        "api-key-client", // principal
                        null, // credentials (not needed)
                        authorities
                );
                
                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("API key authentication successful for path: {}", path);
            } else {
                log.debug("Invalid API key provided, continuing to JWT authentication");
                // Don't fail here - let JWT authentication handle it
            }
        }

        // Continue filter chain (JWT authentication will run if API key auth didn't succeed)
        filterChain.doFilter(request, response);
    }
}

