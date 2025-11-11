package com.example.control.infrastructure.seeding.runner;

import com.example.control.infrastructure.seeding.config.SeederConfigProperties;
import com.example.control.infrastructure.seeding.service.DataSeederService;
import com.example.control.infrastructure.seeding.service.DataSeederService.CombinedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Application runner that automatically seeds mock data on startup.
 * <p>
 * This runner is conditionally enabled based on application properties:
 * <ul>
 * <li>seeding.enabled=true - Enables seeding functionality</li>
 * <li>seeding.auto-run-on-startup=true - Triggers auto-seeding on startup</li>
 * </ul>
 * </p>
 * <p>
 * Behavior is controlled by seeding.clean-before-seed property:
 * <ul>
 * <li>true - Performs clean-and-seed (removes existing data first)</li>
 * <li>false - Performs seed only (adds to existing data)</li>
 * </ul>
 * </p>
 * <p>
 * This runner executes after Spring Boot has fully initialized all beans
 * but before the application starts accepting requests, making it ideal
 * for development environment setup.
 * </p>
 *
 * @see DataSeederService
 * @see SeederConfigProperties
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "seeding", name = { "enabled", "auto-run-on-startup" }, havingValue = "true")
public class SeederApplicationRunner implements ApplicationRunner {

    private final DataSeederService seederService;
    private final SeederConfigProperties config;

    /**
     * Execute the seeding operation on application startup.
     * <p>
     * This method is called after the application context is refreshed and
     * all beans are initialized. It checks the configuration to determine
     * whether to clean existing data before seeding.
     * </p>
     * <p>
     * Seeding failures are logged but do not prevent application startup.
     * This ensures that the application can still start even if mock data
     * generation fails (e.g., due to database connectivity issues).
     * </p>
     *
     * @param args application arguments (not used)
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("=".repeat(80));
        log.info("Auto-seeding mock data on startup");
        log.info("Clean before seed: {}", config.isCleanBeforeSeed());
        log.info("=".repeat(80));

        try {
            if (config.isCleanBeforeSeed()) {
                log.info("Performing clean-and-seed operation...");
                CombinedResult result = seederService.cleanAndSeed();
                logCombinedResult(result);
            } else {
                log.info("Performing seed-only operation (preserving existing data)...");
                DataSeederService.SeedResult result = seederService.seed();
                logSeedResult(result);
            }

            log.info("=".repeat(80));
            log.info("Mock data seeding completed successfully");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("FAILED to seed mock data on startup", e);
            log.error("Application will continue to start but may have insufficient test data");
            log.error("=".repeat(80));
            // Don't throw exception - allow application to start even if seeding fails
        }
    }

    /**
     * Log detailed results of clean-and-seed operation.
     */
    private void logCombinedResult(CombinedResult result) {
        log.info("Clean-and-seed operation completed:");
        log.info("  Cleaned:");
        log.info("    - {} approval decisions", result.cleanResult.approvalDecisionsDeleted);
        log.info("    - {} approval requests", result.cleanResult.approvalRequestsDeleted);
        log.info("    - {} service shares", result.cleanResult.sharesDeleted);
        log.info("    - {} drift events", result.cleanResult.driftEventsDeleted);
        log.info("    - {} service instances", result.cleanResult.instancesDeleted);
        log.info("    - {} application services", result.cleanResult.servicesDeleted);
        log.info("    - {} KV entries", result.cleanResult.kvEntriesDeleted);
        log.info("  Seeded:");
        log.info("    - {} application services", result.seedResult.servicesSeeded);
        log.info("    - {} service instances", result.seedResult.instancesSeeded);
        log.info("    - {} drift events", result.seedResult.driftEventsSeeded);
        log.info("    - {} service shares", result.seedResult.sharesSeeded);
        log.info("    - {} approval requests", result.seedResult.approvalRequestsSeeded);
        log.info("    - {} approval decisions", result.seedResult.approvalDecisionsSeeded);
        log.info("    - {} KV entries", result.seedResult.kvEntriesSeeded);
        log.info("  Total: Deleted {}, Seeded {}",
                result.cleanResult.getTotalDeleted(),
                result.seedResult.getTotalSeeded());
    }

    /**
     * Log detailed results of seed-only operation.
     */
    private void logSeedResult(DataSeederService.SeedResult result) {
        log.info("Seed operation completed:");
        log.info("  - {} application services", result.servicesSeeded);
        log.info("  - {} service instances", result.instancesSeeded);
        log.info("  - {} drift events", result.driftEventsSeeded);
        log.info("  - {} service shares", result.sharesSeeded);
        log.info("  - {} approval requests", result.approvalRequestsSeeded);
        log.info("  - {} approval decisions", result.approvalDecisionsSeeded);
        log.info("  - {} KV entries", result.kvEntriesSeeded);
        log.info("  Total: Seeded {}", result.getTotalSeeded());
    }
}
