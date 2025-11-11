package com.example.control.domain.model.kv;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents metadata describing a logical list stored within Consul KV.
 * <p>
 * The manifest is persisted as a JSON blob alongside the list items at
 * {@code {prefix}/.manifest}. It captures ordering and versioning information
 * required to reconstruct the list deterministically across clients.
 * </p>
 */
public record KVListManifest(
        List<String> order,
        long version,
        String etag,
        Map<String, Object> metadata
) {

    public KVListManifest {
        order = order == null ? List.of() : List.copyOf(order);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
        etag = etag == null ? "" : etag;
    }

    /**
     * Convenience factory for an empty manifest.
     *
     * @return manifest with no ordering information
     */
    public static KVListManifest empty() {
        return new KVListManifest(List.of(), 0, "", Map.of());
    }

    /**
     * Creates a new manifest with incremented version and optional new etag.
     *
     * @param newOrder ordered list of item identifiers
     * @param current  existing manifest
     * @param newEtag  new etag value (optional)
     * @return updated manifest instance
     */
    public static KVListManifest withOrder(List<String> newOrder, KVListManifest current, String newEtag) {
        long nextVersion = current == null ? 1 : current.version + 1;
        String etagValue = newEtag != null ? newEtag : (current == null ? "" : current.etag);
        return new KVListManifest(newOrder, nextVersion, etagValue, current == null ? Map.of() : current.metadata);
    }
}


