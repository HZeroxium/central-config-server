package com.example.control.seeding.service;

import com.example.control.domain.object.*;
import com.example.control.seeding.config.SeederConfigProperties;
import com.example.control.seeding.factory.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
          .computeIfAbsent(instance.getServiceName(), k -> new ArrayList<>())
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
   * Generates approval requests for orphan services.
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
      return requests;
    }

    int pendingCount = config.getData().getApprovalRequests().getPending();
    int approvedCount = config.getData().getApprovalRequests().getApproved();
    int rejectedCount = config.getData().getApprovalRequests().getRejected();

    int requestIndex = 0;

    // Generate PENDING requests
    for (int i = 0; i < pendingCount && requestIndex < orphanServices.size(); i++) {
      ApplicationService service = orphanServices.get(requestIndex++);
      String targetTeamId = config.getTeamId(i % config.getData().getTeams().getCount());

      ApprovalRequest request = approvalRequestFactory.generate(
          service, targetTeamId, ApprovalRequest.ApprovalStatus.PENDING);
      requests.add(request);
    }

    // Generate APPROVED requests
    for (int i = 0; i < approvedCount && requestIndex < orphanServices.size(); i++) {
      ApplicationService service = orphanServices.get(requestIndex++);
      String targetTeamId = config.getTeamId(i % config.getData().getTeams().getCount());

      ApprovalRequest request = approvalRequestFactory.generate(
          service, targetTeamId, ApprovalRequest.ApprovalStatus.APPROVED);
      requests.add(request);
    }

    // Generate REJECTED requests
    for (int i = 0; i < rejectedCount && requestIndex < orphanServices.size(); i++) {
      ApplicationService service = orphanServices.get(requestIndex++);
      String targetTeamId = config.getTeamId(i % config.getData().getTeams().getCount());

      ApprovalRequest request = approvalRequestFactory.generate(
          service, targetTeamId, ApprovalRequest.ApprovalStatus.REJECTED);
      requests.add(request);
    }

    return requests;
  }

  /**
   * Generates approval decisions for non-pending requests.
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
        // Generate decision for each required gate
        for (ApprovalRequest.ApprovalGate gate : request.getRequired()) {
          ApprovalDecision decision = approvalDecisionFactory.generate(
              request, adminUserId, gate.getGate());
          decisions.add(decision);
        }
      }
    }

    return decisions;
  }
}
