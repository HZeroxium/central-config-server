package com.example.sample.web;

import com.example.sample.config.GreetingProperties;
import com.example.sample.service.GreetingService;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller demonstrating Config Server integration
 * 
 * Shows different ways to access configuration:
 * 1. @ConfigurationProperties (automatic rebinding)
 * 2. @Value + @RefreshScope (manual refresh required)
 * 3. Environment API (direct access)
 */
@RestController
@RequestMapping("/api")
public class ConfigController {

    private final GreetingProperties greetingProps;
    private final GreetingService greetingService;
    private final Environment environment;

    public ConfigController(
            GreetingProperties greetingProps,
            GreetingService greetingService,
            Environment environment) {
        this.greetingProps = greetingProps;
        this.greetingService = greetingService;
        this.environment = environment;
    }

    /**
     * Simple greeting endpoint
     */
    @GetMapping("/hello/{name}")
    public Map<String, Object> hello(@PathVariable String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("greeting", greetingService.greet(name));
        response.put("from", "sample-service");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Configuration comparison endpoint
     * Shows values from different configuration sources
     */
    @GetMapping("/config")
    public Map<String, Object> getCurrentConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // From @ConfigurationProperties (rebinds automatically)
        config.put("greetingProps.message", greetingProps.message());
        config.put("greetingProps.delayMs", greetingProps.delayMs());
        config.put("greetingProps.enabled", greetingProps.enabled());
        
        // From @Value + @RefreshScope (requires refresh)
        config.put("greetingService.messagePrefix", greetingService.getMessagePrefix());
        config.put("greetingService.timeoutMs", greetingService.getTimeoutMs());
        
        // From Environment API
        config.put("spring.application.name", environment.getProperty("spring.application.name"));
        config.put("spring.profiles.active", environment.getProperty("spring.profiles.active", "(none)"));
        config.put("config.client.version", environment.getProperty("config.client.version"));
        
        // Show property sources (using ConfigurableEnvironment)
        // if (environment instanceof org.springframework.core.env.ConfigurableEnvironment) {
        //     config.put("propertySources", ((org.springframework.core.env.ConfigurableEnvironment) environment)
        //         .getPropertySources().toString());
        // }
        
        return config;
    }

    /**
     * Feature flags endpoint (deprecated - use /api/features from FeatureFlagController instead)
     * Demonstrates dynamic configuration from properties
     */
    @GetMapping("/config/features")
    public Map<String, Object> getFeatures() {
        Map<String, Object> features = new HashMap<>();
        features.put("darkMode", environment.getProperty("feature.dark-mode", Boolean.class, false));
        features.put("newEndpointV2", environment.getProperty("feature.new-endpoint-v2", Boolean.class, false));
        features.put("useCacheV1", environment.getProperty("feature.use-cache-v1", Boolean.class, true));
        return features;
    }

    /**
     * Health check with configuration info
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "sample-service");
        health.put("configServerConnected", environment.getProperty("config.client.version") != null);
        health.put("greetingEnabled", greetingProps.enabled());
        return health;
    }
}
