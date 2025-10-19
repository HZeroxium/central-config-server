package com.example.control.domain.port;

import com.example.control.domain.ApplicationService;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.criteria.ApplicationServiceCriteria;

import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApplicationService}.
 * <p>
 * Provides CRUD operations and filtering capabilities for application services,
 * which are group-based access controlled metadata.
 * </p>
 */
public interface ApplicationServiceRepositoryPort extends RepositoryPort<ApplicationService, ApplicationServiceId, ApplicationServiceCriteria> {

    /**
     * Find application service by exact display name.
     * <p>
     * This method provides O(1) lookup for service name resolution during heartbeat processing.
     * Used for auto-linking service instances to their corresponding application services.
     *
     * @param displayName the exact display name to search for
     * @return the application service if found, empty otherwise
     */
    Optional<ApplicationService> findByDisplayName(String displayName);
}
