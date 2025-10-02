package com.example.rest.user.config;

import com.example.rest.user.adapter.ThriftUserClientAdapter;
import com.example.rest.user.adapter.SdkThriftUserClientAdapter;
import com.example.rest.user.port.ThriftUserClientPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration to choose between manual Thrift client and SDK-based client
 */
@Slf4j
@Configuration
public class ThriftClientConfig {

  @Value("${thrift.client.use-sdk:true}")
  private boolean useSdk;

  @Bean
  @Primary
  @ConditionalOnProperty(name = "thrift.client.use-sdk", havingValue = "true", matchIfMissing = true)
  public ThriftUserClientPort sdkThriftUserClientPort(SdkThriftUserClientAdapter adapter) {
    log.info("Using SDK-based Thrift client adapter");
    return adapter;
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "thrift.client.use-sdk", havingValue = "false")
  public ThriftUserClientPort manualThriftUserClientPort(ThriftUserClientAdapter adapter) {
    log.info("Using manual Thrift client adapter");
    return adapter;
  }
}
