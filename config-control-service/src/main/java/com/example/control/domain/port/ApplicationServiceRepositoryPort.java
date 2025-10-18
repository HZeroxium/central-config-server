package com.example.control.domain.port;

import com.example.control.domain.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApplicationService}.
 * <p>
 * Provides CRUD operations and filtering capabilities for application services,
 * which are group-based access controlled metadata.
 * </p>
 */
public interface ApplicationServiceRepositoryPort extends RepositoryPort<ApplicationService, String> {

    /**
     * Delete an application service by ID.
     *
     * @param id the service ID to delete
     */
    void deleteById(String id);

    /**
     * Filter object for querying application services.
     */
    record ApplicationServiceFilter(
            String ownerTeamId,
            ApplicationService.ServiceLifecycle lifecycle,
            List<String> tags,
            String search,
            List<String> userTeamIds
    ) {}
}
