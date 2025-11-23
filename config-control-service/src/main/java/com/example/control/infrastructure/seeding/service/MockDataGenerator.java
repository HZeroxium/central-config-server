package com.example.control.infrastructure.seeding.service;

import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.*;
import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVListStructure;
import com.example.control.infrastructure.seeding.config.SeederConfigProperties;
import com.example.control.infrastructure.seeding.factory.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service responsible for generating mock data using factories.
 * <p>
 * Orchestrates data generation across multiple factories to create a
 * complete dataset with proper relationships and referential integrity.
 * </p>
 *
 * <p>
 * <strong>Generation Flow:</strong>
 * </p>
 * <ol>
 * <li>Generate ApplicationServices (owned and orphan)</li>
 * <li>Generate ServiceInstances for each service</li>
 * <li>Generate DriftEvents for instances with drift</li>
 * <li>Generate ServiceShares between teams</li>
 * <li>Generate ApprovalRequests for orphan services</li>
 * <li>Generate ApprovalDecisions for non-pending requests</li>
 * </ol>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataGenerator {

    private final SeederConfigProperties config;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final ServiceInstanceFactory serviceInstanceFactory;
    private final DriftEventFactory driftEventFactory;
    private final ServiceShareFactory serviceShareFactory;
    private final ApprovalRequestFactory approvalRequestFactory;
    private final ApprovalDecisionFactory approvalDecisionFactory;
    private final FailedHeartbeatFactory failedHeartbeatFactory;
    private final ServiceCredentialFactory serviceCredentialFactory;
    private final KVEntryFactory kvEntryFactory;
    private final KVListFactory kvListFactory;
    private final KeycloakUserResolver keycloakUserResolver;
    private final TestKVServiceGenerator testKVServiceGenerator;

    /**
     * Admin user ID resolved from Keycloak.
     */
    private String adminUserId;

    /**
     * Pool of user IDs for random assignment.
     */
    private List<String> userPool = List.of();

    /**
     * Initializes the generator with admin user ID and user pool.
     * <p>
     * Must be called before generateAll() to ensure proper user ID resolution.
     * </p>
     *
     * @param adminUserId admin user ID from Keycloak
     * @param userPool    pool of user IDs for random assignment
     */
    public void initialize(String adminUserId, List<String> userPool) {
        this.adminUserId = adminUserId;
        this.userPool = userPool != null ? new ArrayList<>(userPool) : List.of();
        
        // Set user pools in factories
        approvalRequestFactory.setUserPool(this.userPool);
        serviceShareFactory.setUserPool(this.userPool);
        
        log.info("MockDataGenerator initialized with adminUserId: {}, userPool size: {}", 
                adminUserId, this.userPool.size());
    }

    /**
     * Generates complete mock dataset according to configuration.
     * <p>
     * Requires initialize() to be called first with admin user ID and user pool.
     * </p>
     *
     * @return generated data container
     * @throws IllegalStateException if not initialized
     */
    public GeneratedData generateAll() {
        if (adminUserId == null || adminUserId.isBlank()) {
            throw new IllegalStateException(
                    "MockDataGenerator not initialized. Call initialize() before generateAll().");
        }
        log.info("Starting mock data generation with configuration: teams={}, services={}/{}/{}, instances={}-{}",
                config.getData().getTeams().getCount(),
                config.getData().getServices().getTeam1Count(),
                config.getData().getServices().getTeam2Count(),
                config.getData().getServices().getOrphanCount(),
                config.getData().getInstancesPerService().getMin(),
                config.getData().getInstancesPerService().getMax());

        GeneratedData data = new GeneratedData();

        // Phase 1: Generate Application Services (including test-kv-service)
        data.services = generateApplicationServices();
        log.info("Generated {} application services", data.services.size());

        // Phase 2: Generate Service Instances
        data.instances = generateServiceInstances(data.services);
        log.info("Generated {} service instances", data.instances.size());

        // Phase 3: Generate Drift Events
        data.driftEvents = generateDriftEvents(data.services, data.instances);
        log.info("Generated {} drift events", data.driftEvents.size());

        // Phase 4: Generate Service Shares
        data.shares = generateServiceShares(data.services);
        log.info("Generated {} service shares", data.shares.size());

        // Phase 5: Generate Approval Requests
        data.approvalRequests = generateApprovalRequests(data.services);
        log.info("Generated {} approval requests", data.approvalRequests.size());

        // Phase 6: Generate Approval Decisions
        data.approvalDecisions = generateApprovalDecisions(data.approvalRequests);
        log.info("Generated {} approval decisions", data.approvalDecisions.size());

        // Phase 7: Generate Service Credentials (for services with owners)
        data.serviceCredentials = generateServiceCredentials(data.services);
        log.info("Generated {} service credentials", data.serviceCredentials.size());

        // Phase 8: Generate Failed Heartbeats (for some instances)
        data.failedHeartbeats = generateFailedHeartbeats(data.services, data.instances);
        log.info("Generated {} failed heartbeats", data.failedHeartbeats.size());

        // Phase 9: Generate KV Entries (including test-kv-service primitives)
        data.kvData = generateKVEntries(data.services);
        int totalKVEntries = data.kvData.getTotalEntryCount();
        log.info("Generated {} KV entries across {} services", totalKVEntries, data.services.size());

        log.info("Mock data generation complete. Total entities: {}", data.getTotalCount());

        return data;
    }

    /**
     * Generates application services according to configuration.
     * <p>
     * Includes test-kv-service as an orphan service for KVApi testing.
     * </p>
     *
     * @return list of generated services
     */
    private List<ApplicationService> generateApplicationServices() {
        List<ApplicationService> services = new ArrayList<>();
        int serviceIndex = 0;

        // Generate team1 services
        String team1Id = config.getTeamId(0);
        for (int i = 0; i < config.getData().getServices().getTeam1Count(); i++) {
            ApplicationService service = applicationServiceFactory.generate(
                    serviceIndex++, team1Id, adminUserId);
            services.add(service);
        }

        // Generate team2 services
        String team2Id = config.getTeamId(1);
        for (int i = 0; i < config.getData().getServices().getTeam2Count(); i++) {
            ApplicationService service = applicationServiceFactory.generate(
                    serviceIndex++, team2Id, adminUserId);
            services.add(service);
        }

        // Generate orphan services (no owner)
        for (int i = 0; i < config.getData().getServices().getOrphanCount(); i++) {
            ApplicationService service = applicationServiceFactory.generate(
                    serviceIndex++, null, adminUserId);
            services.add(service);
        }

        // Generate test-kv-service (orphan service for KVApi testing)
        ApplicationService testService = testKVServiceGenerator.generateTestService(adminUserId);
        services.add(testService);
        log.info("Generated test-kv-service for KVApi testing");

        return services;
    }

    /**
     * Generates service instances for all services.
     *
     * @param services list of services
     * @return list of generated instances
     */
    private List<ServiceInstance> generateServiceInstances(List<ApplicationService> services) {
        List<ServiceInstance> instances = new ArrayList<>();

        for (ApplicationService service : services) {
            int instanceCount = config.getData().getInstancesPerService().getMin() +
                    (int) (Math.random() *
                            (config.getData().getInstancesPerService().getMax() -
                                    config.getData().getInstancesPerService().getMin() + 1));

            for (int i = 0; i < instanceCount; i++) {
                ServiceInstance instance = serviceInstanceFactory.generate(
                        service.getId().id(),
                        service.getId().id(),
                        service.getOwnerTeamId(),
                        i);
                instances.add(instance);
            }
        }

        return instances;
    }

    /**
     * Generates drift events for instances with drift.
     *
     * @param services  list of services
     * @param instances list of instances
     * @return list of generated drift events
     */
    private List<DriftEvent> generateDriftEvents(List<ApplicationService> services,
            List<ServiceInstance> instances) {
        List<DriftEvent> driftEvents = new ArrayList<>();

        // Group instances by service
        Map<String, List<ServiceInstance>> instancesByService = new HashMap<>();
        for (ServiceInstance instance : instances) {
            instancesByService
                    .computeIfAbsent(instance.getServiceId(), k -> new ArrayList<>())
                    .add(instance);
        }

        // Generate drift events for each service
        for (ApplicationService service : services) {
            List<ServiceInstance> serviceInstances = instancesByService.getOrDefault(service.getId().id(), List.of());

            if (serviceInstances.isEmpty()) {
                continue;
            }

            // Determine number of drift events for this service
            int minDrifts = config.getData().getDriftEvents().getMinPerService();
            int maxDrifts = config.getData().getDriftEvents().getMaxPerService();
            int driftCount = minDrifts + (int) (Math.random() * (maxDrifts - minDrifts + 1));
            driftCount = Math.min(driftCount, serviceInstances.size());

            // Generate drift events for random instances
            Collections.shuffle(serviceInstances);
            for (int i = 0; i < driftCount; i++) {
                ServiceInstance instance = serviceInstances.get(i);
                DriftEvent driftEvent = driftEventFactory.generate(
                        service.getId().id(),
                        service.getId().id(),
                        service.getOwnerTeamId(),
                        instance);
                driftEvents.add(driftEvent);
            }
        }

        return driftEvents;
    }

    /**
     * Generates service shares between teams.
     *
     * @param services list of services
     * @return list of generated shares
     */
    private List<ServiceShare> generateServiceShares(List<ApplicationService> services) {
        List<ServiceShare> shares = new ArrayList<>();

        // Filter services with owners (exclude orphans)
        List<ApplicationService> ownedServices = services.stream()
                .filter(s -> s.getOwnerTeamId() != null)
                .toList();

        if (ownedServices.isEmpty()) {
            return shares;
        }

        int shareCount = config.getData().getShares().getCount();

        // Generate shares
        for (int i = 0; i < shareCount && i < ownedServices.size(); i++) {
            ApplicationService service = ownedServices.get(i % ownedServices.size());

            // Determine target team (opposite of owner)
            String ownerTeamId = service.getOwnerTeamId();
            String targetTeamId = ownerTeamId.equals(config.getTeamId(0))
                    ? config.getTeamId(1)
                    : config.getTeamId(0);

            // Alternate between VIEW and EDIT shares
            ServiceShareFactory.ShareType shareType = (i % 2 == 0)
                    ? ServiceShareFactory.ShareType.VIEW
                    : ServiceShareFactory.ShareType.EDIT;

            // Use random user ID from pool for grantedBy
            String grantedBy = userPool.isEmpty() ? adminUserId : 
                    userPool.get((int) (Math.random() * userPool.size()));
            
            ServiceShare share = serviceShareFactory.generate(
                    service, targetTeamId, grantedBy, shareType);
            shares.add(share);
        }

        return shares;
    }

    /**
     * Generates approval requests for orphan services with multi-user scenarios.
     * <p>
     * Creates realistic scenarios including:
     * - Multiple users requesting the same service (competition)
     * - User retry patterns (rejected then approved)
     * - Multi-gate approvals (LINE_MANAGER + SYS_ADMIN)
     * </p>
     *
     * @param services list of services
     * @return list of generated approval requests
     */
    private List<ApprovalRequest> generateApprovalRequests(List<ApplicationService> services) {
        List<ApprovalRequest> requests = new ArrayList<>();

        // Filter orphan services
        List<ApplicationService> orphanServices = services.stream()
                .filter(s -> s.getOwnerTeamId() == null)
                .toList();

        if (orphanServices.isEmpty()) {
            log.warn("No orphan services found for approval request generation");
            return requests;
        }

        int pendingCount = config.getData().getApprovalRequests().getPending();
        int approvedCount = config.getData().getApprovalRequests().getApproved();
        int rejectedCount = config.getData().getApprovalRequests().getRejected();

        log.debug("Generating approval requests: pending={}, approved={}, rejected={}",
                pendingCount, approvedCount, rejectedCount);

        // Use real user IDs from pool
        if (userPool.isEmpty()) {
            log.warn("User pool is empty, cannot generate approval requests");
            return requests;
        }

        String team1Id = config.getTeamId(0);
        String team2Id = config.getTeamId(1);

        // Scenario 1: PENDING requests - multiple users competing for same services
        // Service 0: two different users (team1 and team2) both pending
        if (orphanServices.size() > 0 && pendingCount >= 2 && userPool.size() >= 2) {
            ApplicationService service0 = orphanServices.get(0);

            // First user requests for team1 (PENDING)
            String userId1 = userPool.get(0);
            requests.add(approvalRequestFactory.generateForUser(
                    service0, team1Id, userId1, ApprovalRequest.ApprovalStatus.PENDING));

            // Second user requests for team2 (PENDING) - competition
            String userId2 = userPool.size() > 1 ? userPool.get(1) : userPool.get(0);
            requests.add(approvalRequestFactory.generateForUser(
                    service0, team2Id, userId2, ApprovalRequest.ApprovalStatus.PENDING));

            pendingCount -= 2;
        }

        // Add remaining PENDING requests from various users
        int serviceIdx = 1;
        for (int i = 0; i < pendingCount && serviceIdx < orphanServices.size(); i++) {
            ApplicationService service = orphanServices.get(serviceIdx);
            String userId = userPool.get(i % userPool.size());
            String targetTeamId = (i % 2 == 0) ? team1Id : team2Id;

            requests.add(approvalRequestFactory.generateForUser(
                    service, targetTeamId, userId, ApprovalRequest.ApprovalStatus.PENDING));
            serviceIdx++;
        }

        // Scenario 2: APPROVED requests - simulate successful approvals
        // Include multi-gate approvals (with LINE_MANAGER)
        for (int i = 0; i < approvedCount && serviceIdx < orphanServices.size(); i++) {
            ApplicationService service = orphanServices.get(serviceIdx);
            String userId = userPool.get(i % userPool.size());
            String targetTeamId = (i % 2 == 0) ? team1Id : team2Id;

            // Every other approved request has LINE_MANAGER gate
            if (i % 2 == 0 && userPool.size() > 1) {
                // With LINE_MANAGER gate (use second user as manager)
                String managerId = userPool.get(1);
                String requesterId = userPool.get(0);
                requests.add(approvalRequestFactory.generateWithManager(
                        service, targetTeamId, requesterId, managerId, ApprovalRequest.ApprovalStatus.APPROVED));
            } else {
                // SYS_ADMIN only
                requests.add(approvalRequestFactory.generateForUser(
                        service, targetTeamId, userId, ApprovalRequest.ApprovalStatus.APPROVED));
            }
            serviceIdx++;
        }

        // Scenario 3: REJECTED requests - simulate rejections
        for (int i = 0; i < rejectedCount && serviceIdx < orphanServices.size(); i++) {
            ApplicationService service = orphanServices.get(serviceIdx);
            String userId = userPool.get(i % userPool.size());
            String targetTeamId = (i % 2 == 0) ? team1Id : team2Id;

            requests.add(approvalRequestFactory.generateForUser(
                    service, targetTeamId, userId, ApprovalRequest.ApprovalStatus.REJECTED));
            serviceIdx++;
        }

        log.info("Generated {} approval requests with multi-user scenarios", requests.size());
        return requests;
    }

    /**
     * Generates approval decisions for non-pending requests.
     *
     * <p>
     * Extracts primitive values from ApprovalRequest to avoid passing entity
     * references,
     * which prevents OptimisticLockingFailureException when working with versioned
     * entities.
     *
     * @param approvalRequests list of approval requests
     * @return list of generated decisions
     */
    private List<ApprovalDecision> generateApprovalDecisions(List<ApprovalRequest> approvalRequests) {
        List<ApprovalDecision> decisions = new ArrayList<>();

        for (ApprovalRequest request : approvalRequests) {
            // Only generate decisions for approved/rejected requests
            if (request.getStatus() != ApprovalRequest.ApprovalStatus.PENDING) {
                // Extract primitive values to avoid passing versioned entity reference
                String requestId = request.getId().id();
                ApprovalRequest.ApprovalStatus requestStatus = request.getStatus();
                Instant requestCreatedAt = request.getCreatedAt();
                Instant requestUpdatedAt = request.getUpdatedAt();

                // Generate decision for each required gate
                for (ApprovalRequest.ApprovalGate gate : request.getRequired()) {
                    ApprovalDecision decision = approvalDecisionFactory.generate(
                            requestId, requestStatus, requestCreatedAt, requestUpdatedAt,
                            adminUserId, gate.getGate());
                    decisions.add(decision);
                }
            }
        }

        return decisions;
    }

    /**
     * Generates service credentials for services with owners.
     * <p>
     * Only generates credentials for services that have an owner team
     * (non-orphan services). Approximately 80% of owned services get credentials.
     * </p>
     *
     * @param services list of services
     * @return list of generated service credentials
     */
    private List<ServiceCredential> generateServiceCredentials(List<ApplicationService> services) {
        List<ServiceCredential> credentials = new ArrayList<>();

        // Filter services with owners
        List<ApplicationService> ownedServices = services.stream()
                .filter(s -> s.getOwnerTeamId() != null)
                .toList();

        // Generate credentials for ~80% of owned services
        int targetCount = (int) Math.ceil(ownedServices.size() * 0.8);
        List<ApplicationService> selectedServices = new ArrayList<>(ownedServices);
        Collections.shuffle(selectedServices);
        selectedServices = selectedServices.subList(0, Math.min(targetCount, selectedServices.size()));

        for (ApplicationService service : selectedServices) {
            ServiceCredential credential = serviceCredentialFactory.generate(service, adminUserId);
            if (credential != null) {
                credentials.add(credential);
            }
        }

        return credentials;
    }

    /**
     * Generates failed heartbeats for some service instances.
     * <p>
     * Generates failed heartbeats for approximately 5% of instances
     * to simulate real-world DLQ scenarios.
     * </p>
     *
     * @param services  list of services (for serviceId and teamId lookup)
     * @param instances list of instances
     * @return list of generated failed heartbeats
     */
    private List<FailedHeartbeat> generateFailedHeartbeats(List<ApplicationService> services,
                                                          List<ServiceInstance> instances) {
        List<FailedHeartbeat> failedHeartbeats = new ArrayList<>();

        // Create service lookup map
        Map<String, ApplicationService> serviceMap = new HashMap<>();
        for (ApplicationService service : services) {
            serviceMap.put(service.getId().id(), service);
        }

        // Generate failed heartbeats for ~5% of instances
        int targetCount = (int) Math.ceil(instances.size() * 0.05);
        List<ServiceInstance> selectedInstances = new ArrayList<>(instances);
        Collections.shuffle(selectedInstances);
        selectedInstances = selectedInstances.subList(0, Math.min(targetCount, selectedInstances.size()));

        for (ServiceInstance instance : selectedInstances) {
            ApplicationService service = serviceMap.get(instance.getServiceId());
            if (service == null) {
                continue;
            }

            String environment = instance.getEnvironment() != null 
                    ? instance.getEnvironment() 
                    : "dev";
            
            FailedHeartbeat failedHeartbeat = failedHeartbeatFactory.generate(
                    service.getDisplayName(),
                    instance.getInstanceId(),
                    service.getId().id(),
                    service.getOwnerTeamId(),
                    environment);
            failedHeartbeats.add(failedHeartbeat);
        }

        return failedHeartbeats;
    }

    /**
     * Generates KV entries for approximately 10% of services (random selection).
     * <p>
     * Always includes test-kv-service if it exists in the services list.
     * </p>
     *
     * @param services list of services
     * @return KVData container with structured entries
     */
    private KVData generateKVEntries(List<ApplicationService> services) {
        KVData kvData = new KVData();

        if (!config.getKv().isEnabled()) {
            log.debug("KV seeding is disabled, skipping KV entry generation");
            return kvData;
        }

        // Separate test-kv-service from other services
        List<ApplicationService> regularServices = new ArrayList<>();
        ApplicationService testKvService = null;
        String testServiceId = "test-kv-service";

        for (ApplicationService service : services) {
            if (testServiceId.equals(service.getId().id())) {
                testKvService = service;
            } else {
                regularServices.add(service);
            }
        }

        // Select ~10% of regular services (random selection)
        int targetCount = regularServices.isEmpty() ? 0 : (int) Math.ceil(regularServices.size() * 0.1);
        List<ApplicationService> selectedServices = new ArrayList<>(regularServices);
        Collections.shuffle(selectedServices);
        selectedServices = new ArrayList<>(selectedServices.subList(0, Math.min(targetCount, selectedServices.size())));

        // Always include test-kv-service if it exists
        if (testKvService != null) {
            selectedServices.add(testKvService);
            log.info("Selected {} regular services (10%) plus test-kv-service for KV seeding (total: {})", 
                    targetCount, selectedServices.size());
        } else {
            log.info("Selected {} services (10%) for KV seeding", selectedServices.size());
        }

        // Generate KV entries for selected services
        for (ApplicationService service : selectedServices) {
            String serviceId = service.getId().id();
            generateKVEntriesForService(serviceId, kvData);
        }

        // Generate test-kv-service primitive test entries (always)
        List<KVEntry> testEntries = testKVServiceGenerator.generatePrimitiveTestEntries();
        for (KVEntry entry : testEntries) {
            // Check if entry is LEAF_LIST (flags=3) and add to appropriate container
            if (entry.flags() == 3L) {
                kvData.leafListEntries.computeIfAbsent(testServiceId, k -> new ArrayList<>()).add(entry);
            } else {
                kvData.leafEntries.computeIfAbsent(testServiceId, k -> new ArrayList<>()).add(entry);
            }
        }
        log.info("Generated {} primitive test KV entries for test-kv-service", testEntries.size());

        return kvData;
    }

    /**
     * Generates KV entries for a single service.
     *
     * @param serviceId service ID
     * @param kvData    KVData container to populate
     */
    private void generateKVEntriesForService(String serviceId, KVData kvData) {
        // Determine total entry count
        int minEntries = config.getKv().getEntriesPerService().getMin();
        int maxEntries = config.getKv().getEntriesPerService().getMax();
        int totalEntries = minEntries + (int) (Math.random() * (maxEntries - minEntries + 1));

        // Distribute across categories
        SeederConfigProperties.CategoriesConfig categories = config.getKv().getCategories();

        // Config category
        if (categories.getConfig().isEnabled()) {
            int configEntries = generateCategoryEntryCount(
                    categories.getConfig().getMinEntries(),
                    categories.getConfig().getMaxEntries(),
                    totalEntries);
            generateCategoryEntries(serviceId, "config", configEntries, categories.getConfig(), kvData);
        }

        // Secrets category
        if (categories.getSecrets().isEnabled()) {
            int secretEntries = generateCategoryEntryCount(
                    categories.getSecrets().getMinEntries(),
                    categories.getSecrets().getMaxEntries(),
                    totalEntries);
            generateCategoryEntries(serviceId, "secrets", secretEntries, categories.getSecrets(), kvData);
        }

        // Feature flags category
        if (categories.getFeatureFlags().isEnabled()) {
            int featureFlagEntries = generateCategoryEntryCount(
                    categories.getFeatureFlags().getMinEntries(),
                    categories.getFeatureFlags().getMaxEntries(),
                    totalEntries);
            generateCategoryEntries(serviceId, "feature-flags", featureFlagEntries,
                    categories.getFeatureFlags(), kvData);
        }
    }

    /**
     * Generates entry count for a category.
     *
     * @param minEntries minimum entries
     * @param maxEntries maximum entries
     * @param totalEntries total entries available
     * @return entry count for category
     */
    private int generateCategoryEntryCount(int minEntries, int maxEntries, int totalEntries) {
        int count = minEntries + (int) (Math.random() * (maxEntries - minEntries + 1));
        return Math.min(count, totalEntries);
    }

    /**
     * Generates entries for a category with proper distribution (LEAF/LIST/LEAF_LIST).
     *
     * @param serviceId service ID
     * @param category  category name (config, secrets, feature-flags)
     * @param count     number of entries to generate
     * @param categoryConfig category configuration
     * @param kvData    KVData container to populate
     */
    private void generateCategoryEntries(String serviceId, String category, int count,
                                        SeederConfigProperties.CategoryConfig categoryConfig,
                                        KVData kvData) {
        if (count == 0) {
            return;
        }

        // Calculate three-way distribution
        int leafCount = (int) Math.round(count * categoryConfig.getLeafPercentage() / 100.0);
        int listCount = (int) Math.round(count * categoryConfig.getListPercentage() / 100.0);
        int leafListCount = count - leafCount - listCount; // Remaining for LEAF_LIST

        // Generate LEAF entries
        for (int i = 0; i < leafCount; i++) {
            KVEntry entry;
            if ("config".equals(category)) {
                String key = kvEntryFactory.generateConfigKey();
                entry = kvEntryFactory.generateConfigLeaf(serviceId, key);
            } else if ("secrets".equals(category)) {
                String key = kvEntryFactory.generateSecretKey();
                entry = kvEntryFactory.generateSecretLeaf(serviceId, key);
            } else {
                String key = kvEntryFactory.generateFeatureFlagKey();
                entry = kvEntryFactory.generateFeatureFlagLeaf(serviceId, key);
            }
            kvData.leafEntries.computeIfAbsent(serviceId, k -> new ArrayList<>()).add(entry);
        }

        // Generate LIST entries
        for (int i = 0; i < listCount; i++) {
            int itemCount = 3 + (int) (Math.random() * 8); // 3-10 items
            String prefix;
            KVListStructure listStructure;
            if ("config".equals(category)) {
                prefix = kvListFactory.generateConfigListPrefix();
                listStructure = kvListFactory.generateConfigList(prefix, itemCount);
            } else if ("secrets".equals(category)) {
                prefix = kvListFactory.generateSecretListPrefix();
                listStructure = kvListFactory.generateSecretList(prefix, itemCount);
            } else {
                prefix = kvListFactory.generateFeatureFlagListPrefix();
                listStructure = kvListFactory.generateFeatureFlagList(prefix, itemCount);
            }
            String relativePrefix = category + "/" + prefix;
            kvData.listEntries.computeIfAbsent(serviceId, k -> new ArrayList<>())
                    .add(new KVListData(relativePrefix, listStructure));
        }

        // Generate LEAF_LIST entries
        for (int i = 0; i < leafListCount; i++) {
            KVEntry entry;
            if ("config".equals(category)) {
                String key = kvEntryFactory.generateConfigKey() + "-list";
                entry = kvEntryFactory.generateConfigLeafList(serviceId, key);
            } else if ("secrets".equals(category)) {
                String key = kvEntryFactory.generateSecretKey() + "-list";
                entry = kvEntryFactory.generateSecretLeafList(serviceId, key);
            } else {
                String key = kvEntryFactory.generateFeatureFlagKey() + "-list";
                entry = kvEntryFactory.generateFeatureFlagLeafList(serviceId, key);
            }
            kvData.leafListEntries.computeIfAbsent(serviceId, k -> new ArrayList<>()).add(entry);
        }
    }

    /**
     * Generated data container.
     */
    public static class GeneratedData {
        public List<ApplicationService> services = new ArrayList<>();
        public List<ServiceInstance> instances = new ArrayList<>();
        public List<DriftEvent> driftEvents = new ArrayList<>();
        public List<ServiceShare> shares = new ArrayList<>();
        public List<ApprovalRequest> approvalRequests = new ArrayList<>();
        public List<ApprovalDecision> approvalDecisions = new ArrayList<>();
        public List<ServiceCredential> serviceCredentials = new ArrayList<>();
        public List<FailedHeartbeat> failedHeartbeats = new ArrayList<>();
        public KVData kvData = new KVData();

        public int getTotalCount() {
            return services.size() + instances.size() + driftEvents.size() +
                    shares.size() + approvalRequests.size() + approvalDecisions.size() +
                    serviceCredentials.size() + failedHeartbeats.size() +
                    kvData.getTotalEntryCount();
        }
    }

    /**
     * Container for structured KV data.
     */
    public static class KVData {
        /**
         * Leaf entries by service ID.
         */
        public Map<String, List<KVEntry>> leafEntries = new HashMap<>();

        /**
         * List entries by service ID (with relative prefix).
         */
        public Map<String, List<KVListData>> listEntries = new HashMap<>();

        /**
         * LEAF_LIST entries by service ID.
         */
        public Map<String, List<KVEntry>> leafListEntries = new HashMap<>();

        /**
         * Calculate total entry count across all types.
         * <p>
         * For lists, counts items + manifest.
         * For LEAF_LIST, counts as single entries.
         * </p>
         *
         * @return total entry count
         */
        public int getTotalEntryCount() {
            int leafCount = leafEntries.values().stream().mapToInt(List::size).sum();
            
            // Count list items + manifest
            int listCount = listEntries.values().stream()
                    .flatMap(List::stream)
                    .mapToInt(kvListData -> kvListData.listStructure().items().size() + 1) // items + manifest
                    .sum();
            
            // Count LEAF_LIST entries
            int leafListCount = leafListEntries.values().stream().mapToInt(List::size).sum();
            
            return leafCount + listCount + leafListCount;
        }
    }

    /**
     * Wrapper for list data with relative prefix.
     *
     * @param relativePrefix relative prefix (e.g., "config/allowed-ips")
     * @param listStructure  KVListStructure with items and manifest
     */
    public record KVListData(String relativePrefix, KVListStructure listStructure) {
    }
}
