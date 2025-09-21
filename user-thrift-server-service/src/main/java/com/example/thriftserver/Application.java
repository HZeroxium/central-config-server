package com.example.thriftserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import com.example.thriftserver.config.ThriftProperties;
import com.example.thriftserver.config.KafkaTopicsProperties;
import com.example.thriftserver.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties({ ThriftProperties.class, KafkaTopicsProperties.class, AppProperties.class })
@ComponentScan(basePackages = { "com.example.thriftserver", "com.example.kafka", "com.example.common" })
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
