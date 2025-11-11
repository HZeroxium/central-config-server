package com.vng.zing.zcm.kv.dto;

/**
 * Representation of a single KV transaction operation to be executed.
 */
public record KVTransactionOperationRequest(
    String op,
    String path,
    String value,
    String encoding,
    Long flags,
    Long cas
) {

  public KVTransactionOperationRequest {
    if (op == null || op.isBlank()) {
      throw new IllegalArgumentException("op must not be blank");
    }
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("path must not be blank");
    }
    flags = flags == null ? 0L : flags;
  }

  public static KVTransactionOperationRequest set(String path, String value) {
    return new KVTransactionOperationRequest("set", path, value, "utf8", 0L, null);
  }

  public static KVTransactionOperationRequest delete(String path) {
    return new KVTransactionOperationRequest("delete", path, null, null, 0L, null);
  }
}


