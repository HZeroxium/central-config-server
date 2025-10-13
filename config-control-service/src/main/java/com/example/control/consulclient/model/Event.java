package com.example.control.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Consul event.
 */
@Builder
public record Event(
    @JsonProperty("ID")
    String id,
    
    @JsonProperty("Name")
    String name,
    
    @JsonProperty("Payload")
    String payload, // Base64 encoded
    
    @JsonProperty("NodeFilter")
    String nodeFilter,
    
    @JsonProperty("ServiceFilter")
    String serviceFilter,
    
    @JsonProperty("TagFilter")
    String tagFilter,
    
    @JsonProperty("Version")
    int version,
    
    @JsonProperty("LTime")
    int lTime,
    
    @JsonProperty("CreateIndex")
    long createIndex,
    
    @JsonProperty("ModifyIndex")
    long modifyIndex
) {
    
    /**
     * Get the decoded payload as bytes.
     * 
     * @return decoded payload as byte array
     */
    public byte[] getPayloadBytes() {
        if (payload == null || payload.isEmpty()) {
            return new byte[0];
        }
        return java.util.Base64.getDecoder().decode(payload);
    }
    
    /**
     * Get the decoded payload as string.
     * 
     * @return decoded payload as string
     */
    public String getPayloadString() {
        byte[] bytes = getPayloadBytes();
        return new String(bytes);
    }
    
    /**
     * Create event with string payload.
     * 
     * @param id event ID
     * @param name event name
     * @param stringPayload string payload
     * @param nodeFilter node filter
     * @param serviceFilter service filter
     * @param tagFilter tag filter
     * @param version version
     * @param lTime logical time
     * @param createIndex create index
     * @param modifyIndex modify index
     * @return Event with base64 encoded payload
     */
    public static Event withStringPayload(String id, String name, String stringPayload,
                                        String nodeFilter, String serviceFilter, String tagFilter,
                                        int version, int lTime, long createIndex, long modifyIndex) {
        String base64Payload = stringPayload != null ? 
            java.util.Base64.getEncoder().encodeToString(stringPayload.getBytes()) : "";
        return new Event(id, name, base64Payload, nodeFilter, serviceFilter, tagFilter, 
                        version, lTime, createIndex, modifyIndex);
    }
    
    /**
     * Create event with byte payload.
     * 
     * @param id event ID
     * @param name event name
     * @param bytePayload byte payload
     * @param nodeFilter node filter
     * @param serviceFilter service filter
     * @param tagFilter tag filter
     * @param version version
     * @param lTime logical time
     * @param createIndex create index
     * @param modifyIndex modify index
     * @return Event with base64 encoded payload
     */
    public static Event withBytePayload(String id, String name, byte[] bytePayload,
                                      String nodeFilter, String serviceFilter, String tagFilter,
                                      int version, int lTime, long createIndex, long modifyIndex) {
        String base64Payload = bytePayload != null ? 
            java.util.Base64.getEncoder().encodeToString(bytePayload) : "";
        return new Event(id, name, base64Payload, nodeFilter, serviceFilter, tagFilter, 
                        version, lTime, createIndex, modifyIndex);
    }
}
