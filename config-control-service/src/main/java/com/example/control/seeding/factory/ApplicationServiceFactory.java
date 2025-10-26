package com.example.control.seeding.factory;

import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.object.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Factory for generating realistic {@link ApplicationService} mock data.
 * <p>
 * Generates diverse application services with semantic naming, realistic
 * attributes,
 * and proper lifecycle states suitable for testing filtering and querying.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Service IDs: kebab-case names derived from tech/business terms</li>
 * <li>Display Names: Human-readable service names</li>
 * <li>Environments: Realistic distribution (dev, staging, prod)</li>
 * <li>Tags: Context-appropriate categorization</li>
 * <li>Lifecycle: Mix of ACTIVE, DEPRECATED, RETIRED states</li>
 * <li>Timestamps: Staggered creation times over past 6 months</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationServiceFactory {

  private final Faker faker;

  /**
   * Service name templates for generating semantic service names.
   */
  private static final List<String> SERVICE_PREFIXES = List.of(
      "payment", "order", "inventory", "user", "auth", "notification",
      "analytics", "reporting", "catalog", "shipping", "billing",
      "customer", "product", "pricing", "search", "recommendation");

  /**
   * Service type suffixes for additional context.
   */
  private static final List<String> SERVICE_SUFFIXES = List.of(
      "service", "api", "processor", "worker", "gateway", "manager", "handler");

  /**
   * Technology and architecture tags.
   */
  private static final List<String> TECH_TAGS = List.of(
      "microservice", "spring-boot", "java", "rest-api", "grpc",
      "kafka", "redis", "mongodb", "postgres", "kubernetes");

  /**
   * Business domain tags.
   */
  private static final List<String> DOMAIN_TAGS = List.of(
      "core", "backend", "frontend", "infrastructure", "platform",
      "payment", "fulfillment", "analytics", "customer-facing", "internal");

  /**
   * Service priority/criticality tags.
   */
  private static final List<String> PRIORITY_TAGS = List.of(
      "critical", "high-priority", "production", "staging-only", "experimental");

  /**
   * Generates a single {@link ApplicationService} with realistic attributes.
   *
   * @param index       service index for unique ID generation
   * @param ownerTeamId team that owns this service (null for orphan)
   * @param createdBy   user ID who created the service
   * @return generated application service
   */
  public ApplicationService generate(int index, String ownerTeamId, String createdBy) {
    String serviceId = generateServiceId(index);
    String displayName = generateDisplayName(serviceId);

    List<String> environments = generateEnvironments();
    List<String> tags = generateTags();
    ApplicationService.ServiceLifecycle lifecycle = generateLifecycle();

    Instant createdAt = generateCreatedAt();
    Instant updatedAt = generateUpdatedAt(createdAt);

    Map<String, String> attributes = generateAttributes();

    log.debug("Generated service: id={}, displayName={}, owner={}, lifecycle={}",
        serviceId, displayName, ownerTeamId, lifecycle);

    return ApplicationService.builder()
        .id(ApplicationServiceId.of(serviceId))
        .displayName(displayName)
        .ownerTeamId(ownerTeamId)
        .environments(environments)
        .tags(tags)
        .repoUrl(generateRepoUrl(serviceId))
        .lifecycle(lifecycle)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .createdBy(createdBy)
        .attributes(attributes)
        .build();
  }

  /**
   * Generates a kebab-case service ID.
   *
   * @param index service index
   * @return service ID
   */
  private String generateServiceId(int index) {
    String prefix = SERVICE_PREFIXES.get(index % SERVICE_PREFIXES.size());
    String suffix = SERVICE_SUFFIXES.get(faker.random().nextInt(SERVICE_SUFFIXES.size()));

    // Add numeric suffix if index is high to ensure uniqueness
    if (index >= SERVICE_PREFIXES.size()) {
      int variant = (index / SERVICE_PREFIXES.size()) + 1;
      return String.format("%s-%s-%d", prefix, suffix, variant);
    }

    return String.format("%s-%s", prefix, suffix);
  }

  /**
   * Generates a human-readable display name from service ID.
   *
   * @param serviceId service ID
   * @return display name
   */
  private String generateDisplayName(String serviceId) {
    // Convert kebab-case to Title Case
    String[] parts = serviceId.split("-");
    StringBuilder displayName = new StringBuilder();

    for (String part : parts) {
      if (!displayName.isEmpty()) {
        displayName.append(" ");
      }
      displayName.append(capitalize(part));
    }

    return displayName.toString();
  }

  /**
   * Capitalizes first letter of a string.
   *
   * @param str input string
   * @return capitalized string
   */
  private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  /**
   * Generates list of deployment environments.
   * Distribution: 60% all envs, 30% dev+staging, 10% dev only.
   *
   * @return list of environments
   */
  private List<String> generateEnvironments() {
    int roll = faker.random().nextInt(100);

    if (roll < 60) {
      // Full deployment pipeline
      return List.of("dev", "staging", "prod");
    } else if (roll < 90) {
      // Pre-production only
      return List.of("dev", "staging");
    } else {
      // Development only
      return List.of("dev");
    }
  }

  /**
   * Generates service tags combining tech, domain, and priority.
   *
   * @return list of tags (2-5 tags)
   */
  private List<String> generateTags() {
    Set<String> tags = new HashSet<>();

    // Always add 1 tech tag
    tags.add(TECH_TAGS.get(faker.random().nextInt(TECH_TAGS.size())));

    // Add 1-2 domain tags
    tags.add(DOMAIN_TAGS.get(faker.random().nextInt(DOMAIN_TAGS.size())));
    if (faker.random().nextBoolean()) {
      tags.add(DOMAIN_TAGS.get(faker.random().nextInt(DOMAIN_TAGS.size())));
    }

    // Add priority tag with 40% probability
    if (faker.random().nextInt(100) < 40) {
      tags.add(PRIORITY_TAGS.get(faker.random().nextInt(PRIORITY_TAGS.size())));
    }

    return new ArrayList<>(tags);
  }

  /**
   * Generates service lifecycle state.
   * Distribution: 80% ACTIVE, 15% DEPRECATED, 5% RETIRED.
   *
   * @return lifecycle state
   */
  private ApplicationService.ServiceLifecycle generateLifecycle() {
    int roll = faker.random().nextInt(100);

    if (roll < 80) {
      return ApplicationService.ServiceLifecycle.ACTIVE;
    } else if (roll < 95) {
      return ApplicationService.ServiceLifecycle.DEPRECATED;
    } else {
      return ApplicationService.ServiceLifecycle.RETIRED;
    }
  }

  /**
   * Generates repository URL.
   *
   * @param serviceId service ID
   * @return git repository URL
   */
  private String generateRepoUrl(String serviceId) {
    String org = faker.company().name()
        .toLowerCase()
        .replaceAll("[^a-z0-9-]", "-");
    return String.format("https://github.com/%s/%s", org, serviceId);
  }

  /**
   * Generates creation timestamp between 1-180 days ago.
   *
   * @return creation instant
   */
  private Instant generateCreatedAt() {
    long daysAgo = faker.number().numberBetween(1, 180);
    return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
  }

  /**
   * Generates update timestamp between creation and now.
   *
   * @param createdAt creation timestamp
   * @return update instant
   */
  private Instant generateUpdatedAt(Instant createdAt) {
    long daysSinceCreation = ChronoUnit.DAYS.between(createdAt, Instant.now());
    if (daysSinceCreation <= 0) {
      return createdAt;
    }

    long updateDaysAgo = faker.number().numberBetween(0, daysSinceCreation);
    return Instant.now().minus(updateDaysAgo, ChronoUnit.DAYS);
  }

  /**
   * Generates service attributes (metadata).
   *
   * @return attributes map
   */
  private Map<String, String> generateAttributes() {
    Map<String, String> attributes = new HashMap<>();

    attributes.put("framework", faker.options().option("spring-boot", "quarkus", "micronaut"));
    attributes.put("language", faker.options().option("java-21", "java-17", "java-11"));
    attributes.put("build-tool", faker.options().option("gradle", "maven"));
    attributes.put("container-runtime", "docker");
    attributes.put("orchestration", "kubernetes");

    // Add version with 70% probability
    if (faker.random().nextInt(100) < 70) {
      String version = String.format("%d.%d.%d",
          faker.number().numberBetween(1, 5),
          faker.number().numberBetween(0, 20),
          faker.number().numberBetween(0, 50));
      attributes.put("version", version);
    }

    return attributes;
  }
}
