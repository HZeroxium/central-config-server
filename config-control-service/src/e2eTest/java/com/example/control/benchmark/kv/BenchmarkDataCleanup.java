package com.example.control.benchmark.kv;

import com.example.control.application.service.KVService;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Utility for cleaning up benchmark test data.
 * <p>
 * Recursively deletes all generated keys after benchmark completion.
 * Handles cleanup errors gracefully to ensure cleanup continues even if some deletes fail.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class BenchmarkDataCleanup {

    private final BenchmarkConfig config;
    private final PrefixPolicy prefixPolicy;

    /**
     * Cleanup data at service layer.
     *
     * @param kvService KV service
     * @param userContext user context
     * @param generatedKeys set of generated keys
     */
    public void cleanupService(KVService kvService, UserContext userContext, Set<String> generatedKeys) {
        log.info("Cleaning up {} keys at service layer...", generatedKeys.size());

        // Delete by prefix (more efficient than individual deletes)
        String rootPrefix = prefixPolicy.getRootPrefix(config.serviceId());
        try {
            KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder()
                    .recurse(true)
                    .build();
            kvService.delete(config.serviceId(), "", deleteOptions, userContext);
            log.info("Cleaned up prefix: {}", rootPrefix);
        } catch (Exception e) {
            log.warn("Failed to cleanup prefix {}: {}", rootPrefix, e.getMessage());
            // Fallback to individual deletes
            cleanupIndividualService(kvService, userContext, generatedKeys);
        }
    }

    /**
     * Cleanup data at port layer.
     *
     * @param kvStorePort KV store port
     * @param generatedKeys set of generated keys
     */
    public void cleanupPort(KVStorePort kvStorePort, Set<String> generatedKeys) {
        log.info("Cleaning up {} keys at port layer...", generatedKeys.size());

        // Delete by prefix (more efficient than individual deletes)
        String rootPrefix = prefixPolicy.getRootPrefix(config.serviceId());
        try {
            KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder()
                    .recurse(true)
                    .build();
            kvStorePort.delete(rootPrefix + "/", deleteOptions);
            log.info("Cleaned up prefix: {}", rootPrefix);
        } catch (Exception e) {
            log.warn("Failed to cleanup prefix {}: {}", rootPrefix, e.getMessage());
            // Fallback to individual deletes
            cleanupIndividualPort(kvStorePort, generatedKeys);
        }
    }

    /**
     * Cleanup individual keys at service layer (fallback).
     */
    private void cleanupIndividualService(KVService kvService, UserContext userContext, Set<String> generatedKeys) {
        int count = 0;
        int errors = 0;
        for (String relativePath : generatedKeys) {
            try {
                KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder().build();
                kvService.delete(config.serviceId(), relativePath, deleteOptions, userContext);
                count++;
                if (count % 100 == 0) {
                    log.debug("Cleaned up {} keys...", count);
                }
            } catch (Exception e) {
                errors++;
                log.debug("Failed to cleanup key {}: {}", relativePath, e.getMessage());
            }
        }
        log.info("Cleaned up {} keys ({} errors)", count, errors);
    }

    /**
     * Cleanup individual keys at port layer (fallback).
     */
    private void cleanupIndividualPort(KVStorePort kvStorePort, Set<String> generatedKeys) {
        int count = 0;
        int errors = 0;
        for (String relativePath : generatedKeys) {
            try {
                String absoluteKey = prefixPolicy.buildAbsoluteKey(config.serviceId(), relativePath);
                KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder().build();
                kvStorePort.delete(absoluteKey, deleteOptions);
                count++;
                if (count % 100 == 0) {
                    log.debug("Cleaned up {} keys...", count);
                }
            } catch (Exception e) {
                errors++;
                log.debug("Failed to cleanup key {}: {}", relativePath, e.getMessage());
            }
        }
        log.info("Cleaned up {} keys ({} errors)", count, errors);
    }
}

