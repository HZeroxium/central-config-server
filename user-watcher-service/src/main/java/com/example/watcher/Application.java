package com.example.watcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.example.watcher.config.AppProperties;
import com.example.watcher.config.KafkaTopicsProperties;
import com.example.watcher.config.PersistenceProperties;

@SpringBootApplication
@EnableConfigurationProperties({ AppProperties.class, KafkaTopicsProperties.class, PersistenceProperties.class })
@ComponentScan(basePackages = { "com.example.watcher", "com.example.kafka", "com.example.user", "com.example.common" })
@EnableCaching
@EnableMongoRepositories(basePackages = "com.example.user.adapter.mongo")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
