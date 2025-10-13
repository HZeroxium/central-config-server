package com.example.control.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Sealed interface for Consul transaction operations.
 */
public sealed interface TxnOp {
    
    /**
     * KV set operation.
     */
    @Builder
    record KVSet(
        @JsonProperty("Verb")
        String verb,
        
        @JsonProperty("Key")
        String key,
        
        @JsonProperty("Value")
        String value, // Base64 encoded
        
        @JsonProperty("Flags")
        Long flags,
        
        @JsonProperty("Index")
        Long index,
        
        @JsonProperty("Session")
        String session
    ) implements TxnOp {
        
        public static KVSet of(String key, byte[] value) {
            String base64Value = value != null ? java.util.Base64.getEncoder().encodeToString(value) : "";
            return KVSet.builder()
                .verb("set")
                .key(key)
                .value(base64Value)
                .build();
        }
        
        public static KVSet of(String key, byte[] value, Long cas) {
            String base64Value = value != null ? java.util.Base64.getEncoder().encodeToString(value) : "";
            return KVSet.builder()
                .verb("set")
                .key(key)
                .value(base64Value)
                .index(cas)
                .build();
        }
    }
    
    /**
     * KV get operation.
     */
    @Builder
    record KVGet(
        @JsonProperty("Verb")
        String verb,
        
        @JsonProperty("Key")
        String key,
        
        @JsonProperty("Index")
        Long index
    ) implements TxnOp {
        
        public static KVGet of(String key) {
            return KVGet.builder()
                .verb("get")
                .key(key)
                .build();
        }
        
        public static KVGet of(String key, Long index) {
            return KVGet.builder()
                .verb("get")
                .key(key)
                .index(index)
                .build();
        }
    }
    
    /**
     * KV delete operation.
     */
    @Builder
    record KVDelete(
        @JsonProperty("Verb")
        String verb,
        
        @JsonProperty("Key")
        String key,
        
        @JsonProperty("Index")
        Long index
    ) implements TxnOp {
        
        public static KVDelete of(String key) {
            return KVDelete.builder()
                .verb("delete")
                .key(key)
                .build();
        }
        
        public static KVDelete of(String key, Long index) {
            return KVDelete.builder()
                .verb("delete")
                .key(key)
                .index(index)
                .build();
        }
    }
    
    /**
     * KV check index operation.
     */
    @Builder
    record KVCheckIndex(
        @JsonProperty("Verb")
        String verb,
        
        @JsonProperty("Key")
        String key,
        
        @JsonProperty("Index")
        Long index
    ) implements TxnOp {
        
        public static KVCheckIndex of(String key, Long index) {
            return KVCheckIndex.builder()
                .verb("check-index")
                .key(key)
                .index(index)
                .build();
        }
    }
    
    /**
     * KV check session operation.
     */
    @Builder
    record KVCheckSession(
        @JsonProperty("Verb")
        String verb,
        
        @JsonProperty("Key")
        String key,
        
        @JsonProperty("Session")
        String session
    ) implements TxnOp {
        
        public static KVCheckSession of(String key, String session) {
            return KVCheckSession.builder()
                .verb("check-session")
                .key(key)
                .session(session)
                .build();
        }
    }
}
