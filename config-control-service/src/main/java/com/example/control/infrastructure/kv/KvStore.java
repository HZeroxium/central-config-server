package com.example.control.kv;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Interface for key-value store operations supporting both Consul KV and etcd backends.
 * Provides a unified API for basic operations, CAS, transactions, watch, locks, and ephemeral keys.
 */
public interface KvStore extends AutoCloseable {

    /**
     * Put a key-value pair with optional CAS (Compare-And-Swap) and TTL.
     *
     * @param key             the key
     * @param value           the value as bytes
     * @param expectedVersion the expected version for CAS (null for unconditional put)
     * @param ttl             time-to-live (null for no expiration)
     * @throws Exception if operation fails
     */
    void put(String key, byte[] value, String expectedVersion, Duration ttl) throws Exception;

    /**
     * Get a key-value pair.
     *
     * @param key the key
     * @return Optional containing the entry if found
     * @throws Exception if operation fails
     */
    Optional<Entry> get(String key) throws Exception;

    /**
     * Delete a key with optional CAS.
     *
     * @param key             the key
     * @param expectedVersion the expected version for CAS (null for unconditional delete)
     * @return true if deleted, false if not found or CAS failed
     * @throws Exception if operation fails
     */
    boolean delete(String key, String expectedVersion) throws Exception;

    /**
     * List keys with a prefix.
     *
     * @param prefix  the key prefix
     * @param limit   maximum number of results (0 for no limit)
     * @param fromKey start from this key (null for beginning)
     * @return list of entries matching the prefix
     * @throws Exception if operation fails
     */
    List<Entry> list(String prefix, int limit, String fromKey) throws Exception;

    /**
     * Execute a transaction with multiple operations.
     *
     * @param ops list of operations to execute atomically
     * @return list of success results for each operation
     * @throws Exception if operation fails
     */
    List<Boolean> txn(List<TxnOp> ops) throws Exception;

    /**
     * Watch for changes to keys with a prefix.
     *
     * @param prefix  the key prefix to watch
     * @param handler callback for handling events
     */
    void watchPrefix(String prefix, WatchHandler handler);

    /**
     * Acquire a distributed lock.
     *
     * @param lockKey the lock key
     * @param ttl     lock TTL to prevent deadlocks
     * @return lock ID for releasing the lock
     * @throws Exception if operation fails
     */
    String acquireLock(String lockKey, Duration ttl) throws Exception;

    /**
     * Release a distributed lock.
     *
     * @param lockKey the lock key
     * @param lockId  the lock ID returned by acquireLock
     * @return true if released successfully
     * @throws Exception if operation fails
     */
    boolean releaseLock(String lockKey, String lockId) throws Exception;

    /**
     * Put an ephemeral key that will be deleted when the session expires.
     *
     * @param key   the key
     * @param value the value as bytes
     * @param ttl   time-to-live for the ephemeral key
     * @return session ID for the ephemeral key
     * @throws Exception if operation fails
     */
    String putEphemeral(String key, byte[] value, Duration ttl) throws Exception;

    @Override
    default void close() throws Exception {
    }

    /**
     * Handler for watch events.
     */
    interface WatchHandler {
        void onPut(Entry e);

        void onDelete(String key, String version);

        void onError(Throwable t);
    }

    /**
     * Represents a key-value entry with metadata.
     */
    record Entry(String key, byte[] value, String version, long createIndex, long modifyIndex) {
    }

    /**
     * Transaction operation.
     */
    record TxnOp(Type type, String key, byte[] value, String expectedVersion, Duration ttl) {
        public enum Type {PUT, DELETE}
    }
}
