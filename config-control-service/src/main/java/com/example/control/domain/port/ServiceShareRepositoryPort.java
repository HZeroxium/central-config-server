package com.example.control.domain.port;

import com.example.control.domain.ServiceShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ServiceShare}.
 * <p>
 * Provides CRUD operations for service sharing ACL (Access Control List) to allow
 * teams to share specific permissions with other teams or users without granting
 * full team membership.
 * </p>
 */
public interface ServiceShareRepositoryPort {

    /**
     * Persist or update a service share.
     *
     * @param share the service share to save
     * @return the persisted service share
     */
    ServiceShare save(ServiceShare share);

    /**
     * Find a service share by its unique identifier.
     *
     * @param id the share ID
     * @return optional service share
     */
    Optional<ServiceShare> findById(String id);

    /**
     * Find all shares for a specific service.
     *
     * @param serviceId the service ID
     * @return list of shares for the service
     */
    List<ServiceShare> findByService(String serviceId);

    /**
     * Find all shares for a specific grantee (team or user).
     *
     * @param grantToType the type of grantee (TEAM or USER)
     * @param grantToId   the grantee ID
     * @return list of shares for the grantee
     */
    List<ServiceShare> findByGrantee(ServiceShare.GranteeType grantToType, String grantToId);

    /**
     * List service shares with filtering and pagination.
     *
     * @param filter   optional filter parameters
     * @param pageable pagination and sorting information
     * @return a page of service shares
     */
    Page<ServiceShare> list(ServiceShareFilter filter, Pageable pageable);

    /**
     * Delete a service share by ID.
     *
     * @param id the share ID to delete
     */
    void delete(String id);

    /**
     * Check if a specific share exists (for duplicate prevention).
     *
     * @param serviceId   the service ID
     * @param grantToType the grantee type
     * @param grantToId   the grantee ID
     * @param environments optional environment filter
     * @return true if share exists, false otherwise
     */
    boolean existsByServiceAndGranteeAndEnvironments(String serviceId, 
                                                     ServiceShare.GranteeType grantToType, 
                                                     String grantToId, 
                                                     List<String> environments);

    /**
     * Find effective permissions for a user on a service in specific environments.
     *
     * @param userId      the user ID
     * @param userTeamIds the team IDs the user belongs to
     * @param serviceId   the service ID
     * @param environments the environments to check
     * @return list of effective permissions
     */
    List<ServiceShare.SharePermission> findEffectivePermissions(String userId, 
                                                                List<String> userTeamIds, 
                                                                String serviceId, 
                                                                List<String> environments);

    /**
     * Filter object for querying service shares.
     */
    record ServiceShareFilter(
            String serviceId,
            ServiceShare.GranteeType grantToType,
            String grantToId,
            List<String> environments,
            String grantedBy
    ) {}
}
