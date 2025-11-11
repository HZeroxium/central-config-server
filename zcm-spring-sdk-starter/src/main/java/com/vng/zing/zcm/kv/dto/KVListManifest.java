package com.vng.zing.zcm.kv.dto;

import java.util.List;
import java.util.Map;

/**
 * Manifest metadata describing ordering and versioning for a KV list.
 */
public record KVListManifest(
    List<String> order,
    long version,
    String etag,
    Map<String, Object> metadata
) {
  public KVListManifest {
    order = order == null ? List.of() : List.copyOf(order);
    etag = etag == null ? "" : etag;
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  public static KVListManifest empty() {
    return new KVListManifest(List.of(), 0, "", Map.of());
  }
}


