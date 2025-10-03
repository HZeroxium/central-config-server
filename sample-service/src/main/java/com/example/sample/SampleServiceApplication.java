package com.example.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Sample Service Application - Config Client Demo
 * 
 * This service demonstrates:
 * - Loading configuration from Config Server
 * - @ConfigurationProperties for automatic rebinding
 * - @RefreshScope for manual refresh scenarios
 * - Spring Cloud Bus for automatic refresh via Kafka
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SampleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleServiceApplication.class, args);
    }
}
