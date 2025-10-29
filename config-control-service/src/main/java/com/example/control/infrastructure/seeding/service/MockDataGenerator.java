package com.example.control.infrastructure.seeding.service;

import com.example.control.domain.object.*;
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

    /**
     * Generates complete mock dataset according to configuration.
     *
     * @return generated data container
     */
    public GeneratedData generateAll() {
        log.info("Starting mock data generation with configuration: teams={}, services={}/{}/{}, instances={}-{}",
                config.getData().getTeams().getCount(),
                config.getData().getServices().getTeam1Count(),
                config.getData().getServices().getTeam2Count(),
                config.getData().getServices().getOrphanCount(),
                config.getData().getInstancesPerService().getMin(),
                config.getData().getInstancesPerService().getMax());

        GeneratedData data = new GeneratedData();

        // Phase 1: Generate Application Services
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

        log.info("Mock data generation complete. Total entities: {}", data.getTotalCount());

        return data;
    }

    /**
     * Generates application services according to configuration.
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
                    serviceIndex++, team1Id, "admin");
            services.add(service);
        }

        // Generate team2 services
        String team2Id = config.getTeamId(1);
        for (int i = 0; i < config.getData().getServices().getTeam2Count(); i++) {
            ApplicationService service = applicationServiceFactory.generate(
                    serviceIndex++, team2Id, "admin");
            services.add(service);
        }

        // Generate orphan services (no owner)
        for (int i = 0; i < config.getData().getServices().getOrphanCount(); i++) {
            ApplicationService service = applicationServiceFactory.generate(
                    serviceIndex++, null, "admin");
            services.add(service);
        }

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

            ServiceShare share = serviceShareFactory.generate(
                    service, targetTeamId, ownerTeamId, shareType);
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

        // Define test users
        String[] users = { "user1", "user2", "user3", "user4", "user5" };
        String team1Id = config.getTeamId(0);
        String team2Id = config.getTeamId(1);

        // Scenario 1: PENDING requests - multiple users competing for same services
        // Service 0: user1 (team1) and user3 (team2) both pending
        if (orphanServices.size() > 0 && pendingCount >= 2) {
            ApplicationService service0 = orphanServices.get(0);

            // User1 requests for team1 (PENDING)
            requests.add(approvalRequestFactory.generateForUser(
                    service0, team1Id, users[0], ApprovalRequest.ApprovalStatus.PENDING));

            // User3 requests for team2 (PENDING) - competition
            requests.add(approvalRequestFactory.generateForUser(
                    service0, team2Id, users[2], ApprovalRequest.ApprovalStatus.PENDING));

            pendingCount -= 2;
        }

        // Add remaining PENDING requests from various users
        int serviceIdx = 1;
        for (int i = 0; i < pendingCount && serviceIdx < orphanServices.size(); i++) {
            ApplicationService service = orphanServices.get(serviceIdx);
            String userId = users[i % users.length];
            String targetTeamId = (i % 2 == 0) ? team1Id : team2Id;

            requests.add(approvalRequestFactory.generateForUser(
                    service, targetTeamId, userId, ApprovalRequest.ApprovalStatus.PENDING));
            serviceIdx++;
        }

        // Scenario 2: APPROVED requests - simulate successful approvals
        // Include multi-gate approvals (with LINE_MANAGER)
        for (int i = 0; i < approvedCount && serviceIdx < orphanServices.size(); i++) {
            ApplicationService service = orphanServices.get(serviceIdx);
            String userId = users[i % users.length];
            String targetTeamId = (i % 2 == 0) ? team1Id : team2Id;

            // Every other approved request has LINE_MANAGER gate
            if (i % 2 == 0) {
                // With LINE_MANAGER gate (user2 reports to user1)
                String managerId = "user1";
                requests.add(approvalRequestFactory.generateWithManager(
                        service, targetTeamId, users[1], managerId, ApprovalRequest.ApprovalStatus.APPROVED));
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
            String userId = users[i % users.length];
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

        String adminUserId = config.getAdmin().getUserId();

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
     * Generated data container.
     */
    public static class GeneratedData {
        public List<ApplicationService> services = new ArrayList<>();
        public List<ServiceInstance> instances = new ArrayList<>();
        public List<DriftEvent> driftEvents = new ArrayList<>();
        public List<ServiceShare> shares = new ArrayList<>();
        public List<ApprovalRequest> approvalRequests = new ArrayList<>();
        public List<ApprovalDecision> approvalDecisions = new ArrayList<>();

        public int getTotalCount() {
            return services.size() + instances.size() + driftEvents.size() +
                    shares.size() + approvalRequests.size() + approvalDecisions.size();
        }
    }
}
