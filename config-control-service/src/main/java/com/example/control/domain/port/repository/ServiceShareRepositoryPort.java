package com.example.control.domain.port.repository;

import com.example.control.domain.model.ServiceShare;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.port.RepositoryPort;
import com.example.control.domain.valueobject.id.ServiceShareId;

import java.util.List;

/**
 * Port (hexagonal architecture) for persisting and querying
 * {@link ServiceShare}.
 * <p>
 * Provides CRUD operations for service sharing ACL (Access Control List) to
 * allow
 * teams to share specific permissions with other teams or users without
 * granting
 * full team membership.
 * </p>
 */
public interface ServiceShareRepositoryPort extends RepositoryPort<ServiceShare, ServiceShareId, ServiceShareCriteria> {

    /**
     * Check if a specific share exists (for duplicate prevention).
     *
     * @param serviceId    the service ID
     * @param grantToType  the grantee type
     * @param grantToId    the grantee ID
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
     * @param userId       the user ID
     * @param userTeamIds  the team IDs the user belongs to
     * @param serviceId    the service ID
     * @param environments the environments to check
     * @return list of effective permissions
     */
    List<ServiceShare.SharePermission> findEffectivePermissions(String userId,
                                                                List<String> userTeamIds,
                                                                String serviceId,
                                                                List<String> environments);

    /**
     * Find all service IDs that are shared to specific teams.
     * <p>
     * Used for filtering ApplicationServices to include services shared to user's
     * teams.
     * This enables users to see services shared to their teams in addition to owned
     * services.
     * </p>
     *
     * @param teamIds the team IDs to check (user's team membership)
     * @return list of unique service IDs shared to any of the specified teams
     */
    List<String> findServiceIdsByGranteeTeams(List<String> teamIds);
}
