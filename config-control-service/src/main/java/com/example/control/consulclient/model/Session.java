package com.example.control.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Duration;
import java.util.List;

/**
 * Consul session information.
 */
@Builder
public record Session(
    @JsonProperty("ID")
    String id,
    
    @JsonProperty("Name")
    String name,
    
    @JsonProperty("Node")
    String node,
    
    @JsonProperty("Checks")
    List<String> checks,
    
    @JsonProperty("LockDelay")
    String lockDelay, // Duration as string (e.g., "15s")
    
    @JsonProperty("Behavior")
    String behavior,
    
    @JsonProperty("TTL")
    String ttl, // Duration as string (e.g., "15s")
    
    @JsonProperty("CreateIndex")
    long createIndex,
    
    @JsonProperty("ModifyIndex")
    long modifyIndex
) {
    
    /**
     * Get TTL as Duration.
     * 
     * @return TTL as Duration, or null if not set
     */
    public Duration getTtlDuration() {
        if (ttl == null || ttl.isEmpty()) {
            return null;
        }
        try {
            return Duration.parse("PT" + ttl.toUpperCase());
        } catch (Exception e) {
            // Fallback for simple format like "15s"
            if (ttl.endsWith("s")) {
                try {
                    long seconds = Long.parseLong(ttl.substring(0, ttl.length() - 1));
                    return Duration.ofSeconds(seconds);
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }
    
    /**
     * Get lock delay as Duration.
     * 
     * @return lock delay as Duration, or null if not set
     */
    public Duration getLockDelayDuration() {
        if (lockDelay == null || lockDelay.isEmpty()) {
            return null;
        }
        try {
            return Duration.parse("PT" + lockDelay.toUpperCase());
        } catch (Exception e) {
            // Fallback for simple format like "15s"
            if (lockDelay.endsWith("s")) {
                try {
                    long seconds = Long.parseLong(lockDelay.substring(0, lockDelay.length() - 1));
                    return Duration.ofSeconds(seconds);
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }
}
