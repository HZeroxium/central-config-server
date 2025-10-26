package com.example.control.seeding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Configuration properties for the data seeding system.
 * <p>
 * Binds to {@code seeding.*} properties in application.yml and provides
 * type-safe access to seeding configuration with validation.
 * </p>
 *
 * <p>
 * Example configuration:
 * 
 * <pre>
 * seeding:
 *   enabled: true
 *   auto-run-on-startup: true
 *   clean-before-seed: true
 *   data:
 *     teams:
 *       count: 2
 *       ids: [team1, team2]
 *     services:
 *       team1-count: 3
 *       team2-count: 3
 *       orphan-count: 2
 * </pre>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "seeding")
public class SeederConfigProperties {

  /**
   * Whether seeding is enabled globally.
   * Default: false (only enable in dev/seed-data profile).
   */
  private boolean enabled = false;

  /**
   * Whether to automatically run seeding on application startup.
   * Default: false (manual trigger via API only).
   */
  private boolean autoRunOnStartup = false;

  /**
   * Whether to clean existing data before seeding.
   * Default: true (ensures idempotency).
   */
  private boolean cleanBeforeSeed = true;

  /**
   * Data generation configuration.
   */
  @NotNull(message = "Data configuration is required")
  private DataConfig data = new DataConfig();

  /**
   * Admin user configuration for seeding operations.
   */
  @NotNull(message = "Admin configuration is required")
  private AdminConfig admin = new AdminConfig();

  /**
   * Data generation configuration for各entity types.
   */
  @Data
  public static class DataConfig {

    /**
     * Team configuration (IAM teams, not generated but referenced).
     */
    @NotNull(message = "Teams configuration is required")
    private TeamsConfig teams = new TeamsConfig();

    /**
     * Application services configuration.
     */
    @NotNull(message = "Services configuration is required")
    private ServicesConfig services = new ServicesConfig();

    /**
     * Service instances per service configuration.
     */
    @NotNull(message = "Instances per service configuration is required")
    private InstancesPerServiceConfig instancesPerService = new InstancesPerServiceConfig();

    /**
     * Drift events configuration.
     */
    @NotNull(message = "Drift events configuration is required")
    private DriftEventsConfig driftEvents = new DriftEventsConfig();

    /**
     * Service shares configuration.
     */
    @NotNull(message = "Shares configuration is required")
    private SharesConfig shares = new SharesConfig();

    /**
     * Approval requests configuration.
     */
    @NotNull(message = "Approval requests configuration is required")
    private ApprovalRequestsConfig approvalRequests = new ApprovalRequestsConfig();
  }

  /**
   * Team configuration (IAM teams).
   */
  @Data
  public static class TeamsConfig {
    /**
     * Number of teams to use (must match existing Keycloak teams).
     */
    @Min(value = 1, message = "Team count must be at least 1")
    private int count = 2;

    /**
     * Team IDs to use (must exist in Keycloak).
     */
    @NotEmpty(message = "Team IDs cannot be empty")
    private List<String> ids = List.of("team1", "team2");
  }

  /**
   * Application services configuration.
   */
  @Data
  public static class ServicesConfig {
    /**
     * Number of services owned by team1.
     */
    @Min(value = 0, message = "Team1 count must be non-negative")
    private int team1Count = 3;

    /**
     * Number of services owned by team2.
     */
    @Min(value = 0, message = "Team2 count must be non-negative")
    private int team2Count = 3;

    /**
     * Number of orphan services (no owner team).
     */
    @Min(value = 0, message = "Orphan count must be non-negative")
    private int orphanCount = 2;
  }

  /**
   * Service instances per service configuration.
   */
  @Data
  public static class InstancesPerServiceConfig {
    /**
     * Minimum instances per service.
     */
    @Min(value = 1, message = "Minimum instances must be at least 1")
    private int min = 5;

    /**
     * Maximum instances per service.
     */
    @Min(value = 1, message = "Maximum instances must be at least 1")
    private int max = 10;
  }

  /**
   * Drift events configuration.
   */
  @Data
  public static class DriftEventsConfig {
    /**
     * Minimum drift events per service.
     */
    @Min(value = 0, message = "Minimum drift events must be non-negative")
    private int minPerService = 2;

    /**
     * Maximum drift events per service.
     */
    @Min(value = 0, message = "Maximum drift events must be non-negative")
    private int maxPerService = 5;
  }

  /**
   * Service shares configuration.
   */
  @Data
  public static class SharesConfig {
    /**
     * Total number of service shares to generate.
     */
    @Min(value = 0, message = "Shares count must be non-negative")
    private int count = 5;
  }

  /**
   * Approval requests configuration.
   */
  @Data
  public static class ApprovalRequestsConfig {
    /**
     * Number of pending approval requests.
     */
    @Min(value = 0, message = "Pending count must be non-negative")
    private int pending = 2;

    /**
     * Number of approved requests.
     */
    @Min(value = 0, message = "Approved count must be non-negative")
    private int approved = 3;

    /**
     * Number of rejected requests.
     */
    @Min(value = 0, message = "Rejected count must be non-negative")
    private int rejected = 1;
  }

  /**
   * Admin user configuration.
   */
  @Data
  public static class AdminConfig {
    /**
     * Admin user ID for approval operations.
     * Must correspond to a valid Keycloak user with SYS_ADMIN role.
     */
    @NotNull(message = "Admin user ID is required")
    private String userId = "admin";
  }

  /**
   * Calculate total number of services to generate.
   *
   * @return total service count
   */
  public int getTotalServiceCount() {
    return data.services.team1Count + data.services.team2Count + data.services.orphanCount;
  }

  /**
   * Get team ID by index.
   *
   * @param index team index (0-based)
   * @return team ID
   */
  public String getTeamId(int index) {
    if (index < 0 || index >= data.teams.ids.size()) {
      throw new IndexOutOfBoundsException("Team index out of bounds: " + index);
    }
    return data.teams.ids.get(index);
  }
}
