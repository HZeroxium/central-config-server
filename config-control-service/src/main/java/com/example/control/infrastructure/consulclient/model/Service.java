package com.example.control.infrastructure.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Consul service information.
 */
@Builder
public record Service(
        @JsonProperty("ID")
        String id,

        @JsonProperty("Service")
        String service,

        @JsonProperty("Tags")
        List<String> tags,

        @JsonProperty("Address")
        String address,

        @JsonProperty("Meta")
        Map<String, String> meta,

        @JsonProperty("Port")
        int port,

        @JsonProperty("Weights")
        ServiceWeights weights,

        @JsonProperty("EnableTagOverride")
        boolean enableTagOverride,

        @JsonProperty("CreateIndex")
        long createIndex,

        @JsonProperty("ModifyIndex")
        long modifyIndex
) {

    /**
     * Service weights configuration.
     */
    @Builder
    public record ServiceWeights(
            @JsonProperty("Passing")
            int passing,

            @JsonProperty("Warning")
            int warning
    ) {
    }
}
