package com.example.control.domain.model.kv;

import lombok.Getter;

/**
 * Enumerates supported logical types for Consul KV entries.
 * <p>
 * The type is primarily encoded in the Consul {@code Flags} field to avoid
 * creating companion metadata keys. The mapping is:
 * <ul>
 *     <li>{@code 0} - {@link #LEAF}</li>
 *     <li>{@code 2} - {@link #LIST}</li>
 *     <li>{@code 3} - {@link #LEAF_LIST}</li>
 * </ul>
 * </p>
 * <p>
 * Note: Flag value {@code 1} is reserved for future use and will fall back to {@link #LEAF}
 * for backward compatibility with existing Object entries.
 * </p>
 */
@Getter
public enum KVType {
    LEAF(0),
    LIST(2),
    LEAF_LIST(3);

    private final long flagValue;

    KVType(long flagValue) {
        this.flagValue = flagValue;
    }

    /**
     * Resolve the logical type from a Consul KV flags value.
     * <p>
     * The method is lenient and falls back to {@link #LEAF} when the provided
     * flag does not map to a known type to preserve backward compatibility with
     * legacy keys. Flag value 1 (previously OBJECT) is reserved and falls back to LEAF.
     * </p>
     *
     * @param flags Consul KV flags value
     * @return resolved {@link KVType}
     */
    public static KVType fromFlags(long flags) {
        for (KVType value : KVType.values()) {
            if (value.flagValue == flags) {
                return value;
            }
        }
        return LEAF;
    }

    /**
     * Determines whether a flags value explicitly encodes a type.
     *
     * @param flags Consul KV flags value
     * @return {@code true} when the value matches one of the known type flags (LIST or LEAF_LIST)
     */
    public static boolean isTypeFlag(long flags) {
        return flags == LIST.flagValue || flags == LEAF_LIST.flagValue;
    }
}


