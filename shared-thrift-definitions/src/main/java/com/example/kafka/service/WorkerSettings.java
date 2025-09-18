package com.example.kafka.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Settings for worker behavior (sleep time, fail rate) per phase
 * Used for learning/testing purposes
 */
@Component
public class WorkerSettings {
    
    private final Map<Integer, Double> failRates = new ConcurrentHashMap<>();
    private final Map<Integer, Long> sleepMs = new ConcurrentHashMap<>();
    
    public double getFailRate(int phase) {
        return failRates.getOrDefault(phase, 0.0);
    }
    
    public void setFailRate(int phase, double rate) {
        failRates.put(phase, Math.max(0.0, Math.min(1.0, rate))); // Clamp between 0 and 1
    }
    
    public long getSleepMs(int phase) {
        return sleepMs.getOrDefault(phase, 50L); // Default 50ms
    }
    
    public void setSleepMs(int phase, long ms) {
        sleepMs.put(phase, Math.max(0L, ms)); // No negative values
    }
    
    public Map<Integer, Double> getAllFailRates() {
        return Map.copyOf(failRates);
    }
    
    public Map<Integer, Long> getAllSleepMs() {
        return Map.copyOf(sleepMs);
    }
    
    public void reset() {
        failRates.clear();
        sleepMs.clear();
    }
}
