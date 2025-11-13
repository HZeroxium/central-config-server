package com.example.control.domain.model.kv;

import com.example.control.infrastructure.cache.jackson.ByteArrayBase64Deserializer;
import com.example.control.infrastructure.cache.jackson.ByteArrayBase64Serializer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;

import java.util.Base64;

/**
 * Domain model representing a Key-Value entry in Consul KV store.
 * <p>
 * This is a domain-focused representation that abstracts away Consul-specific
 * details while preserving essential metadata for CAS operations and versioning.
 * </p>
 * <p>
 * <strong>Serialization:</strong> The {@code value} field is serialized as Base64 string
 * to avoid type information issues with Jackson's default typing in Redis cache.
 * </p>
 */
@Builder
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public record KVEntry(
        /**
         * The absolute key path in Consul (e.g., "apps/service-id/kv/config/db.url").
         */
        String key,

        /**
         * The value as raw bytes.
         * Serialized as Base64 string in JSON to avoid type information issues.
         */
        @JsonSerialize(using = ByteArrayBase64Serializer.class)
        @JsonDeserialize(using = ByteArrayBase64Deserializer.class)
        byte[] value,

        /**
         * Modify index for CAS operations.
         */
        long modifyIndex,

        /**
         * Create index (when the key was first created).
         */
        long createIndex,

        /**
         * Flags (arbitrary uint64 metadata).
         */
        long flags,

        /**
         * Lock index (incremented when lock is acquired).
         */
        long lockIndex,

        /**
         * Session ID if the key is locked.
         */
        String session
) {
    /**
     * Get the value as a UTF-8 string.
     *
     * @return value as string
     */
    public String getValueAsString() {
        if (value == null || value.length == 0) {
            return "";
        }
        return new String(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Get the value as base64-encoded string.
     *
     * @return base64-encoded value
     */
    public String getValueAsBase64() {
        if (value == null || value.length == 0) {
            return "";
        }
        return Base64.getEncoder().encodeToString(value);
    }
}

