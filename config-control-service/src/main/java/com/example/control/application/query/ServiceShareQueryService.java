package com.example.control.application.query;

import com.example.control.domain.object.ServiceShare;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.id.ServiceShareId;
import com.example.control.domain.port.ServiceShareRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Query service for ServiceShare read operations.
 * <p>
 * Provides read-only access to ServiceShare data with caching support.
 * This service depends only on Repository Ports to avoid circular dependencies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceShareQueryService {

    private final ServiceShareRepositoryPort repository;

    /**
     * Find service share by ID.
     *
     * @param id the share ID
     * @return optional service share
     */
    @Cacheable(value = "service-shares", key = "#id")
    public Optional<ServiceShare> findById(ServiceShareId id) {
        log.debug("Finding service share by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Find all service shares with filtering and pagination.
     * <p>
     * This method does NOT apply user-based filtering - it returns raw data.
     * Use this for admin operations or when building permission-aware queries.
     *
     * @param criteria optional filter parameters
     * @param pageable pagination information
     * @return page of service shares
     */
    @Cacheable(value = "service-shares", key = "'all:' + #criteria.hashCode() + ':' + #pageable")
    public Page<ServiceShare> findAll(ServiceShareCriteria criteria, Pageable pageable) {
        log.debug("Finding all service shares with criteria: {}", criteria);
        return repository.findAll(criteria, pageable);
    }

    /**
     * Find effective permissions for a user on a service in specific environments.
     *
     * @param userId       the user ID
     * @param userTeamIds  the team IDs the user belongs to
     * @param serviceId    the service ID
     * @param environments the environments to check
     * @return list of effective permissions
     */
    @Cacheable(value = "service-shares", key = "'permissions:' + #userId + ':' + #serviceId + ':' + #environments.hashCode()")
    public List<ServiceShare.SharePermission> findEffectivePermissions(String userId,
                                                                       List<String> userTeamIds,
                                                                       String serviceId,
                                                                       List<String> environments) {
        log.debug("Finding effective permissions for user: {} on service: {} in environments: {}",
                userId, serviceId, environments);

        List<ServiceShare.SharePermission> permissions = repository.findEffectivePermissions(
                userId, userTeamIds, serviceId, environments);

        log.debug("Found {} effective permissions for user: {} on service: {}",
                permissions.size(), userId, serviceId);

        return permissions;
    }

    /**
     * Check if a specific share exists (for duplicate prevention).
     *
     * @param serviceId    the service ID
     * @param grantToType  the grantee type
     * @param grantToId    the grantee ID
     * @param environments optional environment filter
     * @return true if share exists, false otherwise
     */
    @Cacheable(value = "service-shares", key = "'exists:' + #serviceId + ':' + #grantToType + ':' + #grantToId + ':' + #environments.hashCode()")
    public boolean existsByServiceAndGranteeAndEnvironments(String serviceId,
                                                            ServiceShare.GranteeType grantToType,
                                                            String grantToId,
                                                            List<String> environments) {
        log.debug("Checking if share exists for service: {} to {}:{} in environments: {}",
                serviceId, grantToType, grantToId, environments);

        boolean exists = repository.existsByServiceAndGranteeAndEnvironments(
                serviceId, grantToType, grantToId, environments);

        log.debug("Share exists: {}", exists);
        return exists;
    }
}
