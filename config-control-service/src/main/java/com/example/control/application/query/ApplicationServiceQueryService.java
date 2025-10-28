package com.example.control.application.query;

import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.port.ApplicationServiceRepositoryPort;
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
 * Query service for ApplicationService read operations.
 * <p>
 * Provides read-only access to ApplicationService data with caching support.
 * This service depends only on Repository Ports to avoid circular dependencies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationServiceQueryService {

    private final ApplicationServiceRepositoryPort repository;

    /**
     * Find application service by ID.
     *
     * @param id the service ID
     * @return optional application service
     */
    @Cacheable(value = "application-services", key = "#id")
    public Optional<ApplicationService> findById(ApplicationServiceId id) {
        log.debug("Finding application service by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Find application service by exact display name.
     * <p>
     * This method provides O(1) lookup for service name resolution during heartbeat
     * processing.
     *
     * @param displayName the exact display name to search for
     * @return the application service if found, empty otherwise
     */
    @Cacheable(value = "application-services", key = "'displayName:' + #displayName")
    public Optional<ApplicationService> findByDisplayName(String displayName) {
        log.debug("Finding application service by display name: {}", displayName);
        return repository.findByDisplayName(displayName);
    }

    /**
     * Find application services owned by a specific team.
     *
     * @param ownerTeamId the team ID
     * @return list of services owned by the team
     */
    @Cacheable(value = "application-services", key = "'ownerTeam:' + #ownerTeamId")
    public List<ApplicationService> findByOwnerTeam(String ownerTeamId) {
        log.debug("Finding application services by owner team: {}", ownerTeamId);
        ApplicationServiceCriteria criteria = ApplicationServiceCriteria.builder()
                .ownerTeamId(ownerTeamId)
                .build();
        return repository.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Find all application services with filtering and pagination.
     * <p>
     * This method does NOT apply user-based filtering - it returns raw data.
     * Use this for admin operations or when building permission-aware queries.
     *
     * @param criteria optional filter parameters
     * @param pageable pagination information
     * @return page of application services
     */
    @Cacheable(value = "application-services", key = "'all:' + #criteria.hashCode() + ':' + #pageable")
    public Page<ApplicationService> findAll(ApplicationServiceCriteria criteria, Pageable pageable) {
        log.debug("Finding all application services with criteria: {}", criteria);
        return repository.findAll(criteria, pageable);
    }

    /**
     * Find all application services without filtering.
     *
     * @return list of all application services
     */
    @Cacheable(value = "application-services", key = "'all'")
    public List<ApplicationService> findAll() {
        log.debug("Finding all application services");
        return repository.findAll(null, Pageable.unpaged()).getContent();
    }
}
