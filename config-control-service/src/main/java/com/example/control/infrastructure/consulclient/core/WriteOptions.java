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
     * Flags (arbitrary uint64 metadata) for KV operations.
     * Used to encode type information (0=LEAF, 2=LIST, 3=LEAF_LIST).
     */
    private Long flags; 

    /**
     * CAS (Compare-And-Set) modify index for conditional write operations.
     * Only update if current modify index matches. null means unconditional update.
     */
    private Long cas;

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
        
        if (cas != null) {
            if (!first) query.append("&");
            query.append("cas=").append(cas);
            first = false;
        }

        if (flags != null) {
            if (!first) query.append("&");
            query.append("flags=").append(flags);
            first = false;
        }

        // Token should NOT be in query string (deprecated), use header instead
        // Removed token from query string to follow Consul best practices

        return query.toString();
    }
}
