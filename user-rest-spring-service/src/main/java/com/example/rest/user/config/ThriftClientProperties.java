package com.example.rest.user.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Thrift client.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "thrift.client")
public class ThriftClientProperties {

  @NotBlank(message = "Thrift host is required")
  private String host = "localhost";

  @Min(value = 1, message = "Port must be between 1 and 65535")
  @Max(value = 65535, message = "Port must be between 1 and 65535")
  private int port = 9090;

  @Min(value = 1000, message = "Timeout must be at least 1000ms")
  private int timeout = 10000;

  @Min(value = 1, message = "Retry attempts must be at least 1")
  private int retryAttempts = 3;

  @Min(value = 1, message = "Connection pool size must be at least 1")
  private int connectionPoolSize = 10;
}
