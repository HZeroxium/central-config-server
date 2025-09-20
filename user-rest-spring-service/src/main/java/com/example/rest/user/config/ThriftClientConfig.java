package com.example.rest.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable Thrift client properties.
 */
@Configuration
@EnableConfigurationProperties(ThriftClientProperties.class)
public class ThriftClientConfig {
}
