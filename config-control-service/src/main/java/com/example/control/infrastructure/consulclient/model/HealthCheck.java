package com.example.control.infrastructure.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Consul health check information.
 */
@Builder
public record HealthCheck(
        @JsonProperty("Node")
        String node,

        @JsonProperty("CheckID")
        String checkId,

        @JsonProperty("Name")
        String name,

        @JsonProperty("Status")
        String status,

        @JsonProperty("Notes")
        String notes,

        @JsonProperty("Output")
        String output,

        @JsonProperty("ServiceID")
        String serviceId,

        @JsonProperty("ServiceName")
        String serviceName,

        @JsonProperty("ServiceTags")
        List<String> serviceTags,

        @JsonProperty("Type")
        String type,

        @JsonProperty("Definition")
        Map<String, Object> definition,

        @JsonProperty("CreateIndex")
        long createIndex,

        @JsonProperty("ModifyIndex")
        long modifyIndex
) {

    /**
     * Check if this health check is passing.
     *
     * @return true if status is "passing"
     */
    public boolean isPassing() {
        return "passing".equalsIgnoreCase(status);
    }

    /**
     * Check if this health check is warning.
     *
     * @return true if status is "warning"
     */
    public boolean isWarning() {
        return "warning".equalsIgnoreCase(status);
    }

    /**
     * Check if this health check is critical.
     *
     * @return true if status is "critical"
     */
    public boolean isCritical() {
        return "critical".equalsIgnoreCase(status);
    }
}
