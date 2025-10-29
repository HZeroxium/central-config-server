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
     * Consistency mode for the query.
     */
    private Consistency consistency;

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

        if (consistency != null) {
            if (!first) query.append("&");
            query.append("consistency=").append(consistency.name().toLowerCase());
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

        if (token != null && !token.isEmpty()) {
            if (!first) query.append("&");
            query.append("token=").append(token);
            first = false;
        }

        return query.toString();
    }
}
