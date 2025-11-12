package com.example.control.infrastructure.seeding.service;

import com.example.control.application.service.KVService;
import com.example.control.domain.model.kv.KVTransactionResponse;
import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for cleaning and seeding KV entries in Consul.
 * <p>
 * Provides operations to clean KV entries for services and seed new entries
 * using KVService APIs (putList, put) for proper transaction handling,
 * automatic flattening, correct flags, and permission checks.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Clean all KV entries for a service (recursive delete)</li>
 * <li>Seed KV entries using KVService APIs (lists, leaves)</li>
 * <li>Handle batch operations efficiently</li>
 * <li>Proper error handling and logging</li>
 * <li>Extract UserContext from SecurityContext for permission checks</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KVSeederService {

    private final KVService kvService;
    private final KVStorePort kvStorePort;
    private final PrefixPolicy prefixPolicy;

    /**
     * Cleans all KV entries for a service.
     * <p>
     * Deletes all keys under the service's KV root prefix using recursive delete.
     * Uses KVService.delete() with empty path and recurse=true.
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

            // List all keys under the prefix to count
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

            // Delete recursively using KVService (with empty relative path = root)
            UserContext userContext = extractUserContext();
            KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder()
                    .recurse(true)
                    .build();

            // Use empty path to delete root (all KV entries for service)
            KVStorePort.KVDeleteResult result = kvService.delete(serviceId, "", deleteOptions, userContext);

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
     * Seeds KV entries for multiple services using structured data.
     *
     * @param kvData structured KV data container
     * @return total number of entries seeded
     */
    @Transactional
    public int seedKVEntriesForServices(MockDataGenerator.KVData kvData) {
        log.info("Seeding KV entries for {} services", 
                Math.max(kvData.leafEntries.size(), kvData.listEntries.size()));

        UserContext userContext = extractUserContext();
        int totalSeeded = 0;

        // Seed leaf entries using KVService.put()
        for (Map.Entry<String, List<KVEntry>> entry : kvData.leafEntries.entrySet()) {
            String serviceId = entry.getKey();
            List<KVEntry> entries = entry.getValue();
            int seeded = seedKVLeafEntries(serviceId, entries, userContext);
            totalSeeded += seeded;
        }

        // Seed list entries using KVService.putList()
        for (Map.Entry<String, List<MockDataGenerator.KVListData>> entry : kvData.listEntries.entrySet()) {
            String serviceId = entry.getKey();
            List<MockDataGenerator.KVListData> listDataList = entry.getValue();
            int seeded = seedKVListEntries(serviceId, listDataList, userContext);
            totalSeeded += seeded;
        }

        log.info("Seeded {} total KV entries across services", totalSeeded);
        return totalSeeded;
    }

    /**
     * Seeds leaf KV entries using KVService.put().
     *
     * @param serviceId service ID
     * @param entries   list of leaf KV entries
     * @param userContext user context for permission checks
     * @return number of entries seeded
     */
    private int seedKVLeafEntries(String serviceId, List<KVEntry> entries, UserContext userContext) {
        if (entries.isEmpty()) {
            return 0;
        }

        log.debug("Seeding {} leaf entries for service: {}", entries.size(), serviceId);
        int seeded = 0;
        int failed = 0;

        for (KVEntry entry : entries) {
            try {
                // Extract relative path from absolute key
                String relativePath = prefixPolicy.extractRelativePath(serviceId, entry.key());
                if (relativePath == null) {
                    log.warn("Could not extract relative path from absolute key: {}", entry.key());
                    failed++;
                    continue;
                }

                KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder()
                        .flags(entry.flags())
                        .build();

                KVStorePort.KVWriteResult result = kvService.put(serviceId, relativePath, entry.value(), writeOptions, userContext);

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

        log.debug("Seeded {} leaf entries for service: {} ({} failed)", seeded, serviceId, failed);
        return seeded;
    }

    /**
     * Seeds list KV entries using KVService.putList().
     *
     * @param serviceId    service ID
     * @param listDataList list of list data with prefixes
     * @param userContext  user context for permission checks
     * @return number of entries seeded (counted as operations, not individual items)
     */
    private int seedKVListEntries(String serviceId, List<MockDataGenerator.KVListData> listDataList,
                                  UserContext userContext) {
        if (listDataList.isEmpty()) {
            return 0;
        }

        log.debug("Seeding {} list entries for service: {}", listDataList.size(), serviceId);
        int seeded = 0;
        int failed = 0;

        for (MockDataGenerator.KVListData listData : listDataList) {
            try {
                KVTransactionResponse response = kvService.putList(
                        serviceId,
                        listData.relativePrefix(),
                        listData.listStructure(),
                        null, // No deletions
                        userContext
                );

                if (response.success()) {
                    // Count as one operation (list contains manifest + items)
                    seeded++;
                } else {
                    failed++;
                    log.warn("Failed to seed KV list: {} for service: {}", listData.relativePrefix(), serviceId);
                }
            } catch (Exception e) {
                failed++;
                log.error("Error seeding KV list: {} for service: {}", listData.relativePrefix(), serviceId, e);
            }
        }

        log.debug("Seeded {} list entries for service: {} ({} failed)", seeded, serviceId, failed);
        return seeded;
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

    /**
     * Extracts UserContext from SecurityContext.
     * <p>
     * Attempts to extract UserContext from the Authentication principal.
     * Falls back to creating UserContext from authorities if principal is not UserContext.
     * </p>
     *
     * @return UserContext extracted from SecurityContext
     * @throws IllegalStateException if SecurityContext has no authentication
     */
    private UserContext extractUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("No authentication found in SecurityContext. " +
                    "Ensure mock SecurityContext is set up before seeding.");
        }

        // Try to extract UserContext from principal
        if (auth.getPrincipal() instanceof UserContext userContext) {
            return userContext;
        }

        // Fallback: create from authorities
        return UserContext.fromAuthorities(auth.getAuthorities().stream()
                .map(GrantedAuthority.class::cast)
                .toList());
    }
}

