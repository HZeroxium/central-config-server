package com.example.control.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Duration;
import java.util.List;

/**
 * Request for creating a Consul session.
 */
@Builder
public record SessionCreateRequest(
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
    String ttl // Duration as string (e.g., "15s")
) {
    
    /**
     * Create a session request with Duration objects.
     * 
     * @param name session name
     * @param node node name
     * @param checks health checks
     * @param lockDelay lock delay duration
     * @param behavior session behavior
     * @param ttl TTL duration
     * @return SessionCreateRequest
     */
    public static SessionCreateRequest withDurations(String name, String node, List<String> checks,
                                                    Duration lockDelay, String behavior, Duration ttl) {
        return SessionCreateRequest.builder()
            .name(name)
            .node(node)
            .checks(checks)
            .lockDelay(formatDuration(lockDelay))
            .behavior(behavior)
            .ttl(formatDuration(ttl))
            .build();
    }
    
    /**
     * Format Duration as Consul string format.
     * 
     * @param duration the duration
     * @return formatted duration string
     */
    private static String formatDuration(Duration duration) {
        if (duration == null) {
            return null;
        }
        long seconds = duration.getSeconds();
        if (seconds == 0) {
            return null;
        }
        return seconds + "s";
    }
}
