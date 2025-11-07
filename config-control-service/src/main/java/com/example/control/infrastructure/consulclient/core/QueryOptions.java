package com.example.control.infrastructure.consulclient.core;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Options for Consul read operations (GET).
 */
@Data
@Builder
public class QueryOptions {

    /**
     * Datacenter to query (default: local datacenter).
     */
    private String datacenter;

    /**
     * Use consistent read (query leader for strong consistency).
     * Mutually exclusive with stale.
     */
    private Boolean consistent;

    /**
     * Use stale read (allow reads from followers for better performance).
     * Mutually exclusive with consistent.
     */
    private Boolean stale;

    /**
     * Minimum index for blocking queries.
     */
    private Long index;

    /**
     * Maximum time to wait for blocking queries.
     */
    private Duration wait;

    /**
     * Filter expression for the query.
     */
    private String filter;

    /**
     * Use cached results when available.
     */
    private Boolean cached;

    /**
     * Recurse into subdirectories (for list operations).
     */
    private Boolean recurse;

    /**
     * Return only keys, not full entries (for list operations).
     * Implies recurse.
     */
    private Boolean keys;

    /**
     * Separator for directory-like listing (e.g., "/").
     */
    private String separator;

    /**
     * Return raw value instead of JSON metadata (for get operations).
     */
    private Boolean raw;

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

        // Consistency flags: ?consistent or ?stale (mutually exclusive)
        if (Boolean.TRUE.equals(consistent)) {
            if (!first) query.append("&");
            query.append("consistent");
            first = false;
        } else if (Boolean.TRUE.equals(stale)) {
            if (!first) query.append("&");
            query.append("stale");
            first = false;
        }

        if (index != null) {
            if (!first) query.append("&");
            query.append("index=").append(index);
            first = false;
        }

        if (wait != null) {
            if (!first) query.append("&");
            query.append("wait=").append(wait.toSeconds()).append("s");
            first = false;
        }

        if (filter != null && !filter.isEmpty()) {
            if (!first) query.append("&");
            query.append("filter=").append(filter);
            first = false;
        }

        if (cached != null) {
            if (!first) query.append("&");
            query.append("cached=").append(cached);
            first = false;
        }

        if (Boolean.TRUE.equals(recurse)) {
            if (!first) query.append("&");
            query.append("recurse");
            first = false;
        }

        if (Boolean.TRUE.equals(keys)) {
            if (!first) query.append("&");
            query.append("keys");
            first = false;
        }

        if (separator != null && !separator.isEmpty()) {
            if (!first) query.append("&");
            query.append("separator=").append(separator);
            first = false;
        }

        if (Boolean.TRUE.equals(raw)) {
            if (!first) query.append("&");
            query.append("raw");
            first = false;
        }

        // Token should NOT be in query string (deprecated), use header instead
        // Removed token from query string to follow Consul best practices

        return query.toString();
    }
}
