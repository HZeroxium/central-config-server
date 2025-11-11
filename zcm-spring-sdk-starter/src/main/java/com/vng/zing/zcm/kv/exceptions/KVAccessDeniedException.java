package com.vng.zing.zcm.kv.exceptions;

/**
 * Exception thrown when KV access is denied (403 Forbidden).
 */
public class KVAccessDeniedException extends KVClientException {
  public KVAccessDeniedException(String message) {
    super(message);
  }

  public KVAccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }
}

