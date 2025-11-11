package com.example.control.domain.port;

import com.example.control.domain.model.kv.KVEntry;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for Key-Value store operations.
 * <p>
 * Provides abstraction over Consul KV store, allowing the domain layer
 * to interact with KV operations without depending on Consul-specific details.
 * </p>
 */
public interface KVStorePort {

    /**
     * Get a single key-value entry.
     *
     * @param absoluteKey the absolute key path (e.g., "apps/service-id/kv/config/db.url")
     * @param options     read options (consistency, raw, etc.)
     * @return optional KV entry, empty if not found
     */
    Optional<KVEntry> get(String absoluteKey, KVReadOptions options);

    /**
     * List all key-value entries under a prefix.
     *
     * @param prefix  the prefix to list (e.g., "apps/service-id/kv/config/")
     * @param options list options (recurse, keysOnly, separator, etc.)
     * @return list of KV entries
     */
    List<KVEntry> listEntries(String prefix, KVListOptions options);

    /**
     * List only keys (without values) under a prefix.
     *
     * @param prefix  the prefix to list
     * @param options list options
     * @return list of key paths (relative to prefix)
     */
    List<String> listKeys(String prefix, KVListOptions options);

    /**
     * Put (create or update) a key-value entry.
     *
     * @param absoluteKey the absolute key path
     * @param value       the value as raw bytes
     * @param options     write options (CAS, flags, etc.)
     * @return write result with success status and modify index
     */
    KVWriteResult put(String absoluteKey, byte[] value, KVWriteOptions options);

    /**
     * Delete a key-value entry.
     *
     * @param absoluteKey the absolute key path
     * @param options     delete options (recurse, CAS, etc.)
     * @return delete result with success status
     */
    KVDeleteResult delete(String absoluteKey, KVDeleteOptions options);

    /**
     * Options for read operations.
     */
    @Data
    @Builder
    class KVReadOptions {
        /**
         * Return raw bytes instead of JSON metadata.
         */
        @Builder.Default
        private boolean raw = false;

        /**
         * Use consistent read (query leader).
         */
        @Builder.Default
        private boolean consistent = false;

        /**
         * Use stale read (allow reads from followers).
         */
        @Builder.Default
        private boolean stale = false;
    }

    /**
     * Options for list operations.
     */
    @Data
    @Builder
    class KVListOptions {
        /**
         * Recurse into subdirectories.
         */
        @Builder.Default
        private boolean recurse = true;

        /**
         * Return only keys (not full entries).
         */
        @Builder.Default
        private boolean keysOnly = false;

        /**
         * Separator for directory-like listing (e.g., "/").
         */
        private String separator;

        /**
         * Use consistent read.
         */
        @Builder.Default
        private boolean consistent = false;

        /**
         * Use stale read.
         */
        @Builder.Default
        private boolean stale = false;
    }

    /**
     * Options for write operations.
     */
    @Data
    @Builder
    class KVWriteOptions {
        /**
         * CAS (Compare-And-Set) modify index. Only update if current modify index matches.
         * null means unconditional update.
         */
        private Long cas;

        /**
         * Flags (arbitrary uint64 metadata).
         */
        @Builder.Default
        private long flags = 0;
    }

    /**
     * Options for delete operations.
     */
    @Data
    @Builder
    class KVDeleteOptions {
        /**
         * Recurse delete (delete all keys under prefix).
         */
        @Builder.Default
        private boolean recurse = false;

        /**
         * CAS (Compare-And-Set) modify index. Only delete if current modify index matches.
         * null means unconditional delete.
         */
        private Long cas;
    }

    /**
     * Result of a write operation.
     */
    record KVWriteResult(boolean success, long modifyIndex) {
    }

    /**
     * Result of a delete operation.
     */
    record KVDeleteResult(boolean success) {
    }
}

