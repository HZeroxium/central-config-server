package com.example.control.infrastructure.consulclient.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Consul KV pair with metadata.
 */
@Builder
public record KVPair(
        @JsonProperty("Key")
        String key,

        @JsonProperty("Value")
        String value, // Base64 encoded value from Consul

        @JsonProperty("Flags")
        long flags,

        @JsonProperty("CreateIndex")
        long createIndex,

        @JsonProperty("ModifyIndex")
        long modifyIndex,

        @JsonProperty("LockIndex")
        long lockIndex,

        @JsonProperty("Session")
        String session
) {

    /**
     * Create KVPair with raw bytes (will be encoded to base64).
     *
     * @param key         the key
     * @param valueBytes  the raw value bytes
     * @param flags       flags
     * @param createIndex create index
     * @param modifyIndex modify index
     * @param lockIndex   lock index
     * @param session     session ID
     * @return KVPair with base64 encoded value
     */
    public static KVPair withValueBytes(String key, byte[] valueBytes, long flags,
                                        long createIndex, long modifyIndex, long lockIndex, String session) {
        String base64Value = valueBytes != null ? Base64.getEncoder().encodeToString(valueBytes) : "";
        return new KVPair(key, base64Value, flags, createIndex, modifyIndex, lockIndex, session);
    }

    /**
     * Create KVPair with string value (will be encoded to base64).
     *
     * @param key         the key
     * @param stringValue the string value
     * @param flags       flags
     * @param createIndex create index
     * @param modifyIndex modify index
     * @param lockIndex   lock index
     * @param session     session ID
     * @return KVPair with base64 encoded value
     */
    public static KVPair withStringValue(String key, String stringValue, long flags,
                                         long createIndex, long modifyIndex, long lockIndex, String session) {
        String base64Value = stringValue != null ? Base64.getEncoder().encodeToString(stringValue.getBytes(StandardCharsets.UTF_8)) : "";
        return new KVPair(key, base64Value, flags, createIndex, modifyIndex, lockIndex, session);
    }

    /**
     * Get the decoded value as bytes.
     *
     * @return decoded value as byte array
     */
    @JsonIgnore
    public byte[] getValueBytes() {
        if (value == null || value.isEmpty()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(value);
    }
}
