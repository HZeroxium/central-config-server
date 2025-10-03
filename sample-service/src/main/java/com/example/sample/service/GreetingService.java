package com.example.sample.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * Service demonstrating @Value + @RefreshScope
 * 
 * @RefreshScope is required for @Value injection to update on refresh
 * The bean will be recreated when refresh occurs
 */
@RefreshScope
@Service
public class GreetingService {

    private final String messagePrefix;
    private final int timeoutMs;

    public GreetingService(
            @Value("${greeting.message:Hello}") String messagePrefix,
            @Value("${greeting.timeout-ms:5000}") int timeoutMs) {
        this.messagePrefix = messagePrefix;
        this.timeoutMs = timeoutMs;
    }

    public String greet(String name) {
        try {
            // Simulate some processing with configured delay
            Thread.sleep(100); // Small delay for demo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return messagePrefix + ", " + name + "!";
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
