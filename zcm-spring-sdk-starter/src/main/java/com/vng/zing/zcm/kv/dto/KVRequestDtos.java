package com.vng.zing.zcm.kv.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request DTOs mirroring config-control-service payloads.
 */
public class KVRequestDtos {

  public record ListWriteRequest(
      @JsonProperty("items") List<KVResponseDtos.ListResponseV2.ListItem> items,
      @JsonProperty("manifest") KVResponseDtos.ListManifest manifest,
      @JsonProperty("deletes") List<String> deletes
  ) {
  }

  public record TransactionRequest(
      @JsonProperty("operations") List<TransactionOperation> operations
  ) {

    public record TransactionOperation(
        @JsonProperty("op") String op,
        @JsonProperty("path") String path,
        @JsonProperty("value") String value,
        @JsonProperty("encoding") String encoding,
        @JsonProperty("flags") Long flags,
        @JsonProperty("cas") Long cas
    ) {
    }
  }
}


