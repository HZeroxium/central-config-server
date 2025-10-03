package com.example.sample.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration Properties example
 * 
 * @ConfigurationProperties automatically rebinds when:
 * - POST /actuator/refresh (manual refresh)
 * - POST /actuator/busrefresh (Spring Cloud Bus refresh)
 * 
 * No @RefreshScope needed - Spring handles rebinding automatically
 */
@ConfigurationProperties(prefix = "greeting")
@Validated
public record GreetingProperties(
    @NotBlank String message,
    @Min(0) int delayMs,
    boolean enabled
) {
    
    public GreetingProperties {
        // Default values
        message = message != null ? message : "Hello";
        delayMs = delayMs >= 0 ? delayMs : 0;
    }
}
