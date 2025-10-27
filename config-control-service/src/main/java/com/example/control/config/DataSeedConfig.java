package com.example.control.config;

import com.example.control.application.service.ApplicationServiceService;
import com.example.control.application.service.DriftEventService;
import com.example.control.application.service.ServiceInstanceService;
import com.example.control.application.service.ServiceShareService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.*;
import com.example.control.domain.id.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Data seeding configuration for development environment.
 * <p>
 * Automatically seeds the database with sample data when the application starts
 * in the 'dev' profile. This provides a consistent test environment with
 * realistic data for development and testing.
 * </p>
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DataSeedConfig {

    private final ApplicationServiceService applicationServiceService;
    private final ServiceInstanceService serviceInstanceService;
    private final DriftEventService driftEventService;
    private final ServiceShareService serviceShareService;
    private final ObjectMapper objectMapper;

    /**
     * Seed the database with sample data on application startup.
     * <p>
     * Only runs in 'dev' profile to avoid polluting production data.
     * Checks if data already exists before seeding to support multiple restarts.
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        log.info("Starting data seeding for dev profile...");
        
        try {
            // Check if data already exists
            if (!applicationServiceService.findAll().isEmpty()) {
                log.info("Data already exists, skipping seed");
                return;
            }
            
            // Load seed data from JSON
            ClassPathResource resource = new ClassPathResource("data-seed.json");
            JsonNode seedData = objectMapper.readTree(resource.getInputStream());
            
            // Create admin user context for seeding
            UserContext adminContext = UserContext.builder()
                .userId("admin")
                .username("admin")
                .email("admin@example.com")
                .roles(List.of("SYS_ADMIN"))
                .teamIds(List.of())
                .build();
            
            // Seed ApplicationServices
            seedApplicationServices(seedData.get("applicationServices"), adminContext);
            
            // Seed ServiceShares
            seedServiceShares(seedData.get("serviceShares"), adminContext);
            
            // Seed ServiceInstances
            seedServiceInstances(seedData.get("serviceInstances"));
            
            // Seed DriftEvents
            seedDriftEvents(seedData.get("driftEvents"));
            
            log.info("Data seeding completed successfully");
            
        } catch (IOException e) {
            log.error("Failed to load seed data", e);
        } catch (Exception e) {
            log.error("Failed to seed data", e);
        }
    }

    private void seedApplicationServices(JsonNode services, UserContext adminContext) {
        if (services == null || !services.isArray()) {
            log.warn("No application services found in seed data");
            return;
        }
        
        log.info("Seeding {} application services", services.size());
        
        StreamSupport.stream(services.spliterator(), false)
            .forEach(serviceNode -> {
                try {
                    ApplicationService service = ApplicationService.builder()
                        .id(ApplicationServiceId.of(serviceNode.get("id").asText()))
                        .displayName(serviceNode.get("displayName").asText())
                        .ownerTeamId(serviceNode.has("ownerTeamId") && !serviceNode.get("ownerTeamId").isNull() 
                            ? serviceNode.get("ownerTeamId").asText() : null)
                        .lifecycle(ApplicationService.ServiceLifecycle.valueOf(serviceNode.get("lifecycle").asText()))
                        .createdBy(adminContext.getUserId())
                        .createdAt(Instant.now())
                        .build();
                    
                    applicationServiceService.save(service, adminContext);
                    log.debug("Created application service: {}", service.getId());
                    
                } catch (Exception e) {
                    log.error("Failed to create application service: {}", serviceNode, e);
                }
            });
    }

    private void seedServiceShares(JsonNode shares, UserContext adminContext) {
        if (shares == null || !shares.isArray()) {
            log.warn("No service shares found in seed data");
            return;
        }
        
        log.info("Seeding {} service shares", shares.size());
        
        StreamSupport.stream(shares.spliterator(), false)
            .forEach(shareNode -> {
                try {
                    ServiceShare share = ServiceShare.builder()
                        .id(ServiceShareId.of(java.util.UUID.randomUUID().toString()))
                        .resourceLevel(ServiceShare.ResourceLevel.SERVICE)
                        .serviceId(shareNode.get("serviceId").asText())
                        .grantToType(ServiceShare.GranteeType.valueOf(shareNode.get("grantToType").asText()))
                        .grantToId(shareNode.get("grantToId").asText())
                        .permissions(StreamSupport.stream(shareNode.get("permissions").spliterator(), false)
                            .map(node -> ServiceShare.SharePermission.valueOf(node.asText()))
                            .toList())
                        .environments(shareNode.has("environments") 
                            ? StreamSupport.stream(shareNode.get("environments").spliterator(), false)
                                .map(JsonNode::asText)
                                .toList() : null)
                        .grantedBy(shareNode.get("grantedBy").asText())
                        .createdAt(Instant.now())
                        .build();
                    
                    serviceShareService.grantShare(
                        share.getServiceId(),
                        share.getGrantToType(),
                        share.getGrantToId(),
                        share.getPermissions(),
                        share.getEnvironments(),
                        share.getExpiresAt(),
                        adminContext
                    );
                    log.debug("Created service share: {}", share.getId());
                    
                } catch (Exception e) {
                    log.error("Failed to create service share: {}", shareNode, e);
                }
            });
    }

    private void seedServiceInstances(JsonNode instances) {
        if (instances == null || !instances.isArray()) {
            log.warn("No service instances found in seed data");
            return;
        }
        
        log.info("Seeding {} service instances", instances.size());
        
        StreamSupport.stream(instances.spliterator(), false)
            .forEach(instanceNode -> {
                try {
                    ServiceInstance instance = ServiceInstance.builder()
                        .id(ServiceInstanceId.of(
                            instanceNode.get("instanceId").asText()
                        ))
                        .serviceId(instanceNode.has("serviceId") ? instanceNode.get("serviceId").asText() : null)
                        .teamId(instanceNode.has("teamId") && !instanceNode.get("teamId").isNull() 
                            ? instanceNode.get("teamId").asText() : null)
                        .host(instanceNode.has("host") ? instanceNode.get("host").asText() : null)
                        .port(instanceNode.has("port") ? instanceNode.get("port").asInt() : null)
                        .environment(instanceNode.has("environment") ? instanceNode.get("environment").asText() : null)
                        .version(instanceNode.has("version") ? instanceNode.get("version").asText() : null)
                        .status(instanceNode.has("status") 
                            ? ServiceInstance.InstanceStatus.valueOf(instanceNode.get("status").asText())
                            : ServiceInstance.InstanceStatus.UNKNOWN)
                        .hasDrift(instanceNode.has("hasDrift") && instanceNode.get("hasDrift").asBoolean())
                        .lastSeenAt(Instant.now())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                    
                    serviceInstanceService.save(instance);
                    log.debug("Created service instance: {}", instance.getId());
                    
                } catch (Exception e) {
                    log.error("Failed to create service instance: {}", instanceNode, e);
                }
            });
    }

    private void seedDriftEvents(JsonNode events) {
        if (events == null || !events.isArray()) {
            log.warn("No drift events found in seed data");
            return;
        }
        
        log.info("Seeding {} drift events", events.size());
        
        StreamSupport.stream(events.spliterator(), false)
            .forEach(eventNode -> {
                try {
                    DriftEvent event = DriftEvent.builder()
                        .id(DriftEventId.of(java.util.UUID.randomUUID().toString()))
                        .serviceName(eventNode.get("serviceName").asText())
                        .instanceId(eventNode.get("instanceId").asText())
                        .serviceId(eventNode.has("serviceId") ? eventNode.get("serviceId").asText() : null)
                        .teamId(eventNode.has("teamId") && !eventNode.get("teamId").isNull() 
                            ? eventNode.get("teamId").asText() : null)
                        .environment(eventNode.has("environment") ? eventNode.get("environment").asText() : null)
                        .status(DriftEvent.DriftStatus.valueOf(eventNode.get("status").asText()))
                        .notes(eventNode.has("description") ? eventNode.get("description").asText() : null)
                        .detectedAt(Instant.parse(eventNode.get("detectedAt").asText()))
                        .build();
                    
                    driftEventService.save(event);
                    log.debug("Created drift event: {}", event.getId());
                    
                } catch (Exception e) {
                    log.error("Failed to create drift event: {}", eventNode, e);
                }
            });
    }
}
