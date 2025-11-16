package com.example.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback controller for circuit breaker failures.
 * <p>
 * Returns 503 Service Unavailable when backend services are unavailable
 * or circuit breaker is open.
 * </p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback endpoint for circuit breaker failures.
     *
     * @return 503 Service Unavailable response
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> fallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Service temporarily unavailable. Please try again later.",
                        "timestamp", Instant.now().toString(),
                        "error", "SERVICE_UNAVAILABLE"
                ));
    }
}

