package com.example.control.infrastructure.config.seeding;

import net.datafaker.Faker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for data seeding components.
 */
@Configuration
public class SeedingConfig {

    /**
     * Provides a Faker instance for generating mock data.
     *
     * @return Faker instance
     */
    @Bean
    public Faker faker() {
        return new Faker();
    }
}
