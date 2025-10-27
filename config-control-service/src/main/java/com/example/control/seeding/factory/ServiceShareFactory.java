package com.example.control.seeding.factory;

import com.example.control.domain.id.ServiceShareId;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ServiceShare;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Factory for generating realistic {@link ServiceShare} mock data.
 * <p>
 * Generates service shares between teams with varied permission levels,
 * environment scoping, and expiration policies suitable for testing
 * access control and sharing workflows.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Grantee Type: TEAM (90%), USER (10%)</li>
 * <li>Permissions: Mix of VIEW and EDIT permissions</li>
 * <li>Environments: All (50%), Specific (50%)</li>
 * <li>Expiration: No expiry (70%), Future expiry (30%)</li>
 * <li>Cross-team sharing scenarios</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceShareFactory {

  private final Faker faker;

  /**
   * View-only permission sets.
   */
  private static final List<List<ServiceShare.SharePermission>> VIEW_PERMISSION_SETS = List.of(
      List.of(ServiceShare.SharePermission.VIEW_SERVICE),
      List.of(ServiceShare.SharePermission.VIEW_SERVICE, ServiceShare.SharePermission.VIEW_INSTANCE),
      List.of(ServiceShare.SharePermission.VIEW_SERVICE, ServiceShare.SharePermission.VIEW_INSTANCE,
          ServiceShare.SharePermission.VIEW_DRIFT));

  /**
   * Edit permission sets (include view permissions).
   */
  private static final List<List<ServiceShare.SharePermission>> EDIT_PERMISSION_SETS = List.of(
      List.of(ServiceShare.SharePermission.VIEW_SERVICE, ServiceShare.SharePermission.VIEW_INSTANCE,
          ServiceShare.SharePermission.EDIT_SERVICE),
      List.of(ServiceShare.SharePermission.VIEW_SERVICE, ServiceShare.SharePermission.VIEW_INSTANCE,
          ServiceShare.SharePermission.EDIT_INSTANCE),
      List.of(ServiceShare.SharePermission.VIEW_SERVICE, ServiceShare.SharePermission.VIEW_INSTANCE,
          ServiceShare.SharePermission.VIEW_DRIFT, ServiceShare.SharePermission.EDIT_SERVICE,
          ServiceShare.SharePermission.EDIT_INSTANCE));

  /**
   * Environment sets for environment-specific shares.
   */
  private static final List<List<String>> ENVIRONMENT_SETS = List.of(
      List.of("dev"),
      List.of("dev", "staging"),
      List.of("staging", "prod"),
      List.of("prod"));

  /**
   * Generates a {@link ServiceShare} between service owner and another team.
   *
   * @param service       service being shared
   * @param grantToTeamId team receiving the share
   * @param grantedBy     user who created the share
   * @param shareType     type of share (view or edit)
   * @return generated service share
   */
  public ServiceShare generate(ApplicationService service, String grantToTeamId,
      String grantedBy, ShareType shareType) {

    ServiceShare.GranteeType granteeType = selectGranteeType();
    String grantToId = (granteeType == ServiceShare.GranteeType.TEAM)
        ? grantToTeamId
        : generateUserId();

    List<ServiceShare.SharePermission> permissions = selectPermissions(shareType);
    List<String> environments = selectEnvironments(service);

    Instant createdAt = generateCreatedAt();
    Instant updatedAt = createdAt;
    Instant expiresAt = generateExpiresAt(createdAt);

    log.debug("Generated share: service={} grantTo={}:{} permissions={} envs={}",
        service.getId().id(), granteeType, grantToId, permissions.size(),
        environments == null ? "all" : environments.size());

    return ServiceShare.builder()
        .id(ServiceShareId.of(UUID.randomUUID().toString()))
        .resourceLevel(ServiceShare.ResourceLevel.SERVICE)
        .serviceId(service.getId().id())
        .instanceId(null)
        .grantToType(granteeType)
        .grantToId(grantToId)
        .permissions(permissions)
        .environments(environments)
        .grantedBy(grantedBy)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .expiresAt(expiresAt)
        .build();
  }

  /**
   * Selects grantee type.
   * Distribution: TEAM (90%), USER (10%)
   *
   * @return grantee type
   */
  private ServiceShare.GranteeType selectGranteeType() {
    return (faker.random().nextInt(100) < 90)
        ? ServiceShare.GranteeType.TEAM
        : ServiceShare.GranteeType.USER;
  }

  /**
   * Generates a mock user ID.
   *
   * @return user ID
   */
  private String generateUserId() {
    return faker.options().option("user1", "user2", "user3", "john.doe", "jane.smith");
  }

  /**
   * Selects permissions based on share type.
   *
   * @param shareType share type (view or edit)
   * @return list of permissions
   */
  private List<ServiceShare.SharePermission> selectPermissions(ShareType shareType) {
    List<List<ServiceShare.SharePermission>> sets = (shareType == ShareType.VIEW)
        ? VIEW_PERMISSION_SETS
        : EDIT_PERMISSION_SETS;

    return new ArrayList<>(sets.get(faker.random().nextInt(sets.size())));
  }

  /**
   * Selects environments for the share.
   * Distribution: All environments (50%), Specific environments (50%)
   *
   * @param service service being shared
   * @return list of environments or null for all
   */
  private List<String> selectEnvironments(ApplicationService service) {
    // 50% chance of no environment filter (all environments)
    if (faker.random().nextBoolean()) {
      return null;
    }

    // Select subset of service's environments
    List<String> serviceEnvs = service.getEnvironments();
    if (serviceEnvs == null || serviceEnvs.isEmpty()) {
      return null;
    }

    // Pick random environment set that overlaps with service environments
    List<List<String>> validSets = ENVIRONMENT_SETS.stream()
        .filter(set -> set.stream().anyMatch(serviceEnvs::contains))
        .toList();

    if (validSets.isEmpty()) {
      return null;
    }

    List<String> selectedSet = validSets.get(faker.random().nextInt(validSets.size()));

    // Filter to only include service's environments
    return selectedSet.stream()
        .filter(serviceEnvs::contains)
        .toList();
  }

  /**
   * Generates share creation timestamp with mixed distribution.
   * <p>
   * Strategy: 50% recent (1-7 days ago), 50% older (30-90 days ago).
   * This allows testing of time-based filtering and sorting.
   * </p>
   *
   * @return creation instant
   */
  private Instant generateCreatedAt() {
    long daysAgo;

    if (faker.random().nextBoolean()) {
      // 50% recent: 1-7 days ago
      daysAgo = faker.number().numberBetween(1, 8);
    } else {
      // 50% older: 30-90 days ago
      daysAgo = faker.number().numberBetween(30, 91);
    }

    return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
  }

  /**
   * Generates expiration timestamp.
   * Distribution: No expiry (70%), Future expiry 30-180 days (30%)
   *
   * @param createdAt creation timestamp
   * @return expiration instant or null
   */
  private Instant generateExpiresAt(Instant createdAt) {
    // 70% no expiration
    if (faker.random().nextInt(100) < 70) {
      return null;
    }

    // 30% expiration 30-180 days from creation
    long daysToExpire = faker.number().numberBetween(30, 180);
    return createdAt.plus(daysToExpire, ChronoUnit.DAYS);
  }

  /**
   * Share type enumeration for generation.
   */
  public enum ShareType {
    VIEW,
    EDIT
  }
}
