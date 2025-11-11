package com.example.control.domain.model.kv;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an assembled object stored in Consul KV as hierarchical keys.
 * <p>
 * Values are materialized into a nested map that mirrors the logical JSON/YAML
 * structure. Leaf values are represented as {@link String} instances while
 * objects and lists use {@link Map} / {@link java.util.List} respectively.
 * </p>
 */
public record KVObjectStructure(
        Map<String, Object> value,
        KVType type
) {

    public KVObjectStructure {
        value = value == null ? Map.of() : Collections.unmodifiableMap(value);
        type = type == null ? KVType.LEAF : type;
    }

    /**
     * Convenience accessor for retrieving a property by key.
     *
     * @param key property name
     * @return optional value if present
     */
    public Optional<Object> find(String key) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(value.get(key));
    }
}


