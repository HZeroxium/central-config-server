package com.example.control.config;

import com.example.control.domain.port.DriftEventRepositoryPort;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import com.example.control.infrastructure.adapter.mongo.DriftEventMongoAdapter;
import com.example.control.infrastructure.adapter.mongo.ServiceInstanceMongoAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration wiring domain ports to Mongo adapters.
 */
@Configuration
public class PortAdapterConfig {

  @Bean
  public ServiceInstanceRepositoryPort serviceInstanceRepositoryPort(ServiceInstanceMongoAdapter adapter) {
    return adapter;
  }

  @Bean
  public DriftEventRepositoryPort driftEventRepositoryPort(DriftEventMongoAdapter adapter) {
    return adapter;
  }
}


