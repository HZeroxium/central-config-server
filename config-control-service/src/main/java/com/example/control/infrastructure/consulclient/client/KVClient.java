package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.core.WriteOptions;
import com.example.control.infrastructure.consulclient.model.KVPair;

import java.util.List;
import java.util.Optional;

/**
 * Client for Consul KV operations.
 */
public interface KVClient {

    /**
     * Get a single key.
     *
     * @param key     the key to get
     * @param options query options
     * @return response with optional KVPair
     */
    ConsulResponse<Optional<KVPair>> get(String key, QueryOptions options);

    /**
     * Get a single key with default options.
     *
     * @param key the key to get
     * @return response with optional KVPair
     */
    default ConsulResponse<Optional<KVPair>> get(String key) {
        return get(key, null);
    }

    /**
     * List keys with a prefix.
     *
     * @param prefix  the key prefix
     * @param options query options
     * @return response with list of KVPairs
     */
    ConsulResponse<List<KVPair>> list(String prefix, QueryOptions options);

    /**
     * List keys with a prefix using default options.
     *
     * @param prefix the key prefix
     * @return response with list of KVPairs
     */
    default ConsulResponse<List<KVPair>> list(String prefix) {
        return list(prefix, null);
    }

    /**
     * List only keys (without values) under a prefix.
     *
     * @param prefix  the key prefix
     * @param options query options
     * @return response with list of key paths (strings)
     */
    ConsulResponse<List<String>> listKeys(String prefix, QueryOptions options);

    /**
     * List only keys with default options.
     *
     * @param prefix the key prefix
     * @return response with list of key paths
     */
    default ConsulResponse<List<String>> listKeys(String prefix) {
        return listKeys(prefix, null);
    }

    /**
     * Put a key-value pair.
     *
     * @param key     the key
     * @param value   the value as raw bytes
     * @param options write options
     * @param cas     the expected modify index for CAS operation (null for unconditional)
     * @return response with boolean success
     */
    ConsulResponse<Boolean> put(String key, byte[] value, WriteOptions options, Long cas);

    /**
     * Put a key-value pair with default options.
     *
     * @param key   the key
     * @param value the value as raw bytes
     * @return response with boolean success
     */
    default ConsulResponse<Boolean> put(String key, byte[] value) {
        return put(key, value, null, null);
    }

    /**
     * Put a key-value pair with CAS.
     *
     * @param key   the key
     * @param value the value as raw bytes
     * @param cas   the expected modify index
     * @return response with boolean success
     */
    default ConsulResponse<Boolean> putCas(String key, byte[] value, Long cas) {
        return put(key, value, null, cas);
    }

    /**
     * Delete a key.
     *
     * @param key     the key to delete
     * @param options write options
     * @param cas     the expected modify index for CAS operation (null for unconditional)
     * @return response with boolean success
     */
    ConsulResponse<Boolean> delete(String key, WriteOptions options, Long cas);

    /**
     * Delete a key with default options.
     *
     * @param key the key to delete
     * @return response with boolean success
     */
    default ConsulResponse<Boolean> delete(String key) {
        return delete(key, null, null);
    }

    /**
     * Delete a key with CAS.
     *
     * @param key the key to delete
     * @param cas the expected modify index
     * @return response with boolean success
     */
    default ConsulResponse<Boolean> deleteCas(String key, Long cas) {
        return delete(key, null, cas);
    }

    /**
     * Acquire a lock on a key.
     *
     * @param key       the key to lock
     * @param value     the value to set when acquiring the lock
     * @param sessionId the session ID
     * @param options   write options
     * @return response with boolean success
     */
    ConsulResponse<Boolean> acquire(String key, byte[] value, String sessionId, WriteOptions options);

    /**
     * Acquire a lock on a key with default options.
     *
     * @param key       the key to lock
     * @param value     the value to set when acquiring the lock
     * @param sessionId the session ID
     * @return response with boolean success
     */
    default ConsulResponse<Boolean> acquire(String key, byte[] value, String sessionId) {
        return acquire(key, value, sessionId, null);
    }

    /**
     * Release a lock on a key.
     *
     * @param key       the key to unlock
     * @param value     the value to set when releasing the lock
     * @param sessionId the session ID
     * @param options   write options
     * @return response with boolean success
     */
    ConsulResponse<Boolean> release(String key, byte[] value, String sessionId, WriteOptions options);

    /**
     * Release a lock on a key with default options.
     *
     * @param key       the key to unlock
     * @param value     the value to set when releasing the lock
     * @param sessionId the session ID
     * @return response with boolean success
     */
    default ConsulResponse<Boolean> release(String key, byte[] value, String sessionId) {
        return release(key, value, sessionId, null);
    }
}
