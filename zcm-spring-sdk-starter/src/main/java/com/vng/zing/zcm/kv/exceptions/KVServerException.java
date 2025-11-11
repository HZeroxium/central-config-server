package com.vng.zing.zcm.kv.exceptions;

/**
 * Exception thrown when KV server returns an error (5xx).
 */
public class KVServerException extends KVClientException {
  public KVServerException(String message) {
    super(message);
  }

  public KVServerException(String message, Throwable cause) {
    super(message, cause);
  }
}

