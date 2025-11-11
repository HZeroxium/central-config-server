package com.example.control.domain.model.kv;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a logical list assembled from Consul KV items along with manifest metadata.
 */
public record KVListStructure(
        List<Item> items,
        KVListManifest manifest,
        KVType type
) {
    public KVListStructure {
        items = items == null ? List.of() : List.copyOf(items);
        manifest = manifest == null ? KVListManifest.empty() : manifest;
        type = type == null ? KVType.LIST : type;
    }

    public static KVListStructure empty() {
        return new KVListStructure(Collections.emptyList(), KVListManifest.empty(), KVType.LIST);
    }

    /**
     * Represents a single logical list item.
     *
     * @param id   stable identifier used in manifest ordering
     * @param data structured key-value pairs for the item
     */
    public record Item(String id, Map<String, Object> data) {
        public Item {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("List item id must not be null or blank");
            }
            data = data == null ? Map.of() : Map.copyOf(data);
        }
    }
}


