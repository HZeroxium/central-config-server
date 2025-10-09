package com.vng.zing.zcm.loadbalancer;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Represents a load balancing request with context information.
 * <p>
 * This class provides additional context for load balancing algorithms
 * such as Rendezvous and Consistent Hashing to make more informed decisions.
 * It includes information about the request, client, and any custom attributes.
 */
public class LbRequest {
    
    private final String requestId;
    private final String clientId;
    private final String sessionId;
    private final String userId;
    private final Map<String, Object> attributes;
    private final long timestamp;
    
    private LbRequest(Builder builder) {
        this.requestId = builder.requestId;
        this.clientId = builder.clientId;
        this.sessionId = builder.sessionId;
        this.userId = builder.userId;
        this.attributes = new HashMap<>(builder.attributes);
        this.timestamp = builder.timestamp;
    }
    
    /**
     * Creates a new builder for LbRequest.
     *
     * @return a new LbRequest builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a simple LbRequest with just a request ID.
     *
     * @param requestId the request identifier
     * @return a new LbRequest instance
     */
    public static LbRequest of(String requestId) {
        return builder().requestId(requestId).build();
    }
    
    /**
     * Creates an LbRequest with request ID and user ID for session affinity.
     *
     * @param requestId the request identifier
     * @param userId the user identifier for session affinity
     * @return a new LbRequest instance
     */
    public static LbRequest withUser(String requestId, String userId) {
        return builder().requestId(requestId).userId(userId).build();
    }
    
    /**
     * Creates an LbRequest with client information.
     *
     * @param requestId the request identifier
     * @param clientId the client identifier
     * @param sessionId the session identifier
     * @return a new LbRequest instance
     */
    public static LbRequest withClient(String requestId, String clientId, String sessionId) {
        return builder()
            .requestId(requestId)
            .clientId(clientId)
            .sessionId(sessionId)
            .build();
    }
    
    /**
     * Gets a stable key for hashing algorithms.
     * <p>
     * This method provides a deterministic key that can be used by
     * algorithms like Rendezvous and Consistent Hashing. The key
     * prioritizes session affinity when available.
     *
     * @return a stable key for hashing
     */
    public String getHashKey() {
        if (sessionId != null) {
            return sessionId;
        }
        if (userId != null) {
            return userId;
        }
        if (clientId != null) {
            return clientId;
        }
        return requestId;
    }
    
    /**
     * Gets a key for consistent hashing that includes request context.
     *
     * @param serviceName the service name
     * @return a composite key for consistent hashing
     */
    public String getConsistentHashKey(String serviceName) {
        String baseKey = getHashKey();
        return serviceName + ":" + baseKey;
    }
    
    /**
     * Gets a key for rendezvous hashing.
     *
     * @param serviceName the service name
     * @param instanceId the instance identifier
     * @return a composite key for rendezvous hashing
     */
    public String getRendezvousHashKey(String serviceName, String instanceId) {
        return serviceName + ":" + getHashKey() + ":" + instanceId;
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public String getClientId() { return clientId; }
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public Map<String, Object> getAttributes() { return new HashMap<>(attributes); }
    public long getTimestamp() { return timestamp; }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LbRequest lbRequest = (LbRequest) o;
        return timestamp == lbRequest.timestamp &&
               Objects.equals(requestId, lbRequest.requestId) &&
               Objects.equals(clientId, lbRequest.clientId) &&
               Objects.equals(sessionId, lbRequest.sessionId) &&
               Objects.equals(userId, lbRequest.userId) &&
               Objects.equals(attributes, lbRequest.attributes);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(requestId, clientId, sessionId, userId, attributes, timestamp);
    }
    
    @Override
    public String toString() {
        return "LbRequest{" +
               "requestId='" + requestId + '\'' +
               ", clientId='" + clientId + '\'' +
               ", sessionId='" + sessionId + '\'' +
               ", userId='" + userId + '\'' +
               ", timestamp=" + timestamp +
               ", attributes=" + attributes.size() + " items" +
               '}';
    }
    
    /**
     * Builder class for LbRequest.
     */
    public static class Builder {
        private String requestId;
        private String clientId;
        private String sessionId;
        private String userId;
        private final Map<String, Object> attributes = new HashMap<>();
        private long timestamp = System.currentTimeMillis();
        
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public LbRequest build() {
            if (requestId == null) {
                requestId = "req-" + System.currentTimeMillis() + "-" + Thread.currentThread().getName();
            }
            return new LbRequest(this);
        }
    }
}
