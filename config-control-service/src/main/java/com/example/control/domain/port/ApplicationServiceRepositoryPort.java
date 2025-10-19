package com.example.control.domain.port;

import com.example.control.domain.ApplicationService;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.criteria.ApplicationServiceCriteria;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApplicationService}.
 * <p>
 * Provides CRUD operations and filtering capabilities for application services,
 * which are group-based access controlled metadata.
 * </p>
 */
public interface ApplicationServiceRepositoryPort extends RepositoryPort<ApplicationService, ApplicationServiceId, ApplicationServiceCriteria> {
}
