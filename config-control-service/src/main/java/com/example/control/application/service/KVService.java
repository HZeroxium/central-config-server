package com.example.control.application.service;

import com.example.control.application.command.KVCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.KVQueryService;
import com.example.control.application.service.kv.KVTransactionService;
import com.example.control.application.service.kv.KVTypeCodec;
import com.example.control.application.service.kv.KVTypeDetector;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVListManifest;
import com.example.control.domain.model.kv.KVListStructure;
import com.example.control.domain.model.kv.KVTransactionOperation;
import com.example.control.domain.model.kv.KVTransactionRequest;
import com.example.control.domain.model.kv.KVTransactionResponse;
import com.example.control.domain.model.kv.KVType;
import com.example.control.domain.port.KVStorePort;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.cache.KVCacheEvictionService;
import com.example.control.infrastructure.config.security.DomainPermissionEvaluator;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrator service for KV operations.
 * <p>
 * Coordinates between Query/Command services, validates ApplicationService exists,
 * checks permissions, and handles prefix normalization.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KVService {

    private final KVQueryService kvQueryService;
    private final KVCommandService kvCommandService;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final DomainPermissionEvaluator permissionEvaluator;
    private final PrefixPolicy prefixPolicy;
    private final KVTypeCodec kvTypeCodec;
    private final KVTypeDetector kvTypeDetector;
    private final KVTransactionService kvTransactionService;
    private final KVCacheEvictionService cacheEvictionService;

    private static final String MANIFEST_KEY = ".manifest";
    private static final String ITEMS_PREFIX = "items";

    /**
     * Get a single KV entry.
     *
     * @param serviceId   the service ID
     * @param path        the relative path
     * @param options     read options
     * @param userContext the current user context
     * @return optional KV entry
     */
    public Optional<KVEntry> get(String serviceId, String path, KVStorePort.KVReadOptions options, UserContext userContext) {
        log.debug("Getting KV entry for service: {}, path: {}", serviceId, path);

        // Validate service exists and user has permission
        validateServiceAccess(serviceId, userContext, false);

        // Build absolute key
        String absoluteKey = prefixPolicy.buildAbsoluteKey(serviceId, path);

        return kvQueryService.get(absoluteKey, options);
    }

    /**
     * Get a logical list represented via manifest + items subtree.
     * <p>
     * Cached for stale reads only. Consistent reads bypass cache to ensure freshness.
     * </p>
     */
    @Cacheable(
            value = "kv-entries",
            key = "T(com.example.control.infrastructure.cache.KVCacheKeyGenerator).generateListStructureKey(#serviceId, #prefix)",
            condition = "!#options.consistent"
    )
    public Optional<KVListStructure> getList(String serviceId,
                                             String prefix,
                                             KVStorePort.KVReadOptions options,
                                             UserContext userContext) {
        log.debug("Getting KV list for service: {}, prefix: {}", serviceId, prefix);

        validateServiceAccess(serviceId, userContext, false);

        // Manifest
        String manifestKey = prefixPolicy.buildAbsoluteKey(serviceId, joinRelative(prefix, MANIFEST_KEY));
        Optional<KVEntry> manifestEntry = kvQueryService.get(manifestKey, options);
        KVListManifest manifest = manifestEntry
                .map(KVEntry::value)
                .map(kvTypeCodec::parseManifest)
                .orElse(KVListManifest.empty());

        String itemsPrefixAbsolute = prefixPolicy.buildAbsolutePrefix(serviceId, joinRelative(prefix, ITEMS_PREFIX));
        List<KVEntry> entries = kvQueryService.listEntries(
                itemsPrefixAbsolute,
                toListOptions(options, false)
        );
        if (entries.isEmpty()) {
            if (manifestEntry.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new KVListStructure(List.of(), manifest, KVType.LIST));
        }

        KVType type = entries.stream()
                .map(kvTypeDetector::fromEntry)
                .filter(t -> t != KVType.LEAF)
                .findFirst()
                .orElse(KVType.LIST);

        List<KVListStructure.Item> items = buildListItems(entries, itemsPrefixAbsolute, manifest);
        return Optional.of(new KVListStructure(items, manifest, type));
    }

    /**
     * List KV entries under a prefix.
     *
     * @param serviceId   the service ID
     * @param prefix      the relative prefix
     * @param options     list options
     * @param userContext the current user context
     * @return list of KV entries
     */
    public List<KVEntry> listEntries(String serviceId, String prefix, KVStorePort.KVListOptions options, UserContext userContext) {
        log.debug("Listing KV entries for service: {}, prefix: {}", serviceId, prefix);

        // Validate service exists and user has permission
        validateServiceAccess(serviceId, userContext, false);

        // Build absolute prefix
        String absolutePrefix = prefixPolicy.buildAbsolutePrefix(serviceId, prefix);

        return kvQueryService.listEntries(absolutePrefix, options);
    }

    /**
     * List only keys under a prefix.
     *
     * @param serviceId   the service ID
     * @param prefix      the relative prefix
     * @param options     list options
     * @param userContext the current user context
     * @return list of key paths (relative to service root)
     */
    public List<String> listKeys(String serviceId, String prefix, KVStorePort.KVListOptions options, UserContext userContext) {
        log.debug("Listing KV keys for service: {}, prefix: {}", serviceId, prefix);

        // Validate service exists and user has permission
        validateServiceAccess(serviceId, userContext, false);

        // Build absolute prefix
        String absolutePrefix = prefixPolicy.buildAbsolutePrefix(serviceId, prefix);

        List<String> absoluteKeys = kvQueryService.listKeys(absolutePrefix, options);

        // Convert absolute keys to relative paths
        return absoluteKeys.stream()
                .map(key -> prefixPolicy.extractRelativePath(serviceId, key))
                .filter(path -> path != null)
                .toList();
    }

    /**
     * Put (create or update) a KV entry.
     *
     * @param serviceId   the service ID
     * @param path        the relative path
     * @param value       the value as raw bytes
     * @param options     write options
     * @param userContext the current user context
     * @return write result
     */
    @Transactional
    public KVStorePort.KVWriteResult put(String serviceId, String path, byte[] value, KVStorePort.KVWriteOptions options, UserContext userContext) {
        log.info("Putting KV entry for service: {}, path: {}", serviceId, path);

        // Validate service exists and user has permission to edit
        validateServiceAccess(serviceId, userContext, true);

        // Build absolute key
        String absoluteKey = prefixPolicy.buildAbsoluteKey(serviceId, path);

        return kvCommandService.put(absoluteKey, value, options);
    }

    /**
     * Delete a KV entry.
     *
     * @param serviceId   the service ID
     * @param path        the relative path
     * @param options     delete options
     * @param userContext the current user context
     * @return delete result
     */
    @Transactional
    public KVStorePort.KVDeleteResult delete(String serviceId, String path, KVStorePort.KVDeleteOptions options, UserContext userContext) {
        log.info("Deleting KV entry for service: {}, path: {}", serviceId, path);

        // Validate service exists and user has permission to edit
        validateServiceAccess(serviceId, userContext, true);

        // Build absolute key
        String absoluteKey = prefixPolicy.buildAbsoluteKey(serviceId, path);

        return kvCommandService.delete(absoluteKey, options);
    }

    /**
     * Execute a transactional batch of KV operations.
     */
    @Transactional
    public KVTransactionResponse executeTransaction(String serviceId,
                                                    KVTransactionRequest request,
                                                    UserContext userContext) {
        validateServiceAccess(serviceId, userContext, true);
        return kvTransactionService.execute(request);
    }

    @Transactional
    public KVTransactionResponse putList(String serviceId,
                                         String prefix,
                                         KVListStructure structure,
                                         List<String> deletes,
                                         UserContext userContext) {
        validateServiceAccess(serviceId, userContext, true);

        List<KVTransactionOperation> operations = new java.util.ArrayList<>();

        // Manifest operation
        byte[] manifestBytes = kvTypeCodec.writeManifest(structure.manifest());
        operations.add(KVTransactionOperation.SetOperation.builder()
                .key(prefixPolicy.buildAbsoluteKey(serviceId, joinRelative(prefix, MANIFEST_KEY)))
                .value(manifestBytes)
                .flags(KVType.LIST.getFlagValue())
                .targetType(KVType.LIST)
                .build());

        // Upsert items
        for (KVListStructure.Item item : structure.items()) {
            String itemPrefix = prefixPolicy.buildAbsolutePrefix(serviceId, joinRelative(joinRelative(prefix, ITEMS_PREFIX), item.id()));
            Map<String, byte[]> flattened = flattenStructure(item.data());
            flattened.forEach((relativePath, bytes) -> operations.add(
                    KVTransactionOperation.SetOperation.builder()
                            .key(itemPrefix + relativePath)
                            .value(bytes)
                            .flags(0L)
                            .targetType(KVType.LEAF)
                            .build()
            ));
        }

        // Deletions
        if (deletes != null && !deletes.isEmpty()) {
            deletes.forEach(id -> operations.addAll(buildDeleteOperationsForItem(serviceId, prefix, id)));
        }

        KVTransactionRequest request = new KVTransactionRequest(serviceId, operations);
        KVTransactionResponse response = kvTransactionService.execute(request);

        // Evict cache for the prefix and all parent prefixes after successful transaction
        // (Transaction service already evicts individual keys, but we also need to evict
        // list structure and view caches for the prefix)
        if (response.success()) {
            cacheEvictionService.evictPrefix(serviceId, prefix);
        }

        return response;
    }

    /**
     * Validate that service exists and user has appropriate permission.
     *
     * @param serviceId   the service ID
     * @param userContext the user context
     * @param requireEdit true if edit permission is required, false for view
     * @return the application service
     * @throws IllegalArgumentException if service not found or permission denied
     */
    private ApplicationService validateServiceAccess(String serviceId, UserContext userContext, boolean requireEdit) {
        Optional<ApplicationService> serviceOpt = applicationServiceQueryService.findById(ApplicationServiceId.of(serviceId));
        if (serviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Application service not found: " + serviceId);
        }

        ApplicationService service = serviceOpt.get();

        // Check permissions
        if (requireEdit) {
            if (!permissionEvaluator.canEditService(userContext, service)) {
                log.warn("User {} attempted to edit KV for service {} without permission", 
                        userContext.getUserId(), serviceId);
                throw new IllegalArgumentException("Application service not found: " + serviceId);
            }
        } else {
            if (!permissionEvaluator.canViewService(userContext, service)) {
                log.warn("User {} attempted to view KV for service {} without permission", 
                        userContext.getUserId(), serviceId);
                throw new IllegalArgumentException("Application service not found: " + serviceId);
            }
        }

        return service;
    }

    private KVStorePort.KVListOptions toListOptions(KVStorePort.KVReadOptions readOptions, boolean keysOnly) {
        return KVStorePort.KVListOptions.builder()
                .recurse(true)
                .keysOnly(keysOnly)
                .consistent(readOptions != null && readOptions.isConsistent())
                .stale(readOptions != null && readOptions.isStale())
                .build();
    }

    private void insertValue(Map<String, Object> root, String path, String value) {
        String[] segments = path.split("/");
        Map<String, Object> current = root;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i == segments.length - 1) {
                current.put(segment, value);
            } else {
                Object nested = current.get(segment);
                if (nested instanceof Map<?, ?> map) {
                    current = castToMap(map);
                } else {
                    Map<String, Object> newMap = new java.util.LinkedHashMap<>();
                    current.put(segment, newMap);
                    current = newMap;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private List<KVListStructure.Item> buildListItems(List<KVEntry> entries,
                                                      String itemsPrefixAbsolute,
                                                      KVListManifest manifest) {
        java.util.Map<String, Map<String, Object>> byId = new java.util.LinkedHashMap<>();
        for (KVEntry entry : entries) {
            String relative = entry.key().substring(itemsPrefixAbsolute.length());
            if (relative.isBlank()) {
                continue;
            }
            String[] parts = relative.split("/", 2);
            if (parts.length < 2) {
                continue;
            }
            String itemId = parts[0];
            String fieldPath = parts[1];
            Map<String, Object> item = byId.computeIfAbsent(itemId, id -> new java.util.LinkedHashMap<>());
            insertValue(item, fieldPath, kvTypeCodec.asString(entry.value()));
        }

        List<String> orderedIds = manifest.order();
        if (!orderedIds.isEmpty()) {
            return orderedIds.stream()
                    .map(id -> toListItem(id, byId.get(id)))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        return byId.entrySet().stream()
                .map(entry -> toListItem(entry.getKey(), entry.getValue()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private KVListStructure.Item toListItem(String id, Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        return new KVListStructure.Item(id, java.util.Collections.unmodifiableMap(data));
    }

    private boolean isManifestKey(String absoluteKey, String serviceId, String relativePrefix) {
        String manifestKey = prefixPolicy.buildAbsoluteKey(serviceId, joinRelative(relativePrefix, MANIFEST_KEY));
        return manifestKey.equals(absoluteKey);
    }

    private boolean isItemsKey(String absoluteKey, String serviceId, String relativePrefix) {
        String itemsPrefix = prefixPolicy.buildAbsolutePrefix(serviceId, joinRelative(relativePrefix, ITEMS_PREFIX));
        return absoluteKey.startsWith(itemsPrefix);
    }

    private String joinRelative(String prefix, String segment) {
        if (prefix == null || prefix.isBlank()) {
            return segment;
        }
        return prefix.endsWith("/") ? prefix + segment : prefix + "/" + segment;
    }

    private Map<String, byte[]> flattenStructure(Map<String, Object> data) {
        Map<String, byte[]> result = new java.util.LinkedHashMap<>();
        if (data == null) {
            return result;
        }
        data.forEach((key, value) -> flattenEntry(key, value, result));
        return result;
    }

    private void flattenEntry(String key, Object value, Map<String, byte[]> target) {
        if (value instanceof Map<?, ?> mapValue) {
            mapValue.forEach((childKey, childValue) -> {
                String nestedKey = joinRelative(key, String.valueOf(childKey));
                flattenEntry(nestedKey, childValue, target);
            });
            return;
        }
        if (value instanceof List<?> listValue) {
            target.put(key, kvTypeCodec.toJsonBytes(listValue));
            return;
        }
        if (value instanceof byte[] bytes) {
            target.put(key, bytes);
            return;
        }
        target.put(key, String.valueOf(value).getBytes(StandardCharsets.UTF_8));
    }

    private List<KVTransactionOperation> buildDeleteOperationsForItem(String serviceId,
                                                                      String prefix,
                                                                      String itemId) {
        String itemPrefix = joinRelative(joinRelative(prefix, ITEMS_PREFIX), itemId);
        String absolutePrefix = prefixPolicy.buildAbsolutePrefix(serviceId, itemPrefix);
        List<KVEntry> existing = kvQueryService.listEntries(
                absolutePrefix,
                KVStorePort.KVListOptions.builder().recurse(true).keysOnly(false).build()
        );
        List<KVTransactionOperation> deletions = new java.util.ArrayList<>();
        for (KVEntry entry : existing) {
            deletions.add(KVTransactionOperation.DeleteOperation.builder()
                    .key(entry.key())
                    .cas(null)
                    .recurse(false)
                    .targetType(KVType.LEAF)
                    .build());
        }
        return deletions;
    }

    /**
     * View prefix as structured document (JSON, YAML, or Properties).
     * <p>
     * Cached for stale reads only. Consistent reads bypass cache to ensure freshness.
     * </p>
     */
    @Cacheable(
            value = "kv-entries",
            key = "T(com.example.control.infrastructure.cache.KVCacheKeyGenerator).generateViewKey(#serviceId, #prefix, #format.name())",
            condition = "!#options.consistent"
    )
    @Transactional(readOnly = true)
    public Optional<byte[]> view(String serviceId,
                                 String prefix,
                                 KVTypeCodec.StructuredFormat format,
                                 KVStorePort.KVReadOptions options,
                                 UserContext userContext) {
        validateServiceAccess(serviceId, userContext, false);

        String absolutePrefix = prefixPolicy.buildAbsolutePrefix(serviceId, prefix);
        List<KVEntry> entries = kvQueryService.listEntries(
                absolutePrefix,
                toListOptions(options, false)
        );
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> data = new java.util.LinkedHashMap<>();
        for (KVEntry entry : entries) {
            String relative = entry.key().substring(absolutePrefix.length());
            if (relative == null || relative.isBlank()) {
                continue;
            }
            data.put(relative, kvTypeCodec.asString(entry.value()));
        }

        byte[] payload = kvTypeCodec.serializeStructuredContent(data, format);
        return Optional.of(payload);
    }
}

