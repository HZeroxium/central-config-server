package com.example.control.api.controller.infra;

import com.example.control.infrastructure.seeding.service.DataSeederService;
import com.example.control.infrastructure.seeding.service.DataSeederService.CleanResult;
import com.example.control.infrastructure.seeding.service.DataSeederService.CombinedResult;
import com.example.control.infrastructure.seeding.service.DataSeederService.SeedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * REST controller for managing mock data seeding operations.
 * <p>
 * This controller provides endpoints to clean existing mock data, seed new mock
 * data,
 * or perform both operations atomically. It is only enabled when seeding
 * configuration
 * is active (seeding.enabled=true).
 * </p>
 * <p>
 * <strong>Security:</strong> All endpoints require SYS_ADMIN role OR execution
 * in
 * dev/local/seed-data profiles for development convenience. Production
 * environments
 * should NEVER have these profiles active.
 * </p>
 *
 * @see DataSeederService
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/seed")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "seeding.enabled", havingValue = "true")
@Tag(name = "Data Seeding", description = "Operations for managing mock data generation (Admin only)")
public class SeederController {

    private final DataSeederService seederService;
    private final Environment environment;

    /**
     * Check if seeding operations are allowed in current environment.
     * Allowed if:
     * - User has SYS_ADMIN role, OR
     * - Running in dev/local/seed-data profile
     *
     * @return true if allowed
     */
    private boolean isSeedingAllowed() {
        // Check if running in development-friendly profiles
        String[] profiles = environment.getActiveProfiles();
        boolean isDevProfile = Arrays.stream(profiles)
                .anyMatch(p -> p.equals("dev") || p.equals("local") || p.equals("seed-data"));

        if (isDevProfile) {
            log.debug("Seeding allowed: running in dev-friendly profile");
            return true;
        }

        // In production, only SYS_ADMIN can seed
        log.debug("Seeding requires SYS_ADMIN role in production profile");
        return false; // Will be checked by @PreAuthorize
    }

    /**
     * Clean all mock data from the database.
     * <p>
     * This endpoint deletes all entities in the following order (respecting
     * dependencies):
     * <ol>
     * <li>ApprovalDecision</li>
     * <li>ApprovalRequest</li>
     * <li>ServiceShare</li>
     * <li>DriftEvent</li>
     * <li>ServiceInstance</li>
     * <li>ApplicationService</li>
     * </ol>
     * IAM entities (IamUser, IamTeam) are NOT cleaned.
     * </p>
     *
     * @return CleanResult containing counts of deleted entities for each type
     */
    @DeleteMapping("/clean")
    @PreAuthorize("hasRole('SYS_ADMIN') or @seederController.isSeedingAllowed()")
    @Operation(summary = "Clean all mock data", description = "Removes all application services, instances, drift events, shares, "
            +
            "approval requests, and decisions. IAM data (users/teams) is preserved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data cleaned successfully", content = @Content(schema = @Schema(implementation = CleanResult.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYS_ADMIN role or dev profile"),
            @ApiResponse(responseCode = "500", description = "Internal server error during cleaning operation")
    })
    public ResponseEntity<CleanResult> cleanData() {
        log.info("Received request to clean mock data");

        try {
            CleanResult result = seederService.cleanAll();

            log.info("Mock data cleaned successfully: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to clean mock data", e);
            throw new RuntimeException("Failed to clean mock data: " + e.getMessage(), e);
        }
    }

    /**
     * Seed new mock data into the database.
     * <p>
     * This endpoint generates and saves mock data in the following order
     * (respecting dependencies):
     * <ol>
     * <li>ApplicationService</li>
     * <li>ServiceInstance</li>
     * <li>DriftEvent</li>
     * <li>ServiceShare</li>
     * <li>ApprovalRequest</li>
     * <li>ApprovalDecision</li>
     * </ol>
     * Generated data is based on configuration in application.yml (seeding.data.*).
     * </p>
     * <p>
     * NOTE: This endpoint does NOT clean existing data first. If you need a fresh
     * start,
     * use the clean-and-seed endpoint instead.
     * </p>
     *
     * @return SeedResult containing counts of created entities for each type
     */
    @PostMapping("/seed")
    @PreAuthorize("hasRole('SYS_ADMIN') or @seederController.isSeedingAllowed()")
    @Operation(summary = "Seed new mock data", description = "Generates and saves new mock data (services, instances, drift events, etc.) "
            +
            "based on seeding configuration. Does not clean existing data first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data seeded successfully", content = @Content(schema = @Schema(implementation = SeedResult.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYS_ADMIN role or dev profile"),
            @ApiResponse(responseCode = "500", description = "Internal server error during seeding operation")
    })
    public ResponseEntity<SeedResult> seedData() {
        log.info("Received request to seed mock data");

        try {
            SeedResult result = seederService.seed();

            log.info("Mock data seeded successfully: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to seed mock data", e);
            throw new RuntimeException("Failed to seed mock data: " + e.getMessage(), e);
        }
    }

    /**
     * Clean all existing mock data and then seed fresh data (atomic operation).
     * <p>
     * This endpoint is a convenience operation that combines clean and seed in a
     * single transaction.
     * It ensures you always start with a known state by removing all existing data
     * before seeding.
     * </p>
     * <p>
     * This is the recommended endpoint for:
     * <ul>
     * <li>Development environment reset</li>
     * <li>Integration test setup</li>
     * <li>Demo environment refresh</li>
     * </ul>
     * </p>
     *
     * @return CombinedResult containing both clean and seed results
     */
    @PostMapping("/clean-and-seed")
    @PreAuthorize("hasRole('SYS_ADMIN') or @seederController.isSeedingAllowed()")
    @Operation(summary = "Clean and seed mock data", description = "Atomically removes all existing mock data and generates fresh data. "
            +
            "This is the recommended endpoint for environment resets.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data cleaned and seeded successfully", content = @Content(schema = @Schema(implementation = CombinedResult.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYS_ADMIN role or dev profile"),
            @ApiResponse(responseCode = "500", description = "Internal server error during clean-and-seed operation")
    })
    public ResponseEntity<CombinedResult> cleanAndSeedData() {
        log.info("Received request to clean and seed mock data");

        try {
            CombinedResult result = seederService.cleanAndSeed();

            log.info("Mock data cleaned and seeded successfully: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to clean and seed mock data", e);
            throw new RuntimeException("Failed to clean and seed mock data: " + e.getMessage(), e);
        }
    }
}
