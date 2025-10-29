package com.example.control.infrastructure.kv.consul;

import com.example.control.application.external.ConsulClient;
import com.example.control.infrastructure.kv.KvStore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consul KV implementation of KvStore interface.
 * Uses Consul's KV API with sessions for locks and ephemeral keys.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsulKvStore implements KvStore {

    private final ConsulClient consulClient;
    private final ExecutorService watchExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentHashMap<String, Boolean> activeWatchers = new ConcurrentHashMap<>();

    @Override
    public void put(String key, byte[] value, String expectedVersion, Duration ttl) throws Exception {
        Long cas = expectedVersion != null ? Long.parseLong(expectedVersion) : null;
        boolean success = consulClient.kvPut(key, value, cas);
        if (!success) {
            throw new IllegalStateException("Failed to put key: " + key +
                    (expectedVersion != null ? " (CAS failed)" : ""));
        }
    }

    @Override
    public Optional<Entry> get(String key) throws Exception {
        Optional<JsonNode> nodeOpt = consulClient.kvGetJson(key);
        if (nodeOpt.isEmpty()) {
            return Optional.empty();
        }

        JsonNode node = nodeOpt.get();
        if (!node.isArray() || node.size() == 0) {
            return Optional.empty();
        }

        JsonNode kv = node.get(0);
        return Optional.of(parseEntry(kv));
    }

    @Override
    public boolean delete(String key, String expectedVersion) throws Exception {
        Long cas = expectedVersion != null ? Long.parseLong(expectedVersion) : null;
        return consulClient.kvDelete(key, cas);
    }

    @Override
    public List<Entry> list(String prefix, int limit, String fromKey) throws Exception {
        List<JsonNode> nodes = consulClient.kvListJson(prefix);
        List<Entry> entries = new ArrayList<>();

        boolean started = fromKey == null;
        for (JsonNode node : nodes) {
            String key = node.get("Key").asText();

            if (!started && key.equals(fromKey)) {
                started = true;
                continue;
            }
            if (!started) {
                continue;
            }

            entries.add(parseEntry(node));

            if (limit > 0 && entries.size() >= limit) {
                break;
            }
        }

        return entries;
    }

    @Override
    public List<Boolean> txn(List<TxnOp> ops) throws Exception {
        // Use Consul's native /v1/txn endpoint for atomic transactions
        List<ConsulClient.TxnOperation> consulOps = new ArrayList<>();

        for (TxnOp op : ops) {
            switch (op.type()) {
                case PUT -> {
                    Long cas = op.expectedVersion() != null ? Long.parseLong(op.expectedVersion()) : null;
                    ConsulClient.TxnOperation consulOp = new ConsulClient.TxnOperation(
                            "set", // verb
                            op.key(), // key
                            op.value(), // value
                            cas, // cas for conditional operations
                            null, // session
                            null, // acquire
                            null // release
                    );
                    consulOps.add(consulOp);
                }
                case DELETE -> {
                    Long cas = op.expectedVersion() != null ? Long.parseLong(op.expectedVersion()) : null;
                    ConsulClient.TxnOperation consulOp = new ConsulClient.TxnOperation(
                            "delete", // verb
                            op.key(), // key
                            null, // value
                            cas, // cas for conditional operations
                            null, // session
                            null, // acquire
                            null // release
                    );
                    consulOps.add(consulOp);
                }
                default -> {
                    log.warn("Unknown transaction operation type: {}", op.type());
                }
            }
        }

        if (consulOps.isEmpty()) {
            return List.of();
        }

        ConsulClient.TxnResult txnResult = consulClient.kvTxn(consulOps);

        List<Boolean> results = new ArrayList<>();
        if (txnResult.success()) {
            // All operations succeeded
            for (int i = 0; i < ops.size(); i++) {
                results.add(true);
            }
        } else {
            // Transaction failed - check individual operation results
            log.error("Consul transaction failed with errors: {}", txnResult.errors());
            for (int i = 0; i < ops.size(); i++) {
                results.add(false);
            }
        }

        return results;
    }

    @Override
    public void watchPrefix(String prefix, WatchHandler handler) {
        String watchKey = prefix;
        if (activeWatchers.putIfAbsent(watchKey, true) != null) {
            log.warn("Watch already active for prefix: {}", prefix);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                long lastIndex = 0;
                while (activeWatchers.containsKey(watchKey)) {
                    try {
                        ConsulClient.WatchResult result = consulClient.kvListBlockingWithIndex(prefix, lastIndex);
                        lastIndex = result.index();

                        if (!result.data().equals("[]")) {
                            // Parse and emit events
                            parseAndEmitWatchEvents(result.data(), handler);
                        }
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                            log.warn("Watch thread interrupted for prefix: {}", prefix);
                            break;
                        }
                        log.error("Watch error for prefix: {}", prefix, e);
                        handler.onError(e);
                        try {
                            Thread.sleep(1000); // Backoff on error
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("Watch thread interrupted during backoff for prefix: {}", prefix);
                            break;
                        }
                    }
                }
            } finally {
                activeWatchers.remove(watchKey);
                log.debug("Watch stopped for prefix: {}", prefix);
            }
        }, watchExecutor);
    }

    @Override
    public String acquireLock(String lockKey, Duration ttl) throws Exception {
        String sessionId = consulClient.createSession(ttl, true);
        String lockValue = "locked-" + System.currentTimeMillis();

        boolean acquired = consulClient.putWithAcquire(lockKey, lockValue.getBytes(), sessionId);
        if (!acquired) {
            consulClient.destroySession(sessionId);
            throw new IllegalStateException("Failed to acquire lock: " + lockKey);
        }

        return sessionId;
    }

    @Override
    public boolean releaseLock(String lockKey, String lockId) throws Exception {
        boolean deleted = consulClient.kvDelete(lockKey, null);
        boolean destroyed = consulClient.destroySession(lockId);
        return deleted && destroyed;
    }

    @Override
    public String putEphemeral(String key, byte[] value, Duration ttl) throws Exception {
        String sessionId = consulClient.createSession(ttl, true);
        boolean success = consulClient.putWithSession(key, value, sessionId);

        if (!success) {
            consulClient.destroySession(sessionId);
            throw new IllegalStateException("Failed to create ephemeral key: " + key);
        }

        return sessionId;
    }

    @Override
    public void close() throws Exception {
        activeWatchers.clear();
        watchExecutor.shutdown();
    }

    /**
     * Parse a Consul KV JSON node into an Entry.
     */
    private Entry parseEntry(JsonNode node) {
        String key = node.get("Key").asText();
        String valueStr = node.get("Value").asText();
        byte[] value = valueStr.isEmpty() ? new byte[0] : Base64.getDecoder().decode(valueStr);
        long createIndex = node.get("CreateIndex").asLong();
        long modifyIndex = node.get("ModifyIndex").asLong();

        return new Entry(key, value, String.valueOf(modifyIndex), createIndex, modifyIndex);
    }

    /**
     * Parse watch data and emit events to handler.
     */
    private void parseAndEmitWatchEvents(String data, WatchHandler handler) {
        try {
            if (data.equals("[]")) {
                return;
            }

            JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(data);
            if (node.isArray()) {
                for (JsonNode kv : node) {
                    String key = kv.get("Key").asText();
                    String valueStr = kv.get("Value").asText();

                    if (valueStr.isEmpty()) {
                        // Key was deleted
                        long modifyIndex = kv.get("ModifyIndex").asLong();
                        handler.onDelete(key, String.valueOf(modifyIndex));
                    } else {
                        // Key was created/updated
                        Entry entry = parseEntry(kv);
                        handler.onPut(entry);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse watch events", e);
            handler.onError(e);
        }
    }
}
