package com.vng.zing.zcm.kv;

/**
 * Interface for providing JWT access tokens for KV API authentication.
 * <p>
 * Implementations should provide tokens either from the current request context
 * (e.g., pass-through JWT from Spring Security) or from a service account
 * (e.g., client credentials flow).
 */
public interface KVTokenProvider {

  /**
   * Gets an access token for KV API authentication.
   *
   * @return JWT access token string
   * @throws RuntimeException if token cannot be obtained
   */
  String getAccessToken();
}

