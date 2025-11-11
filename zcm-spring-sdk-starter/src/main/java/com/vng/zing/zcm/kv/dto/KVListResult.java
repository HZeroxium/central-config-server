package com.vng.zing.zcm.kv.dto;

import java.util.List;
import java.util.Map;

/**
 * Structured list read from the KV store.
 */
public record KVListResult(
    List<KVListItem> items,
    KVListManifest manifest,
    String type
) {
  public KVListResult {
    items = items == null ? List.of() : List.copyOf(items);
    manifest = manifest == null ? KVListManifest.empty() : manifest;
    type = type == null ? "LIST" : type;
  }

  public record KVListItem(String id, Map<String, Object> data) {
    public KVListItem {
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("List item id must not be blank");
      }
      data = data == null ? Map.of() : Map.copyOf(data);
    }
  }
}


