package com.example.control.domain.port.repository;

import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.port.RepositoryPort;
import com.example.control.domain.valueobject.id.ApplicationServiceId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Port (hexagonal architecture) for persisting and querying
 * {@link ApplicationService}.
 * <p>
 * Provides CRUD operations and filtering capabilities for application services,
 * which are group-based access controlled metadata.
 * </p>
 */
public interface ApplicationServiceRepositoryPort
        extends RepositoryPort<ApplicationService, ApplicationServiceId, ApplicationServiceCriteria> {

    /**
     * Find application service by exact display name.
     * <p>
     * This method provides O(1) lookup for service name resolution during heartbeat
     * processing.
     * Used for auto-linking service instances to their corresponding application
     * services.
     *
     * @param displayName the exact display name to search for
     * @return the application service if found, empty otherwise
     */
    Optional<ApplicationService> findByDisplayName(String displayName);

    /**
     * Find application services by display names (batch lookup).
     * <p>
     * Efficiently loads multiple application services in a single query for batch
     * processing.
     * Used during heartbeat batch processing to reduce database queries.
     *
     * @param displayNames set of display names to search for
     * @return list of application services matching the display names
     */
    List<ApplicationService> findByDisplayNames(Set<String> displayNames);
}
