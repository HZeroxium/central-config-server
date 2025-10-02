package com.example.control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Config Control Service - Aggregator/Policy service for centralized
 * configuration management.
 * Provides drift detection, heartbeat tracking, and refresh orchestration.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
@EnableMongoRepositories(basePackages = "com.example.control.infrastructure.repository")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
