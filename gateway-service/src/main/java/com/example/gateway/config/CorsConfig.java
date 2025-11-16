package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for gateway.
 * <p>
 * Configures CORS at gateway level to handle cross-origin requests.
 * This replaces CORS handling in backend services.
 * </p>
 *
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    /**
     * Configures CORS filter for all routes.
     * <p>
     * Allows all origins, methods, and headers for development.
     * In production, origins should be restricted.
     * </p>
     *
     * @return CorsWebFilter configured for gateway routes
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Allow all origins (restrict in production)
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        
        // Allowed methods
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Allowed headers
        corsConfig.setAllowedHeaders(List.of("*"));
        
        // Allow credentials
        corsConfig.setAllowCredentials(true);
        
        // Max age for preflight requests
        corsConfig.setMaxAge(3600L);
        
        // Exposed headers
        corsConfig.setExposedHeaders(Arrays.asList(
                "X-Correlation-ID",
                "X-Gateway-Version",
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Reset"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}

