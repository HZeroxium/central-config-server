package com.example.sample.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration Properties example
 * 
 * @ConfigurationProperties requires @RefreshScope to rebind when:
 * - POST /actuator/refresh (manual refresh)
 * - POST /actuator/busrefresh (Spring Cloud Bus refresh)
 * 
 */
@ConfigurationProperties(prefix = "greeting")
@Validated
public class GreetingProperties {
    
    @NotBlank
    private String message = "Hello";
    
    @Min(0)
    private int delayMs = 0;
    
    private boolean enabled = true;
    
    // Default constructor
    public GreetingProperties() {}
    
    // Getters and setters
    public String message() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message != null ? message : "Hello";
    }
    
    public int delayMs() {
        return delayMs;
    }
    
    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs >= 0 ? delayMs : 0;
    }
    
    public boolean enabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
