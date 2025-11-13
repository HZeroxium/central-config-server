package com.example.control.application.command;

import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.cache.KVCacheEvictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command service for KV write operations.
 * <p>
 * Handles write operations (PUT, DELETE). Does NOT handle permission checks
 * or business logic - those are handled by the orchestrator service.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KVCommandService {

    private final KVStorePort kvStorePort;
    private final KVCacheEvictionService cacheEvictionService;
    private final PrefixPolicy prefixPolicy;

    /**
     * Put (create or update) a KV entry.
     * <p>
     * Evicts cache for the key and all parent prefixes after successful write.
     * </p>
     *
     * @param absoluteKey the absolute key path
     * @param value       the value as raw bytes
     * @param options     write options
     * @return write result
     */
    public KVStorePort.KVWriteResult put(String absoluteKey, byte[] value, KVStorePort.KVWriteOptions options) {
        log.debug("Putting KV entry: {}", absoluteKey);
        KVStorePort.KVWriteResult result = kvStorePort.put(absoluteKey, value, options);

        // Evict cache after successful write
        if (result.success()) {
            evictCacheForAbsoluteKey(absoluteKey);
        }

        return result;
    }

    /**
     * Delete a KV entry.
     * <p>
     * Evicts cache for the key and all parent prefixes after successful delete.
     * </p>
     *
     * @param absoluteKey the absolute key path
     * @param options     delete options
     * @return delete result
     */
    public KVStorePort.KVDeleteResult delete(String absoluteKey, KVStorePort.KVDeleteOptions options) {
        log.debug("Deleting KV entry: {}", absoluteKey);
        KVStorePort.KVDeleteResult result = kvStorePort.delete(absoluteKey, options);

        // Evict cache after successful delete
        if (result.success()) {
            evictCacheForAbsoluteKey(absoluteKey);
        }

        return result;
    }

    /**
     * Evict cache for an absolute key by extracting serviceId and relative path.
     *
     * @param absoluteKey the absolute key
     */
    private void evictCacheForAbsoluteKey(String absoluteKey) {
        try {
            // Extract serviceId from absolute key
            String serviceId = extractServiceIdFromAbsolute(absoluteKey);
            if (serviceId == null) {
                log.warn("Cannot extract serviceId from absolute key for cache eviction: {}", absoluteKey);
                return;
            }

            // Extract relative path
            String relativePath = prefixPolicy.extractRelativePath(serviceId, absoluteKey);
            if (relativePath == null) {
                log.warn("Cannot extract relative path from absolute key for cache eviction: {}", absoluteKey);
                return;
            }

            // Evict cache
            cacheEvictionService.evictKeyAndParents(serviceId, relativePath);
        } catch (Exception e) {
            log.warn("Failed to evict cache for absolute key: {}", absoluteKey, e);
        }
    }

    /**
     * Extract serviceId from absolute key.
     * Pattern: apps/{serviceId}/kv/...
     *
     * @param absoluteKey the absolute key
     * @return serviceId, or null if extraction fails
     */
    private String extractServiceIdFromAbsolute(String absoluteKey) {
        if (absoluteKey == null || !absoluteKey.startsWith("apps/")) {
            return null;
        }

        String remaining = absoluteKey.substring(5); // Remove "apps/"
        int firstSlash = remaining.indexOf('/');
        if (firstSlash < 0) {
            return null;
        }

        return remaining.substring(0, firstSlash);
    }
}

