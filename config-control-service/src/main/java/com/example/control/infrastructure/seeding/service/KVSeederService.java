package com.example.control.infrastructure.seeding.service;

import com.example.control.application.command.KVCommandService;
import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for cleaning and seeding KV entries in Consul.
 * <p>
 * Provides operations to clean KV entries for services and seed new entries
 * using KVCommandService and KVStorePort.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Clean all KV entries for a service (recursive delete)</li>
 * <li>Seed KV entries to Consul</li>
 * <li>Handle batch operations efficiently</li>
 * <li>Proper error handling and logging</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KVSeederService {

    private final KVCommandService kvCommandService;
    private final KVStorePort kvStorePort;
    private final PrefixPolicy prefixPolicy;

    /**
     * Cleans all KV entries for a service.
     * <p>
     * Deletes all keys under the service's KV root prefix using recursive delete.
     * </p>
     *
     * @param serviceId service ID
     * @return number of entries deleted (approximate, based on list operation)
     */
    public long cleanKVEntries(String serviceId) {
        log.info("Cleaning KV entries for service: {}", serviceId);

        try {
            String rootPrefix = prefixPolicy.getRootPrefix(serviceId);
            String absolutePrefix = rootPrefix + "/";

            // List all keys under the prefix
            KVStorePort.KVListOptions listOptions = KVStorePort.KVListOptions.builder()
                    .recurse(true)
                    .keysOnly(true)
                    .build();

            List<String> keys = kvStorePort.listKeys(absolutePrefix, listOptions);
            int keyCount = keys.size();

            if (keyCount == 0) {
                log.debug("No KV entries found for service: {}", serviceId);
                return 0;
            }

            // Delete recursively
            KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder()
                    .recurse(true)
                    .build();

            KVStorePort.KVDeleteResult result = kvStorePort.delete(absolutePrefix, deleteOptions);

            if (result.success()) {
                log.info("Cleaned {} KV entries for service: {}", keyCount, serviceId);
                return keyCount;
            } else {
                log.warn("Failed to clean KV entries for service: {}", serviceId);
                return 0;
            }
        } catch (Exception e) {
            log.error("Error cleaning KV entries for service: {}", serviceId, e);
            return 0;
        }
    }

    /**
     * Seeds KV entries for a service.
     * <p>
     * Writes KV entries to Consul. Uses individual puts for leaf entries and
     * batches operations when possible.
     * </p>
     *
     * @param serviceId service ID
     * @param entries   list of KV entries to seed
     * @return number of entries seeded
     */
    public int seedKVEntries(String serviceId, List<KVEntry> entries) {
        log.info("Seeding {} KV entries for service: {}", entries.size(), serviceId);

        if (entries.isEmpty()) {
            return 0;
        }

        int seeded = 0;
        int failed = 0;

        for (KVEntry entry : entries) {
            try {
                KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder()
                        .flags(entry.flags())
                        .build();

                KVStorePort.KVWriteResult result = kvCommandService.put(
                        entry.key(),
                        entry.value(),
                        writeOptions
                );

                if (result.success()) {
                    seeded++;
                } else {
                    failed++;
                    log.warn("Failed to seed KV entry: {}", entry.key());
                }
            } catch (Exception e) {
                failed++;
                log.error("Error seeding KV entry: {}", entry.key(), e);
            }
        }

        log.info("Seeded {} KV entries for service: {} ({} failed)", seeded, serviceId, failed);
        return seeded;
    }

    /**
     * Seeds KV entries for multiple services.
     *
     * @param entriesByService map of service ID to list of KV entries
     * @return total number of entries seeded
     */
    public int seedKVEntriesForServices(Map<String, List<KVEntry>> entriesByService) {
        log.info("Seeding KV entries for {} services", entriesByService.size());

        int totalSeeded = 0;

        for (Map.Entry<String, List<KVEntry>> entry : entriesByService.entrySet()) {
            String serviceId = entry.getKey();
            List<KVEntry> entries = entry.getValue();
            int seeded = seedKVEntries(serviceId, entries);
            totalSeeded += seeded;
        }

        log.info("Seeded {} total KV entries across {} services", totalSeeded, entriesByService.size());
        return totalSeeded;
    }

    /**
     * Cleans KV entries for multiple services.
     *
     * @param serviceIds list of service IDs
     * @return total number of entries deleted
     */
    public long cleanKVForServices(List<String> serviceIds) {
        log.info("Cleaning KV entries for {} services", serviceIds.size());

        long totalDeleted = 0;

        for (String serviceId : serviceIds) {
            long deleted = cleanKVEntries(serviceId);
            totalDeleted += deleted;
        }

        log.info("Cleaned {} total KV entries across {} services", totalDeleted, serviceIds.size());
        return totalDeleted;
    }
}

