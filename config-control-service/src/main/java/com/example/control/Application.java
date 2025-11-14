package com.example.control;

import com.example.control.infrastructure.config.misc.ConfigProxyProperties;
import com.example.control.infrastructure.config.misc.ConfigServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Config Control Service - Aggregator/Policy service for centralized
 * configuration management.
 * Provides drift detection, heartbeat tracking, and refresh orchestration.
 */
@SpringBootApplication(scanBasePackages = "com.example.control")
@EnableDiscoveryClient
@EnableScheduling
@ConfigurationPropertiesScan
@EnableMongoRepositories(basePackages = "com.example.control.infrastructure.adapter.persistence.mongo.repository")
@EnableConfigurationProperties({ ConfigServerProperties.class, ConfigProxyProperties.class })
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
