package com.vng.zing.zcm.kv.dto;

import java.util.List;

/**
 * Request payload for writing a structured list to the KV store.
 */
public record KVListWriteRequest(
    List<KVListResult.KVListItem> items,
    KVListManifest manifest,
    List<String> deletes
) {
  public KVListWriteRequest {
    items = items == null ? List.of() : List.copyOf(items);
    manifest = manifest == null ? KVListManifest.empty() : manifest;
    deletes = deletes == null ? List.of() : List.copyOf(deletes);
  }

  public static KVListWriteRequest empty() {
    return new KVListWriteRequest(List.of(), KVListManifest.empty(), List.of());
  }
}


