package com.example.control.infrastructure.config.misc;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Config Server client.
 * <p>
 * Maps properties from {@code config.server.*} in application.yml.
 * Supports both direct URL and service discovery via Consul.
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "config.server")
public class ConfigServerProperties {
    /**
     * Base URL of Spring Cloud Config Server, e.g. http://config-server:8888
     * Used as fallback when service discovery is disabled or fails.
     */
    @NotBlank
    private String url = "http://config-server:8888";

    /**
     * Service discovery configuration.
     */
    private ServiceDiscovery serviceDiscovery = new ServiceDiscovery();

    /**
     * Load balancer configuration.
     */
    private LoadBalancer loadBalancer = new LoadBalancer();

    @Data
    public static class ServiceDiscovery {
        /**
         * Enable service discovery via Consul.
         */
        private boolean enabled = true;

        /**
         * Service name in Consul (default: "config-server").
         */
        @NotBlank
        private String serviceName = "config-server";

        /**
         * Fallback to direct URL if service discovery fails or no instances found.
         */
        private boolean fallbackToUrl = true;
    }

    @Data
    public static class LoadBalancer {
        /**
         * Load balancing strategy: round-robin, random, health-based.
         */
        @NotBlank
        private String strategy = "round-robin";
    }
}
