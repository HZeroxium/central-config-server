package com.example.control.application.service;

import com.example.control.application.command.KVCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.KVQueryService;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.KVEntry;
import com.example.control.domain.port.KVStorePort;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.config.security.DomainPermissionEvaluator;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
}

