package com.example.user.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for application-level profiling and metrics collection.
 * Enables method-level timing and custom metrics for business operations.
 */
@Configuration
@EnableAspectJAutoProxy
public class ProfilingConfig {

    /**
     * Enable @Timed annotation support for method-level profiling.
     * This allows us to measure execution time of individual methods.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
