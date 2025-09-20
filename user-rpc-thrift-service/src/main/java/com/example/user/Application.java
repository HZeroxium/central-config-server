package com.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ComponentScan(basePackages = { "com.example.user", "com.example.kafka", "com.example.common" })
@EnableCaching
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}