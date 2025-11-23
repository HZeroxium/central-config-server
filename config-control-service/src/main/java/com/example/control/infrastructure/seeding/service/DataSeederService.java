package com.example.control.infrastructure.seeding.service;

import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.port.repository.*;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.model.*;
import com.example.control.infrastructure.seeding.config.SeederConfigProperties;
import com.example.control.infrastructure.configfile.ConfigFileGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

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
    private final FailedHeartbeatRepositoryPort failedHeartbeatRepository;
    private final ServiceCredentialRepositoryPort serviceCredentialRepository;
    private final KVSeederService kvSeederService;
    private final SeederConfigProperties config;
    private final KeycloakUserResolver keycloakUserResolver;
    
    @Autowired(required = false)
    private ConfigFileGeneratorService configFileGeneratorService;

    /**
     * Cleans all non-IAM data from the database.
     * <p>
     * Removes all generated data while preserving IAM users and teams
     * managed by Keycloak. Deletion order respects referential integrity:
     * <ol>
     * <li>Approval Decisions</li>
     * <li>Approval Requests</li>
     * <li>Service Shares</li>
     * <li>Failed Heartbeats</li>
     * <li>Service Credentials</li>
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

            // Clean KV entries first (before deleting services)
            if (config.getKv().isEnabled() && config.getKv().isCleanBeforeSeed()) {
                // Get all service IDs from existing services before deletion
                List<String> serviceIds = applicationServiceRepository.findAll(null, Pageable.unpaged())
                        .getContent()
                        .stream()
                        .map(s -> s.getId().id())
                        .toList();
                result.kvEntriesDeleted = kvSeederService.cleanKVForServices(serviceIds);
                log.info("Deleted {} KV entries", result.kvEntriesDeleted);
            } else {
                result.kvEntriesDeleted = 0;
            }

            // Delete in reverse order of dependencies to respect referential integrity
            result.approvalDecisionsDeleted = approvalDecisionRepository.deleteAll();
            log.info("Deleted {} approval decisions", result.approvalDecisionsDeleted);

            result.approvalRequestsDeleted = approvalRequestRepository.deleteAll();
            log.info("Deleted {} approval requests", result.approvalRequestsDeleted);

            result.sharesDeleted = serviceShareRepository.deleteAll();
            log.info("Deleted {} service shares", result.sharesDeleted);

            result.failedHeartbeatsDeleted = failedHeartbeatRepository.deleteAll();
            log.info("Deleted {} failed heartbeats", result.failedHeartbeatsDeleted);

            result.serviceCredentialsDeleted = serviceCredentialRepository.deleteAll();
            log.info("Deleted {} service credentials", result.serviceCredentialsDeleted);

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
     * <li>Service Credentials (depends on ApplicationService)</li>
     * <li>Service Instances</li>
     * <li>Failed Heartbeats (depends on ServiceInstance)</li>
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
            // Load users from Keycloak and resolve admin user ID
            log.info("Loading users from Keycloak for seeding...");
            keycloakUserResolver.loadUsers();
            
            String adminUsername = config.getAdmin().getUsername();
            String adminUserId = keycloakUserResolver.resolveAdminUserId(adminUsername);
            List<String> userPool = keycloakUserResolver.getAllUserIds();
            
            log.info("Resolved admin user ID: {} (username: {}), user pool size: {}", 
                    adminUserId, adminUsername, userPool.size());
            
            // Initialize MockDataGenerator with admin user ID and user pool
            mockDataGenerator.initialize(adminUserId, userPool);
            
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

            // 2. Service Credentials (after ApplicationServices, before ServiceInstances)
            if (data.serviceCredentials != null && !data.serviceCredentials.isEmpty()) {
                log.info("Persisting {} service credentials...", data.serviceCredentials.size());
                for (ServiceCredential credential : data.serviceCredentials) {
                    serviceCredentialRepository.save(credential);
                }
                result.serviceCredentialsSeeded = data.serviceCredentials.size();
                log.info("Persisted {} service credentials", result.serviceCredentialsSeeded);
            } else {
                result.serviceCredentialsSeeded = 0;
            }

            // 3. Service Instances
            log.info("Persisting {} service instances...", data.instances.size());
            for (ServiceInstance instance : data.instances) {
                serviceInstanceRepository.save(instance);
            }
            result.instancesSeeded = data.instances.size();
            log.info("Persisted {} service instances", result.instancesSeeded);

            // 4. Failed Heartbeats (after ServiceInstances)
            if (data.failedHeartbeats != null && !data.failedHeartbeats.isEmpty()) {
                log.info("Persisting {} failed heartbeats...", data.failedHeartbeats.size());
                for (FailedHeartbeat failedHeartbeat : data.failedHeartbeats) {
                    failedHeartbeatRepository.save(failedHeartbeat);
                }
                result.failedHeartbeatsSeeded = data.failedHeartbeats.size();
                log.info("Persisted {} failed heartbeats", result.failedHeartbeatsSeeded);
            } else {
                result.failedHeartbeatsSeeded = 0;
            }

            // 5. Drift Events
            log.info("Persisting {} drift events...", data.driftEvents.size());
            for (DriftEvent driftEvent : data.driftEvents) {
                driftEventRepository.save(driftEvent);
            }
            result.driftEventsSeeded = data.driftEvents.size();
            log.info("Persisted {} drift events", result.driftEventsSeeded);

            // 6. Service Shares
            log.info("Persisting {} service shares...", data.shares.size());
            for (ServiceShare share : data.shares) {
                serviceShareRepository.save(share);
            }
            result.sharesSeeded = data.shares.size();
            log.info("Persisted {} service shares", result.sharesSeeded);

            // 7. Approval Requests
            log.info("Persisting {} approval requests...", data.approvalRequests.size());
            for (ApprovalRequest request : data.approvalRequests) {
                // Each ApprovalRequest is a fresh entity with version=0
                // Save returns a new instance with version=1, but we don't reuse it
                approvalRequestRepository.save(request);
            }
            result.approvalRequestsSeeded = data.approvalRequests.size();
            log.info("Persisted {} approval requests", result.approvalRequestsSeeded);

            // 8. Approval Decisions
            log.info("Persisting {} approval decisions...", data.approvalDecisions.size());
            for (ApprovalDecision decision : data.approvalDecisions) {
                // ApprovalDecisions are independent entities that reference ApprovalRequests
                // They don't modify the ApprovalRequest's version
                approvalDecisionRepository.save(decision);
            }
            result.approvalDecisionsSeeded = data.approvalDecisions.size();
            log.info("Persisted {} approval decisions", result.approvalDecisionsSeeded);

            // 9. KV Entries
            if (config.getKv().isEnabled()) {
                log.info("Seeding KV entries for services...");
                result.kvEntriesSeeded = kvSeederService.seedKVEntriesForServices(data.kvData);
                log.info("Seeded {} KV entries", result.kvEntriesSeeded);
            } else {
                result.kvEntriesSeeded = 0;
            }

            // 10. Config Files
            if (config.isGenerateConfigFiles() && configFileGeneratorService != null) {
                try {
                    log.info("Generating config files for {} services...", data.services.size());
                    ConfigFileGeneratorService.AggregateGenerationResult configResult =
                            configFileGeneratorService.generateForServices(data.services);
                    result.configFilesGenerated = configResult.totalFilesGenerated;
                    log.info("Generated {} config files", result.configFilesGenerated);
                } catch (Exception e) {
                    log.error("Failed to generate config files during seeding", e);
                    result.configFilesGenerated = 0;
                    // Don't fail seeding if config file generation fails
                }
            } else {
                if (config.isGenerateConfigFiles() && configFileGeneratorService == null) {
                    log.warn("Config file generation is enabled but ConfigFileGeneratorService is not available");
                }
                result.configFilesGenerated = 0;
            }

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
                .roles(List.of("SYS_ADMIN")) // Admin role for seeding operations (bare role name)
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
        public long failedHeartbeatsDeleted;
        public long serviceCredentialsDeleted;
        public long kvEntriesDeleted;

        public long getTotalDeleted() {
            return servicesDeleted + instancesDeleted + driftEventsDeleted +
                    sharesDeleted + approvalRequestsDeleted + approvalDecisionsDeleted +
                    failedHeartbeatsDeleted + serviceCredentialsDeleted + kvEntriesDeleted;
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
        public int failedHeartbeatsSeeded;
        public int serviceCredentialsSeeded;
        public int kvEntriesSeeded;
        public int configFilesGenerated;

        public int getTotalSeeded() {
            return servicesSeeded + instancesSeeded + driftEventsSeeded +
                    sharesSeeded + approvalRequestsSeeded + approvalDecisionsSeeded +
                    failedHeartbeatsSeeded + serviceCredentialsSeeded + kvEntriesSeeded;
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
