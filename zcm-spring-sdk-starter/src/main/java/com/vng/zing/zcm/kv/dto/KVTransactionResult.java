package com.vng.zing.zcm.kv.dto;

import java.util.List;

/**
 * Result of executing transactional KV operations.
 */
public record KVTransactionResult(
    boolean success,
    List<OperationResult> operations,
    String message
) {

  public KVTransactionResult {
    operations = operations == null ? List.of() : List.copyOf(operations);
    message = message == null ? "" : message;
  }

  public record OperationResult(String path, boolean success, Long modifyIndex, String message) {
    public OperationResult {
      message = message == null ? "" : message;
    }

  public static KVTransactionResult success(List<OperationResult> operations) {
    return new KVTransactionResult(true, operations, "");
  }

  public static KVTransactionResult failure(List<OperationResult> operations, String message) {
    return new KVTransactionResult(false, operations, message);
  }
}
}


