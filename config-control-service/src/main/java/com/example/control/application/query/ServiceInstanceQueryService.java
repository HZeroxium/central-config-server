package com.example.control.application.query;

import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.id.ServiceInstanceId;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query service for ServiceInstance read operations.
 * <p>
 * Provides read-only access to ServiceInstance data with caching support.
 * This service depends only on Repository Ports to avoid circular dependencies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceInstanceQueryService {

    private final ServiceInstanceRepositoryPort repository;

    /**
     * Find service instance by ID.
     *
     * @param id the service instance ID
     * @return optional instance
     */
    @Cacheable(value = "service-instances", key = "#id")
    public Optional<ServiceInstance> findById(ServiceInstanceId id) {
        log.debug("Finding service instance by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Find all service instances with filtering and pagination.
     * <p>
     * This method does NOT apply user-based filtering - it returns raw data.
     * Use this for admin operations or when building permission-aware queries.
     *
     * @param criteria optional filter parameters
     * @param pageable pagination information
     * @return page of service instances
     */
    @Cacheable(value = "service-instances", key = "'all:' + #criteria.hashCode() + ':' + #pageable")
    public Page<ServiceInstance> findAll(ServiceInstanceCriteria criteria, Pageable pageable) {
        log.debug("Finding all service instances with criteria: {}", criteria);
        return repository.findAll(criteria, pageable);
    }

    /**
     * Count all service instances.
     *
     * @return total instance count
     */
    @Cacheable(value = "service-instances", key = "'count:all'")
    public long countAll() {
        log.debug("Counting all service instances");
        return repository.countAll();
    }

    /**
     * Count service instances with filtering.
     *
     * @param criteria filter criteria
     * @return count of instances matching criteria
     */
    @Cacheable(value = "service-instances", key = "'count:' + #criteria.hashCode()")
    public long count(ServiceInstanceCriteria criteria) {
        log.debug("Counting service instances with criteria: {}", criteria);
        return repository.count(criteria);
    }
}
