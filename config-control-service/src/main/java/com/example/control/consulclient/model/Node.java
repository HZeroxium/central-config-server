package com.example.control.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Map;

/**
 * Consul node information.
 */
@Builder
public record Node(
    @JsonProperty("ID")
    String id,
    
    @JsonProperty("Node")
    String node,
    
    @JsonProperty("Address")
    String address,
    
    @JsonProperty("Datacenter")
    String datacenter,
    
    @JsonProperty("TaggedAddresses")
    Map<String, String> taggedAddresses,
    
    @JsonProperty("Meta")
    Map<String, String> meta,
    
    @JsonProperty("CreateIndex")
    long createIndex,
    
    @JsonProperty("ModifyIndex")
    long modifyIndex
) {
}
