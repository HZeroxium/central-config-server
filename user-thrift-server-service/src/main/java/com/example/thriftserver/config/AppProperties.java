package com.example.thriftserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  @NotBlank
  private String name = "user-thrift-server-service";

  @NotBlank
  private String version = "1.0.0";

  @NotBlank
  private String environment = "development";

  @Positive
  private int rpcTimeoutSeconds = 30;

  @Positive
  private int maxPendingReplies = 1000;
}
