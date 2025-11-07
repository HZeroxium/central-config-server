package com.example.control.infrastructure.consulclient.core;

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
     * Recurse delete (for DELETE operations).
     */
    private Boolean recurse;

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

        if (Boolean.TRUE.equals(recurse)) {
            if (!first) query.append("&");
            query.append("recurse=true");
            first = false;
        }

        // Token should NOT be in query string (deprecated), use header instead
        // Removed token from query string to follow Consul best practices

        return query.toString();
    }
}
