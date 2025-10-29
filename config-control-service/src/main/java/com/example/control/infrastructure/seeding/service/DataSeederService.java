package com.example.control.infrastructure.seeding.service;

import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.*;
import com.example.control.domain.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for persisting mock data to the database.
 * <p>
 * Provides clean, seed, and combined operations for database seeding with
 * proper transactional boundaries and logging.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Selective cleaning (preserve IAM data)</li>
 * <li>Transactional integrity for data consistency</li>
 * <li>Comprehensive logging for traceability</li>
 * <li>Idempotent operations (clean-then-seed)</li>
 * <li>Mock SecurityContext for audit trail</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSeederService {

    /**
     * Seeder admin user ID for audit trail.
     */
    private static final String SEEDER_ADMIN_USER_ID = "seeder-admin";
    private static final String SEEDER_ADMIN_USERNAME = "seeder-admin";
    private static final String SEEDER_ADMIN_EMAIL = "seeder-admin@system.local";
    private final MockDataGenerator mockDataGenerator;
    // Repository ports for direct access
    private final ApplicationServiceRepositoryPort applicationServiceRepository;
    private final ServiceInstanceRepositoryPort serviceInstanceRepository;
    private final DriftEventRepositoryPort driftEventRepository;
    private final ServiceShareRepositoryPort serviceShareRepository;
    private final ApprovalRequestRepositoryPort approvalRequestRepository;
    private final ApprovalDecisionRepositoryPort approvalDecisionRepository;

    /**
     * Cleans all non-IAM data from the database.
     * <p>
     * Removes all generated data while preserving IAM users and teams
     * managed by Keycloak. Deletion order respects referential integrity:
     * <ol>
     * <li>Approval Decisions</li>
     * <li>Approval Requests</li>
     * <li>Service Shares</li>
     * <li>Drift Events</li>
     * <li>Service Instances</li>
     * <li>Application Services</li>
     * </ol>
     * </p>
     *
     * @return summary of deleted counts
     */
    @Transactional
    public CleanResult cleanAll() {
        log.info("Starting clean operation: removing all non-IAM data...");

        // Setup mock security context for audit trail
        Authentication previousAuth = setupMockSecurityContext();

        try {
            CleanResult result = new CleanResult();

            // Delete in reverse order of dependencies to respect referential integrity
            result.approvalDecisionsDeleted = approvalDecisionRepository.deleteAll();
            log.info("Deleted {} approval decisions", result.approvalDecisionsDeleted);

            result.approvalRequestsDeleted = approvalRequestRepository.deleteAll();
            log.info("Deleted {} approval requests", result.approvalRequestsDeleted);

            result.sharesDeleted = serviceShareRepository.deleteAll();
            log.info("Deleted {} service shares", result.sharesDeleted);

            result.driftEventsDeleted = driftEventRepository.deleteAll();
            log.info("Deleted {} drift events", result.driftEventsDeleted);

            result.instancesDeleted = serviceInstanceRepository.deleteAll();
            log.info("Deleted {} service instances", result.instancesDeleted);

            result.servicesDeleted = applicationServiceRepository.deleteAll();
            log.info("Deleted {} application services", result.servicesDeleted);

            log.info("Clean operation complete. Total deleted: {}", result.getTotalDeleted());

            return result;
        } finally {
            // Restore previous authentication context
            restoreSecurityContext(previousAuth);
        }
    }

    /**
     * Seeds the database with mock data.
     * <p>
     * Generates and persists a complete dataset using MockDataGenerator.
     * Insertion order respects referential integrity:
     * <ol>
     * <li>Application Services</li>
     * <li>Service Instances</li>
     * <li>Drift Events</li>
     * <li>Service Shares</li>
     * <li>Approval Requests</li>
     * <li>Approval Decisions</li>
     * </ol>
     * </p>
     *
     * <p>
     * <strong>Note on Optimistic Locking:</strong> Each entity is saved
     * independently
     * without reusing instances to avoid version conflicts. Approval Requests are
     * saved
     * before Approval Decisions, and each entity maintains its own version state.
     * </p>
     *
     * @return summary of seeded counts
     */
    @Transactional
    public SeedResult seed() {
        log.info("Starting seed operation: generating and persisting mock data...");

        // Setup mock security context for audit trail
        Authentication previousAuth = setupMockSecurityContext();

        try {
            // Generate all mock data
            MockDataGenerator.GeneratedData data = mockDataGenerator.generateAll();

            SeedResult result = new SeedResult();

            // Persist in order of dependencies

            // 1. Application Services
            log.info("Persisting {} application services...", data.services.size());
            for (ApplicationService service : data.services) {
                applicationServiceRepository.save(service);
            }
            result.servicesSeeded = data.services.size();
            log.info("Persisted {} application services", result.servicesSeeded);

            // 2. Service Instances
            log.info("Persisting {} service instances...", data.instances.size());
            for (ServiceInstance instance : data.instances) {
                serviceInstanceRepository.save(instance);
            }
            result.instancesSeeded = data.instances.size();
            log.info("Persisted {} service instances", result.instancesSeeded);

            // 3. Drift Events
            log.info("Persisting {} drift events...", data.driftEvents.size());
            for (DriftEvent driftEvent : data.driftEvents) {
                driftEventRepository.save(driftEvent);
            }
            result.driftEventsSeeded = data.driftEvents.size();
            log.info("Persisted {} drift events", result.driftEventsSeeded);

            // 4. Service Shares
            log.info("Persisting {} service shares...", data.shares.size());
            for (ServiceShare share : data.shares) {
                serviceShareRepository.save(share);
            }
            result.sharesSeeded = data.shares.size();
            log.info("Persisted {} service shares", result.sharesSeeded);

            // 5. Approval Requests
            log.info("Persisting {} approval requests...", data.approvalRequests.size());
            for (ApprovalRequest request : data.approvalRequests) {
                // Each ApprovalRequest is a fresh entity with version=0
                // Save returns a new instance with version=1, but we don't reuse it
                approvalRequestRepository.save(request);
            }
            result.approvalRequestsSeeded = data.approvalRequests.size();
            log.info("Persisted {} approval requests", result.approvalRequestsSeeded);

            // 6. Approval Decisions
            log.info("Persisting {} approval decisions...", data.approvalDecisions.size());
            for (ApprovalDecision decision : data.approvalDecisions) {
                // ApprovalDecisions are independent entities that reference ApprovalRequests
                // They don't modify the ApprovalRequest's version
                approvalDecisionRepository.save(decision);
            }
            result.approvalDecisionsSeeded = data.approvalDecisions.size();
            log.info("Persisted {} approval decisions", result.approvalDecisionsSeeded);

            log.info("Seed operation complete. Total seeded: {}", result.getTotalSeeded());

            return result;
        } finally {
            // Restore previous authentication context
            restoreSecurityContext(previousAuth);
        }
    }

    /**
     * Cleans and then seeds the database in a single transaction.
     * <p>
     * This is the idempotent operation that ensures a consistent starting state
     * before seeding. Recommended for development and testing workflows.
     * </p>
     *
     * @return combined result with clean and seed summaries
     */
    @Transactional
    public CombinedResult cleanAndSeed() {
        log.info("Starting clean-and-seed operation...");

        CombinedResult result = new CombinedResult();

        // Clean first
        result.cleanResult = cleanAll();

        // Then seed
        result.seedResult = seed();

        log.info("Clean-and-seed operation complete. Deleted: {}, Seeded: {}",
                result.cleanResult.getTotalDeleted(), result.seedResult.getTotalSeeded());

        return result;
    }

    /**
     * Sets up a mock SecurityContext with seeder-admin user for audit trail.
     * <p>
     * This allows MongoDB audit fields (@CreatedBy, @LastModifiedBy) to be
     * populated with a meaningful user ID instead of "system".
     * </p>
     *
     * @return previous Authentication object (may be null) to restore later
     */
    private Authentication setupMockSecurityContext() {
        Authentication previous = SecurityContextHolder.getContext().getAuthentication();

        // Create mock UserContext for seeder-admin
        UserContext seederAdmin = UserContext.builder()
                .userId(SEEDER_ADMIN_USER_ID)
                .username(SEEDER_ADMIN_USERNAME)
                .email(SEEDER_ADMIN_EMAIL)
                .teamIds(List.of()) // No team association
                .roles(List.of("ROLE_SYS_ADMIN")) // Admin role for seeding operations
                .build();

        // Create authentication token
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                seederAdmin,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SYS_ADMIN")));

        // Set in security context
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        log.debug("Mock SecurityContext setup with user: {}", SEEDER_ADMIN_USER_ID);

        return previous;
    }

    /**
     * Restores the previous SecurityContext after seeding operations.
     *
     * @param previousAuth previous Authentication object to restore (may be null)
     */
    private void restoreSecurityContext(Authentication previousAuth) {
        if (previousAuth != null) {
            SecurityContextHolder.getContext().setAuthentication(previousAuth);
            log.debug("SecurityContext restored to previous authentication");
        } else {
            SecurityContextHolder.clearContext();
            log.debug("SecurityContext cleared after seeding");
        }
    }

    /**
     * Result of clean operation.
     */
    public static class CleanResult {
        public long servicesDeleted;
        public long instancesDeleted;
        public long driftEventsDeleted;
        public long sharesDeleted;
        public long approvalRequestsDeleted;
        public long approvalDecisionsDeleted;

        public long getTotalDeleted() {
            return servicesDeleted + instancesDeleted + driftEventsDeleted +
                    sharesDeleted + approvalRequestsDeleted + approvalDecisionsDeleted;
        }
    }

    /**
     * Result of seed operation.
     */
    public static class SeedResult {
        public int servicesSeeded;
        public int instancesSeeded;
        public int driftEventsSeeded;
        public int sharesSeeded;
        public int approvalRequestsSeeded;
        public int approvalDecisionsSeeded;

        public int getTotalSeeded() {
            return servicesSeeded + instancesSeeded + driftEventsSeeded +
                    sharesSeeded + approvalRequestsSeeded + approvalDecisionsSeeded;
        }
    }

    /**
     * Combined result of clean-and-seed operation.
     */
    public static class CombinedResult {
        public CleanResult cleanResult;
        public SeedResult seedResult;

        public String getSummary() {
            return String.format("Deleted %d entities, Seeded %d entities",
                    cleanResult.getTotalDeleted(), seedResult.getTotalSeeded());
        }
    }
}
