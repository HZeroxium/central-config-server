package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway application.
 * <p>
 * Provides API gateway functionality with service discovery, load balancing,
 * circuit breaking, rate limiting, and observability.
 * </p>
 *
 * @since 1.0.0
 */
@SpringBootApplication()
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

