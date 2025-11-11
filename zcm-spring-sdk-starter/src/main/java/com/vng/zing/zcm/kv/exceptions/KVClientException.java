package com.vng.zing.zcm.kv.exceptions;

/**
 * Base exception for KV client errors.
 */
public class KVClientException extends RuntimeException {
  public KVClientException(String message) {
    super(message);
  }

  public KVClientException(String message, Throwable cause) {
    super(message, cause);
  }
}

