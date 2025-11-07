package com.example.control.infrastructure.adapter.kv;

import com.example.control.domain.model.KVEntry;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.consulclient.client.KVClient;
import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.core.WriteOptions;
import com.example.control.infrastructure.consulclient.model.KVPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing KVStorePort using Consul KV client.
 * <p>
 * Maps domain operations to Consul-specific API calls and converts
 * Consul responses to domain models.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsulKVAdapter implements KVStorePort {

    private final KVClient kvClient;

    @Override
    public Optional<KVEntry> get(String absoluteKey, KVReadOptions options) {
        log.debug("Getting KV entry: {} with raw: {}", absoluteKey, options != null && options.isRaw());

        QueryOptions queryOptions = QueryOptions.builder()
                .consistent(options != null && options.isConsistent())
                .stale(options != null && options.isStale())
                .raw(options != null && options.isRaw())
                .build();

        ConsulResponse<Optional<KVPair>> response = kvClient.get(absoluteKey, queryOptions);

        return response.getBody()
                .map(this::toKVEntry);
    }

    @Override
    public List<KVEntry> listEntries(String prefix, KVListOptions options) {
        log.debug("Listing KV entries with prefix: {}", prefix);

        QueryOptions queryOptions = QueryOptions.builder()
                .consistent(options != null && options.isConsistent())
                .stale(options != null && options.isStale())
                .recurse(options == null || options.isRecurse())
                .separator(options != null ? options.getSeparator() : null)
                .build();

        ConsulResponse<List<KVPair>> response = kvClient.list(prefix, queryOptions);

        List<KVPair> pairs = response.getBody();
        if (pairs == null) {
            return List.of();
        }

        return pairs.stream()
                .map(this::toKVEntry)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listKeys(String prefix, KVListOptions options) {
        log.debug("Listing KV keys with prefix: {}", prefix);

        QueryOptions queryOptions = QueryOptions.builder()
                .consistent(options != null && options.isConsistent())
                .stale(options != null && options.isStale())
                .recurse(options == null || options.isRecurse())
                .separator(options != null ? options.getSeparator() : null)
                .build();

        ConsulResponse<List<String>> response = kvClient.listKeys(prefix, queryOptions);

        List<String> keys = response.getBody();
        return keys != null ? keys : List.of();
    }

    @Override
    public KVWriteResult put(String absoluteKey, byte[] value, KVWriteOptions options) {
        log.debug("Putting KV entry: {} with CAS: {}", absoluteKey, options.getCas());

        WriteOptions writeOptions = WriteOptions.builder().build();
        Long cas = options.getCas();
        ConsulResponse<Boolean> response = kvClient.put(absoluteKey, value, writeOptions, cas);

        Boolean success = response.getBody();
        if (Boolean.TRUE.equals(success)) {
            // After successful put, get the entry to retrieve modifyIndex
            // This is a trade-off: we need modifyIndex for CAS operations
            Optional<KVEntry> entry = get(absoluteKey, KVReadOptions.builder().build());
            long modifyIndex = entry.map(KVEntry::modifyIndex).orElse(0L);
            return new KVWriteResult(true, modifyIndex);
        }

        return new KVWriteResult(false, 0L);
    }

    @Override
    public KVDeleteResult delete(String absoluteKey, KVDeleteOptions options) {
        log.debug("Deleting KV entry: {} with recurse: {}, CAS: {}", 
                absoluteKey, options.isRecurse(), options.getCas());

        WriteOptions writeOptions = WriteOptions.builder()
                .recurse(options.isRecurse())
                .build();
        Long cas = options.getCas();
        
        ConsulResponse<Boolean> response = kvClient.delete(absoluteKey, writeOptions, cas);

        Boolean success = response.getBody();
        return new KVDeleteResult(Boolean.TRUE.equals(success));
    }

    /**
     * Convert Consul KVPair to domain KVEntry.
     */
    private KVEntry toKVEntry(KVPair pair) {
        return KVEntry.builder()
                .key(pair.key())
                .value(pair.getValueBytes())
                .modifyIndex(pair.modifyIndex())
                .createIndex(pair.createIndex())
                .flags(pair.flags())
                .lockIndex(pair.lockIndex())
                .session(pair.session())
                .build();
    }

}

