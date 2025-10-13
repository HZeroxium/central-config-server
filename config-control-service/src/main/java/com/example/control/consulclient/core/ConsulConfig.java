package com.example.control.consulclient.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration properties for Consul SDK client.
 */
@Data
@ConfigurationProperties(prefix = "consulclient")
public class ConsulConfig {
    
    /**
     * Consul server base URL (default: http://localhost:8500).
     */
    private URI consulUrl = URI.create("http://localhost:8500");
    
    /**
     * Consul ACL token for authentication.
     */
    private String token = "";
    
    /**
     * Default timeout for HTTP requests.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeout = Duration.ofSeconds(30);
    
    /**
     * Default timeout for blocking queries.
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration waitTimeout = Duration.ofMinutes(5);
    
    /**
     * Enable the Consul SDK client.
     */
    private boolean enabled = true;
}
