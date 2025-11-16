package com.example.configserver.config;

import com.example.configserver.metrics.ConfigServerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for observability features (metrics, health indicators).
 * <p>
 * Configures Micrometer metrics and provides beans for custom metrics components.
 *
 * @author Config Server Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class ObservabilityConfig {

    /**
     * Creates a ConfigServerMetrics bean.
     *
     * @param meterRegistry the Micrometer meter registry
     * @return ConfigServerMetrics instance
     */
    @Bean
    public ConfigServerMetrics configServerMetrics(MeterRegistry meterRegistry) {
        log.info("Creating ConfigServerMetrics bean");
        return new ConfigServerMetrics(meterRegistry);
    }

    /**
     * Customizes the MeterRegistry with common tags.
     * <p>
     * Note: Additional tags are configured in application-observability.yml
     * This bean allows programmatic customization if needed.
     *
     * @return MeterRegistryCustomizer
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> {
            log.info("Customizing MeterRegistry with common tags");
            // Tags are configured in application-observability.yml
            // This is a placeholder for any programmatic customization
        };
    }
}

