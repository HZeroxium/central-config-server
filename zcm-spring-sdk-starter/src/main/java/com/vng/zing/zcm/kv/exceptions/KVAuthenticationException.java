package com.vng.zing.zcm.kv.exceptions;

/**
 * Exception thrown when KV authentication fails (401 Unauthorized).
 */
public class KVAuthenticationException extends KVClientException {
  public KVAuthenticationException(String message) {
    super(message);
  }

  public KVAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}

