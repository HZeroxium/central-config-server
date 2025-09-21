package com.example.thriftserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
@Validated
@ConfigurationProperties(prefix = "thrift")
public class ThriftProperties {

  @Min(1024)
  private int port = 9090;

  @NotBlank
  private String serverName = "thrift-server";

  @Min(1)
  private int maxThreads = 10;

  @Min(1)
  private int minThreads = 2;
}
