package com.example.control.application.query;

import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.port.KVStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Query service for KV read operations.
 * <p>
 * Provides read-only access to KV data. Does NOT handle permission checks
 * or business logic - those are handled by the orchestrator service.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KVQueryService {

    private final KVStorePort kvStorePort;

    /**
     * Get a single KV entry.
     * <p>
     * Cached for stale reads only. Consistent reads bypass cache to ensure freshness.
     * </p>
     *
     * @param absoluteKey the absolute key path
     * @param options     read options
     * @return optional KV entry
     */
    // @Cacheable(
    //         value = "kv-entries",
    //         key = "T(com.example.control.infrastructure.cache.KVCacheKeyGenerator).generateKeyFromAbsolute(#absoluteKey)",
    //         condition = "!#options.consistent && T(com.example.control.infrastructure.cache.KVCacheKeyGenerator).generateKeyFromAbsolute(#absoluteKey) != null"
    // )
    public Optional<KVEntry> get(String absoluteKey, KVStorePort.KVReadOptions options) {
        log.debug("Getting KV entry: {}", absoluteKey);
        return kvStorePort.get(absoluteKey, options);
    }

    /**
     * List all KV entries under a prefix.
     * <p>
     * Cached for stale reads only. Consistent reads bypass cache to ensure freshness.
     * </p>
     *
     * @param prefix  the prefix to list
     * @param options list options
     * @return list of KV entries
     */
    // @Cacheable(
    //         value = "kv-entries",
    //         key = "T(com.example.control.infrastructure.cache.KVCacheKeyGenerator).generateListKeyFromAbsolute(#prefix, 'entries', #options)",
    //         condition = "!#options.consistent && T(com.example.control.infrastructure.cache.KVCacheKeyGenerator).generateListKeyFromAbsolute(#prefix, 'entries', #options) != null"
    // )
    public List<KVEntry> listEntries(String prefix, KVStorePort.KVListOptions options) {
        log.debug("Listing KV entries with prefix: {}", prefix);
        return kvStorePort.listEntries(prefix, options);
    }

    /**
     * List only keys under a prefix.
     * <p>
     * Cached for stale reads only. Consistent reads bypass cache to ensure freshness.
     * </p>
     *
     * @param prefix  the prefix to list
     * @param options list options
     * @return list of key paths
     */
    // @Cacheable(
    //         value = "kv-entries",
    //         key = "T(com.example.control.infrastructure.cache.KVCacheKeyGenerator).generateListKeyFromAbsolute(#prefix, 'keys', #options)",
    //         condition = "!#options.consistent && T(com.example.control.infrastructure.cache.KVCacheKeyGenerator).generateListKeyFromAbsolute(#prefix, 'keys', #options) != null"
    // )
    public List<String> listKeys(String prefix, KVStorePort.KVListOptions options) {
        log.debug("Listing KV keys with prefix: {}", prefix);
        return kvStorePort.listKeys(prefix, options);
    }
}

