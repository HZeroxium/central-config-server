package com.example.control.kv.model;

import com.example.control.kv.KvStore;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * DTOs for KV Store REST API requests and responses.
 * Uses Java records for modern, concise data transfer objects.
 */
public final class KvDtos {
  
  /**
   * Request for putting a key-value pair.
   */
  public record PutRequest(
      String base64Value, 
      String expectedVersion, 
      Long ttlSeconds) {
    
    /**
     * Convert base64 value to bytes.
     */
    public byte[] valueBytes() { 
      return (base64Value == null) ? new byte[0] : Base64.getDecoder().decode(base64Value); 
    }
    
    /**
     * Convert TTL seconds to Duration.
     */
    public Duration ttl() { 
      return (ttlSeconds == null) ? null : Duration.ofSeconds(ttlSeconds); 
    }
  }
  
  /**
   * Response for put operations.
   */
  public record PutResponse(boolean success, String version) {}
  
  /**
   * Key-value entry with metadata.
   */
  public record Entry(
      String key, 
      String base64Value, 
      String version, 
      long createIndex, 
      long modifyIndex) {
    
    /**
     * Convert base64 value to bytes.
     */
    public byte[] valueBytes() {
      return (base64Value == null) ? new byte[0] : Base64.getDecoder().decode(base64Value);
    }
    
    /**
     * Create from KvStore.Entry.
     */
    public static Entry from(KvStore.Entry entry) {
      return new Entry(
          entry.key(),
          Base64.getEncoder().encodeToString(entry.value()),
          entry.version(),
          entry.createIndex(),
          entry.modifyIndex()
      );
    }
  }
  
  /**
   * Response for delete operations.
   */
  public record DeleteResponse(boolean deleted) {}
  
  /**
   * Response for list operations.
   */
  public record ListResponse(List<Entry> items) {}
  
  /**
   * Transaction operation.
   */
  public record TxnOp(
      String type, 
      String key, 
      String base64Value, 
      String expectedVersion, 
      Long ttlSeconds) {
    
    /**
     * Convert base64 value to bytes.
     */
    public byte[] valueBytes() {
      return (base64Value == null) ? new byte[0] : Base64.getDecoder().decode(base64Value);
    }
    
    /**
     * Convert TTL seconds to Duration.
     */
    public Duration ttl() {
      return (ttlSeconds == null) ? null : Duration.ofSeconds(ttlSeconds);
    }
    
    /**
     * Convert to KvStore.TxnOp.
     */
    public KvStore.TxnOp toKvStoreOp() {
      return new KvStore.TxnOp(
          KvStore.TxnOp.Type.valueOf(type.toUpperCase()),
          key,
          valueBytes(),
          expectedVersion,
          ttl()
      );
    }
  }
  
  /**
   * Request for transaction operations.
   */
  public record TxnRequest(List<TxnOp> ops) {}
  
  /**
   * Response for transaction operations.
   */
  public record TxnResponse(List<Boolean> results) {}
  
  /**
   * Request for lock operations.
   */
  public record LockRequest(
      @NotBlank String lockKey, 
      Long ttlSeconds) {
    
    /**
     * Convert TTL seconds to Duration.
     */
    public Duration ttl() {
      return (ttlSeconds == null) ? Duration.ofMinutes(5) : Duration.ofSeconds(ttlSeconds);
    }
  }
  
  /**
   * Response for lock acquisition.
   */
  public record LockResponse(String lockId) {}
  
  /**
   * Request for ephemeral key operations.
   */
  public record EphemeralRequest(
      @NotBlank String key, 
      String base64Value, 
      Long ttlSeconds) {
    
    /**
     * Convert base64 value to bytes.
     */
    public byte[] valueBytes() {
      return (base64Value == null) ? new byte[0] : Base64.getDecoder().decode(base64Value);
    }
    
    /**
     * Convert TTL seconds to Duration.
     */
    public Duration ttl() {
      return (ttlSeconds == null) ? Duration.ofMinutes(1) : Duration.ofSeconds(ttlSeconds);
    }
  }
  
  /**
   * Response for ephemeral key operations.
   */
  public record EphemeralResponse(String sessionId) {}
  
  /**
   * Error response for exception handling.
   */
  public record ErrorResponse(String code, String message, String timestamp) {}
}
