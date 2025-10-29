package com.example.control.kv.etcd;

import com.example.control.kv.KvProperties;
import com.example.control.kv.KvStore;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class EtcdKvStore implements KvStore {

    private final KvProperties.Etcd etcdProps;
    private final EtcdClients etcdClients;
    private final ExecutorService watchExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void put(String key, byte[] value, String expectedVersion, Duration ttl) throws Exception {
        String fullKey = buildKey(key);
        ByteSequence keySeq = ByteSequence.from(fullKey, StandardCharsets.UTF_8);
        ByteSequence valueSeq = ByteSequence.from(value);

        // For now, ignore TTL and expectedVersion for simplicity
        // TODO: Implement proper TTL and CAS support
        PutResponse response = etcdClients.getKv()
                .put(keySeq, valueSeq)
                .get();

        log.debug("Put key {} with revision {}", key, response.getHeader().getRevision());
    }

    @Override
    public Optional<Entry> get(String key) throws Exception {
        String fullKey = buildKey(key);
        ByteSequence keySeq = ByteSequence.from(fullKey, StandardCharsets.UTF_8);

        GetResponse response = etcdClients.getKv()
                .get(keySeq)
                .get();

        if (response.getKvs().isEmpty()) {
            return Optional.empty();
        }

        KeyValue kv = response.getKvs().get(0);
        return Optional.of(toEntry(kv, fullKey));
    }

    @Override
    public boolean delete(String key, String expectedVersion) throws Exception {
        String fullKey = buildKey(key);
        ByteSequence keySeq = ByteSequence.from(fullKey, StandardCharsets.UTF_8);

        // For now, ignore expectedVersion for simplicity
        // TODO: Implement proper CAS support
        io.etcd.jetcd.kv.DeleteResponse response = etcdClients.getKv()
                .delete(keySeq)
                .get();

        return response.getDeleted() > 0;
    }

    @Override
    public List<Entry> list(String prefix, int limit, String fromKey) throws Exception {
        String fullPrefix = buildKey(prefix);
        ByteSequence prefixSeq = ByteSequence.from(fullPrefix, StandardCharsets.UTF_8);

        GetOption.Builder getOptionBuilder = GetOption.newBuilder()
                .withPrefix(prefixSeq);

        if (limit > 0) {
            getOptionBuilder.withLimit(limit);
        }

        GetResponse response = etcdClients.getKv()
                .get(prefixSeq, getOptionBuilder.build())
                .get();

        List<Entry> entries = response.getKvs().stream()
                .map(kv -> toEntry(kv, kv.getKey().toString(StandardCharsets.UTF_8)))
                .collect(Collectors.toList());

        // Simple in-memory filtering for fromKey
        if (fromKey != null) {
            String fullFromKey = buildKey(fromKey);
            entries = entries.stream()
                    .filter(entry -> entry.key().compareTo(fullFromKey) > 0)
                    .collect(Collectors.toList());
        }

        return entries;
    }

    @Override
    public List<Boolean> txn(List<TxnOp> ops) throws Exception {
        // For now, execute operations sequentially
        // TODO: Implement proper etcd transactions
        List<Boolean> results = new java.util.ArrayList<>();

        for (TxnOp op : ops) {
            try {
                switch (op.type()) {
                    case PUT -> {
                        put(op.key(), op.value(), op.expectedVersion(), op.ttl());
                        results.add(true);
                    }
                    case DELETE -> {
                        boolean deleted = delete(op.key(), op.expectedVersion());
                        results.add(deleted);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to execute operation: {}", op, e);
                results.add(false);
            }
        }

        return results;
    }

    @Override
    public void watchPrefix(String prefix, WatchHandler handler) {
        watchExecutor.submit(() -> {
            String fullPrefix = buildKey(prefix);
            ByteSequence prefixSeq = ByteSequence.from(fullPrefix, StandardCharsets.UTF_8);

            Watch.Listener listener = new Watch.Listener() {
                @Override
                public void onNext(io.etcd.jetcd.watch.WatchResponse response) {
                    for (WatchEvent event : response.getEvents()) {
                        String key = event.getKeyValue().getKey().toString(StandardCharsets.UTF_8);
                        String version = String.valueOf(event.getKeyValue().getModRevision());

                        switch (event.getEventType()) {
                            case PUT -> {
                                Entry entry = toEntry(event.getKeyValue(), key);
                                handler.onPut(entry);
                            }
                            case DELETE -> handler.onDelete(key, version);
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("Error during etcd watch for prefix {}", prefix, throwable);
                    handler.onError(throwable);
                }

                @Override
                public void onCompleted() {
                    log.debug("etcd watch completed for prefix {}", prefix);
                }
            };

            WatchOption watchOption = WatchOption.newBuilder()
                    .withPrefix(prefixSeq)
                    .build();

            Watch.Watcher watcher = etcdClients.getWatch()
                    .watch(prefixSeq, watchOption, listener);

            try {
                // Keep the watcher alive
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                watcher.close();
            }
        });
    }

    @Override
    public String acquireLock(String lockKey, Duration ttl) throws Exception {
        // For now, use a simple approach without etcd locks
        // TODO: Implement proper etcd distributed locks
        String lockValue = "locked-" + System.currentTimeMillis();
        String lockId = Base64.getEncoder().encodeToString(lockValue.getBytes());

        try {
            put(lockKey, lockValue.getBytes(), null, ttl);
            return lockId;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to acquire lock: " + lockKey, e);
        }
    }

    @Override
    public boolean releaseLock(String lockKey, String lockId) throws Exception {
        // For now, just delete the lock key
        // TODO: Implement proper lock validation
        try {
            return delete(lockKey, null);
        } catch (Exception e) {
            log.error("Failed to release lock {}", lockKey, e);
            return false;
        }
    }

    @Override
    public String putEphemeral(String key, byte[] value, Duration ttl) throws Exception {
        // For now, just put the key without lease
        // TODO: Implement proper etcd leases
        try {
            put(key, value, null, ttl);
            return "ephemeral-" + System.currentTimeMillis();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create ephemeral key: " + key, e);
        }
    }

    @Override
    public void close() throws Exception {
        watchExecutor.shutdownNow();
        etcdClients.close();
    }

    private String buildKey(String key) {
        if (etcdProps.getNamespace() == null || etcdProps.getNamespace().isEmpty()) {
            return key;
        }
        return etcdProps.getNamespace() + "/" + key;
    }

    private Entry toEntry(KeyValue kv, String key) {
        byte[] value = kv.getValue().getBytes();
        String version = String.valueOf(kv.getModRevision());
        long createIndex = kv.getCreateRevision();
        long modifyIndex = kv.getModRevision();

        return new Entry(key, value, version, createIndex, modifyIndex);
    }
}