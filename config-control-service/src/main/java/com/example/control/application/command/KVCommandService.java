package com.example.control.application.command;

import com.example.control.domain.port.KVStorePort;
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

    /**
     * Put (create or update) a KV entry.
     *
     * @param absoluteKey the absolute key path
     * @param value       the value as raw bytes
     * @param options     write options
     * @return write result
     */
    public KVStorePort.KVWriteResult put(String absoluteKey, byte[] value, KVStorePort.KVWriteOptions options) {
        log.debug("Putting KV entry: {}", absoluteKey);
        return kvStorePort.put(absoluteKey, value, options);
    }

    /**
     * Delete a KV entry.
     *
     * @param absoluteKey the absolute key path
     * @param options     delete options
     * @return delete result
     */
    public KVStorePort.KVDeleteResult delete(String absoluteKey, KVStorePort.KVDeleteOptions options) {
        log.debug("Deleting KV entry: {}", absoluteKey);
        return kvStorePort.delete(absoluteKey, options);
    }
}

