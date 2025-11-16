package com.example.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Configuration for rate limiting.
 * <p>
 * Extracts user ID from JWT token's 'sub' claim for per-user rate limiting.
 * Falls back to IP address if JWT is not present.
 * </p>
 *
 * @since 1.0.0
 */
@Configuration
public class RateLimitConfig {

    /**
     * Key resolver that extracts user ID from JWT token.
     * <p>
     * Parses the Authorization header, extracts JWT, decodes the 'sub' claim,
     * and uses it as the rate limit key. Falls back to client IP if JWT is missing.
     * </p>
     *
     * @return KeyResolver that returns user ID or IP address
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to extract user ID from JWT token
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    // Decode JWT (simple base64 decode, no validation - backend validates)
                    String[] parts = token.split("\\.");
                    if (parts.length >= 2) {
                        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                        ObjectMapper mapper = new ObjectMapper();
                        var jsonNode = mapper.readTree(payload);
                        String userId = jsonNode.get("sub").asText();
                        if (userId != null && !userId.isEmpty()) {
                            return Mono.just("user:" + userId);
                        }
                    }
                } catch (Exception e) {
                    // If JWT parsing fails, fall back to IP
                }
            }
            
            // Fallback to client IP address
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + clientIp);
        };
    }
}

