package com.example.control.infrastructure.cache;

import com.example.control.domain.port.KVStorePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for generating cache keys for KV operations.
 * <p>
 * Generates deterministic cache keys using the format: {@code serviceId:path} or
 * {@code serviceId:prefix:operationType:optionsHash} for different operation types.
 * </p>
 */
@Slf4j
@Component
public class KVCacheKeyGenerator {

    /**
     * Generate cache key for GET operation.
     * <p>
     * Format: {@code serviceId:path}
     * </p>
     *
     * @param serviceId the service ID
     * @param path      the relative path
     * @return cache key
     */
    public static String generateKey(String serviceId, String path) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("Service ID cannot be null or blank");
        }
        String normalizedPath = normalizePath(path);
        return serviceId + ":" + normalizedPath;
    }

    /**
     * Generate cache key for LIST operations (entries or keys).
     * <p>
     * Format: {@code serviceId:prefix:operationType:optionsHash}
     * </p>
     *
     * @param serviceId     the service ID
     * @param prefix        the relative prefix
     * @param operationType either "entries" or "keys"
     * @param options       list options (recurse, keysOnly, separator, etc.)
     * @return cache key
     */
    public static String generateListKey(String serviceId, String prefix, String operationType, KVStorePort.KVListOptions options) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("Service ID cannot be null or blank");
        }
        String normalizedPrefix = normalizePath(prefix);
        String optionsHash = hashOptions(options);
        return String.format("%s:%s:%s:%s", serviceId, normalizedPrefix, operationType, optionsHash);
    }

    /**
     * Generate cache key for VIEW operation.
     * <p>
     * Format: {@code serviceId:prefix:view:format}
     * </p>
     *
     * @param serviceId the service ID
     * @param prefix    the relative prefix
     * @param format    the output format (json, yaml, properties)
     * @return cache key
     */
    public static String generateViewKey(String serviceId, String prefix, String format) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("Service ID cannot be null or blank");
        }
        String normalizedPrefix = normalizePath(prefix);
        return String.format("%s:%s:view:%s", serviceId, normalizedPrefix, format);
    }

    /**
     * Generate cache key for structured list operation.
     * <p>
     * Format: {@code serviceId:prefix:list}
     * </p>
     *
     * @param serviceId the service ID
     * @param prefix    the relative prefix
     * @return cache key
     */
    public static String generateListStructureKey(String serviceId, String prefix) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("Service ID cannot be null or blank");
        }
        String normalizedPrefix = normalizePath(prefix);
        return String.format("%s:%s:list", serviceId, normalizedPrefix);
    }

    /**
     * Generate cache key from absolute key by extracting serviceId and relative path.
     * <p>
     * Attempts to extract serviceId from the absolute key pattern {@code apps/{serviceId}/kv/...}
     * </p>
     *
     * @param absoluteKey the absolute key
     * @return cache key, or null if extraction fails
     */
    public static String generateKeyFromAbsolute(String absoluteKey) {
        if (absoluteKey == null || absoluteKey.isBlank()) {
            return null;
        }

        // Pattern: apps/{serviceId}/kv/{path}
        if (!absoluteKey.startsWith("apps/")) {
            log.warn("Absolute key does not start with 'apps/': {}", absoluteKey);
            return null;
        }

        String remaining = absoluteKey.substring(5); // Remove "apps/"
        int firstSlash = remaining.indexOf('/');
        if (firstSlash < 0) {
            log.warn("Cannot extract serviceId from absolute key: {}", absoluteKey);
            return null;
        }

        String serviceId = remaining.substring(0, firstSlash);
        String kvPart = remaining.substring(firstSlash + 1);

        if (!kvPart.startsWith("kv/")) {
            log.warn("Absolute key does not contain 'kv/' after serviceId: {}", absoluteKey);
            return null;
        }

        String relativePath = kvPart.substring(3); // Remove "kv/"
        return generateKey(serviceId, relativePath);
    }

    /**
     * Generate cache key for LIST operation from absolute prefix.
     * <p>
     * Extracts serviceId and relative prefix from absolute prefix.
     * </p>
     *
     * @param absolutePrefix the absolute prefix
     * @param operationType either "entries" or "keys"
     * @param options       list options
     * @return cache key, or null if extraction fails
     */
    public static String generateListKeyFromAbsolute(String absolutePrefix, String operationType, KVStorePort.KVListOptions options) {
        if (absolutePrefix == null || absolutePrefix.isBlank()) {
            return null;
        }

        // Pattern: apps/{serviceId}/kv/{prefix}/
        String prefix = absolutePrefix;
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        if (!prefix.startsWith("apps/")) {
            log.warn("Absolute prefix does not start with 'apps/': {}", absolutePrefix);
            return null;
        }

        String remaining = prefix.substring(5); // Remove "apps/"
        int firstSlash = remaining.indexOf('/');
        if (firstSlash < 0) {
            log.warn("Cannot extract serviceId from absolute prefix: {}", absolutePrefix);
            return null;
        }

        String serviceId = remaining.substring(0, firstSlash);
        String kvPart = remaining.substring(firstSlash + 1);

        if (!kvPart.startsWith("kv/")) {
            log.warn("Absolute prefix does not contain 'kv/' after serviceId: {}", absolutePrefix);
            return null;
        }

        String relativePrefix = kvPart.substring(3); // Remove "kv/"
        return generateListKey(serviceId, relativePrefix, operationType, options);
    }

    /**
     * Generate all parent prefix keys for cache eviction.
     * <p>
     * For a path like {@code config/db/url}, generates keys for:
     * - {@code config/db/url} (exact key)
     * - {@code config/db} (parent)
     * - {@code config} (grandparent)
     * - {@code } (root, if path has multiple segments)
     * </p>
     *
     * @param serviceId the service ID
     * @param path      the relative path
     * @return list of cache keys to evict (including the key itself and all parent prefixes)
     */
    public static List<String> generateParentPrefixKeys(String serviceId, String path) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("Service ID cannot be null or blank");
        }

        List<String> keys = new ArrayList<>();
        String normalizedPath = normalizePath(path);

        // Add the exact key
        keys.add(generateKey(serviceId, normalizedPath));

        // Generate parent prefixes
        if (normalizedPath != null && !normalizedPath.isEmpty()) {
            String[] segments = normalizedPath.split("/");
            StringBuilder currentPrefix = new StringBuilder();

            for (int i = 0; i < segments.length - 1; i++) {
                if (i > 0) {
                    currentPrefix.append("/");
                }
                currentPrefix.append(segments[i]);
                keys.add(generateKey(serviceId, currentPrefix.toString()));
            }

            // Add root prefix key for list operations
            keys.add(generateKey(serviceId, ""));
        }

        return keys;
    }

    /**
     * Generate cache keys for all parent prefixes (for list operations).
     * <p>
     * Similar to {@link #generateParentPrefixKeys(String, String)} but also includes
     * list operation keys for the prefix and all parent prefixes.
     * </p>
     *
     * @param serviceId the service ID
     * @param prefix    the relative prefix
     * @return list of cache keys to evict
     */
    public static List<String> generatePrefixEvictionKeys(String serviceId, String prefix) {
        List<String> keys = new ArrayList<>();
        String normalizedPrefix = normalizePath(prefix);

        // Add list operation keys for this prefix and all parents
        keys.add(generateListStructureKey(serviceId, normalizedPrefix));
        keys.add(generateViewKey(serviceId, normalizedPrefix, "json"));
        keys.add(generateViewKey(serviceId, normalizedPrefix, "yaml"));
        keys.add(generateViewKey(serviceId, normalizedPrefix, "properties"));

        // Generate parent prefixes
        if (normalizedPrefix != null && !normalizedPrefix.isEmpty()) {
            String[] segments = normalizedPrefix.split("/");
            StringBuilder currentPrefix = new StringBuilder();

            for (int i = 0; i < segments.length; i++) {
                if (i > 0) {
                    currentPrefix.append("/");
                }
                currentPrefix.append(segments[i]);
                String parentPrefix = currentPrefix.toString();
                keys.add(generateListStructureKey(serviceId, parentPrefix));
                keys.add(generateViewKey(serviceId, parentPrefix, "json"));
                keys.add(generateViewKey(serviceId, parentPrefix, "yaml"));
                keys.add(generateViewKey(serviceId, parentPrefix, "properties"));
            }
        }

        return keys;
    }

    /**
     * Normalize path by removing leading/trailing slashes and empty segments.
     *
     * @param path the path to normalize
     * @return normalized path, or empty string for root
     */
    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        // Remove leading and trailing slashes
        String normalized = path.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Generate a hash for list options to include in cache key.
     * <p>
     * Includes: recurse, keysOnly, separator, consistent, stale
     * </p>
     *
     * @param options the list options
     * @return hash string representing the options
     */
    private static String hashOptions(KVStorePort.KVListOptions options) {
        if (options == null) {
            return "default";
        }
        // Create a deterministic string representation
        String optionsStr = String.format("r:%s,ko:%s,sep:%s,c:%s,s:%s",
                options.isRecurse(),
                options.isKeysOnly(),
                options.getSeparator() != null ? options.getSeparator() : "null",
                options.isConsistent(),
                options.isStale());
        // Use simple hash for brevity (first 8 chars of hash code)
        return String.valueOf(Math.abs(optionsStr.hashCode()));
    }
}

