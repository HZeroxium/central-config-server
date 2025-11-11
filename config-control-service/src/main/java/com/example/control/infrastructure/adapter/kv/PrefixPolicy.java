package com.example.control.infrastructure.adapter.kv;

import com.example.control.domain.model.kv.KVPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Policy for managing KV key prefixes per ApplicationService.
 * <p>
 * Ensures proper namespace isolation and path normalization for KV operations.
 * All keys are organized under: apps/{serviceId}/kv/{userPath}
 * </p>
 */
@Slf4j
@Component
public class PrefixPolicy {

    private static final String ROOT_PREFIX_TEMPLATE = "apps/%s/kv";

    /**
     * Build the root prefix for a service.
     *
     * @param serviceId the service ID
     * @return root prefix (e.g., "apps/service-id/kv")
     */
    public String getRootPrefix(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("Service ID cannot be null or blank");
        }
        return String.format(ROOT_PREFIX_TEMPLATE, serviceId);
    }

    /**
     * Build absolute key from service ID and relative path.
     *
     * @param serviceId   the service ID
     * @param relativePath the relative path (will be validated and normalized)
     * @return absolute key (e.g., "apps/service-id/kv/config/db.url")
     */
    public String buildAbsoluteKey(String serviceId, String relativePath) {
        String root = getRootPrefix(serviceId);
        if (relativePath == null || relativePath.isBlank()) {
            return root;
        }

        // Validate and normalize the path
        KVPath path = KVPath.of(relativePath);
        if (path.isEmpty()) {
            return root;
        }

        return root + "/" + path.value();
    }

    /**
     * Extract relative path from absolute key.
     *
     * @param serviceId   the service ID
     * @param absoluteKey the absolute key
     * @return relative path, or null if key doesn't belong to this service
     */
    public String extractRelativePath(String serviceId, String absoluteKey) {
        if (absoluteKey == null) {
            return null;
        }

        String root = getRootPrefix(serviceId);
        if (!absoluteKey.startsWith(root)) {
            log.warn("Absolute key {} does not start with root prefix {}", absoluteKey, root);
            return null;
        }

        String relative = absoluteKey.substring(root.length());
        // Remove leading slash if present
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return relative.isEmpty() ? null : relative;
    }

    /**
     * Build absolute prefix for listing operations.
     *
     * @param serviceId   the service ID
     * @param relativePrefix the relative prefix (can be null/empty for root)
     * @return absolute prefix
     */
    public String buildAbsolutePrefix(String serviceId, String relativePrefix) {
        String root = getRootPrefix(serviceId);
        if (relativePrefix == null || relativePrefix.isBlank()) {
            return root + "/";
        }

        KVPath path = KVPath.of(relativePrefix);
        if (path.isEmpty()) {
            return root + "/";
        }

        return root + "/" + path.value() + "/";
    }
}

