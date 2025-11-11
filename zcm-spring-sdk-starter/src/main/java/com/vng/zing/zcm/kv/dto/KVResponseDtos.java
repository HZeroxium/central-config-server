package com.vng.zing.zcm.kv.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTOs for KV API operations.
 * <p>
 * These DTOs match the response structure from config-control-service KV API.
 */
public class KVResponseDtos {

  /**
   * Response for a single KV entry.
   */
  public record EntryResponse(
      @JsonProperty("path") String path,
      @JsonProperty("valueBase64") String valueBase64,
      @JsonProperty("modifyIndex") Long modifyIndex,
      @JsonProperty("createIndex") Long createIndex,
      @JsonProperty("flags") Long flags
  ) {
  }

  /**
   * Response for list of KV entries.
   */
  public record ListResponse(
      @JsonProperty("items") List<EntryResponse> items
  ) {
  }

  /**
   * Response for list of keys only.
   */
  public record KeysResponse(
      @JsonProperty("keys") List<String> keys
  ) {
  }

  /**
   * Response DTO for structured object.
   */
  public record ObjectResponse(
      @JsonProperty("data") java.util.Map<String, Object> data,
      @JsonProperty("type") String type
  ) {
  }

  /**
   * Response DTO for structured list.
   */
  public record ListResponseV2(
      @JsonProperty("items") List<ListItem> items,
      @JsonProperty("manifest") ListManifest manifest,
      @JsonProperty("type") String type
  ) {

    public record ListItem(
        @JsonProperty("id") String id,
        @JsonProperty("data") java.util.Map<String, Object> data
    ) {
    }
  }

  /**
   * Manifest metadata DTO.
   */
  public record ListManifest(
      @JsonProperty("order") List<String> order,
      @JsonProperty("version") long version,
      @JsonProperty("etag") String etag,
      @JsonProperty("metadata") java.util.Map<String, Object> metadata
  ) {
  }

  /**
   * Response DTO for transaction execution.
   */
  public record TransactionResponse(
      @JsonProperty("success") boolean success,
      @JsonProperty("results") List<TransactionResult> results,
      @JsonProperty("error") String error
  ) {

    public record TransactionResult(
        @JsonProperty("path") String path,
        @JsonProperty("success") boolean success,
        @JsonProperty("modifyIndex") Long modifyIndex,
        @JsonProperty("message") String message
    ) {
    }
  }
}

