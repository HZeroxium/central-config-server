package com.vng.zing.zcm.kv.dto;

import java.util.Base64;

/**
 * Represents a Key-Value entry from the config-control-service.
 * <p>
 * This record matches the structure of {@code KVDtos.EntryResponse} from the
 * config-control-service API. It provides convenient access to the KV entry
 * data including the value in various formats.
 */
public record KVEntry(
    /**
     * Relative path of the key (relative to service root).
     */
    String path,

    /**
     * Value as base64-encoded string.
     */
    String valueBase64,

    /**
     * Modify index for CAS operations.
     */
    Long modifyIndex,

    /**
     * Create index (when key was first created).
     */
    Long createIndex,

    /**
     * Flags (arbitrary metadata).
     */
    Long flags
) {
  /**
   * Gets the value as raw bytes.
   *
   * @return value as byte array, or empty array if valueBase64 is null/empty
   */
  public byte[] getValueAsBytes() {
    if (valueBase64 == null || valueBase64.isEmpty()) {
      return new byte[0];
    }
    return Base64.getDecoder().decode(valueBase64);
  }

  /**
   * Gets the value as a UTF-8 string.
   *
   * @return value as string, or empty string if valueBase64 is null/empty
   */
  public String getValueAsString() {
    byte[] bytes = getValueAsBytes();
    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
  }
}

