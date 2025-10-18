package com.example.control.domain.port;

import com.example.control.domain.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link ApplicationService}.
 * <p>
 * Provides CRUD operations and filtering capabilities for application services,
 * which are public metadata that can be viewed by all users.
 * </p>
 */
public interface ApplicationServiceRepositoryPort {

    /**
     * Persist or update an application service.
     *
     * @param service the application service to save
     * @return the persisted application service
     */
    ApplicationService save(ApplicationService service);

    /**
     * Find an application service by its unique identifier.
     *
     * @param id the service ID (slug)
     * @return optional application service
     */
    Optional<ApplicationService> findById(String id);

    /**
     * Find all application services.
     *
     * @return list of all application services
     */
    java.util.List<ApplicationService> findAll();

    /**
     * Find application services owned by a specific team.
     *
     * @param ownerTeamId the team ID
     * @return list of services owned by the team
     */
    java.util.List<ApplicationService> findByOwnerTeam(String ownerTeamId);

    /**
     * List application services with filtering and pagination.
     *
     * @param filter   optional filter parameters
     * @param pageable pagination and sorting information
     * @return a page of application services
     */
    Page<ApplicationService> list(ApplicationServiceFilter filter, Pageable pageable);

    /**
     * Delete an application service by ID.
     *
     * @param id the service ID to delete
     */
    void delete(String id);

    /**
     * Count application services by owner team.
     *
     * @param ownerTeamId the team ID
     * @return number of services owned by the team
     */
    long countByOwnerTeam(String ownerTeamId);

    /**
     * Filter object for querying application services.
     */
    record ApplicationServiceFilter(
            String ownerTeamId,
            ApplicationService.ServiceLifecycle lifecycle,
            List<String> tags,
            String search
    ) {}
}
