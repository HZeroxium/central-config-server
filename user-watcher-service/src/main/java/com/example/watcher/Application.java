package com.example.watcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ComponentScan(basePackages = { "com.example.watcher", "com.example.kafka", "com.example.user", "com.example.common" })
@EnableCaching
@EnableMongoRepositories(basePackages = "com.example.user.adapter.mongo")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
