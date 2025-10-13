package com.example.control.consulclient.core;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Options for Consul write operations (PUT, POST, DELETE).
 */
@Data
@Builder
public class WriteOptions {
    
    /**
     * Datacenter to write to (default: local datacenter).
     */
    private String datacenter;
    
    /**
     * ACL token for the request.
     */
    private String token;
    
    /**
     * Timeout for the request.
     */
    private Duration timeout;
    
    /**
     * Build query parameters as a string.
     * 
     * @return query string with parameters
     */
    public String buildQueryString() {
        StringBuilder query = new StringBuilder();
        boolean first = true;
        
        if (datacenter != null && !datacenter.isEmpty()) {
            query.append("dc=").append(datacenter);
            first = false;
        }
        
        if (token != null && !token.isEmpty()) {
            if (!first) query.append("&");
            query.append("token=").append(token);
            first = false;
        }
        
        return query.toString();
    }
}
