package com.example.control.infrastructure.seeding.factory;

import com.example.control.domain.valueobject.id.DriftEventId;
import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.model.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Factory for generating realistic {@link DriftEvent} mock data.
 * <p>
 * Generates diverse drift events with varied severities, statuses, and time
 * distributions
 * suitable for testing drift detection, filtering, and resolution workflows.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Severity: LOW (25%), MEDIUM (35%), HIGH (30%), CRITICAL (10%)</li>
 * <li>Status: DETECTED (40%), ACKNOWLEDGED (20%), RESOLVING (10%), RESOLVED
 * (25%), IGNORED (5%)</li>
 * <li>Time Distribution: last 7 days (50%), 7-30 days (30%), 30+ days
 * (20%)</li>
 * <li>Resolved Events: Have resolution timestamp and resolver ID</li>
 * <li>Notes: Realistic descriptions of drift causes and resolutions</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriftEventFactory {

    /**
     * Drift severity distribution.
     */
    private static final List<DriftEvent.DriftSeverity> SEVERITIES = List.of(
            DriftEvent.DriftSeverity.LOW, DriftEvent.DriftSeverity.LOW, // 25%
            DriftEvent.DriftSeverity.MEDIUM, DriftEvent.DriftSeverity.MEDIUM,
            DriftEvent.DriftSeverity.MEDIUM, DriftEvent.DriftSeverity.MEDIUM, // 35%
            DriftEvent.DriftSeverity.HIGH, DriftEvent.DriftSeverity.HIGH,
            DriftEvent.DriftSeverity.HIGH, // 30%
            DriftEvent.DriftSeverity.CRITICAL // 10%
    );
    /**
     * Drift status distribution.
     */
    private static final List<DriftEvent.DriftStatus> STATUSES = List.of(
            DriftEvent.DriftStatus.DETECTED, DriftEvent.DriftStatus.DETECTED,
            DriftEvent.DriftStatus.DETECTED, DriftEvent.DriftStatus.DETECTED, // 40%
            DriftEvent.DriftStatus.ACKNOWLEDGED, DriftEvent.DriftStatus.ACKNOWLEDGED, // 20%
            DriftEvent.DriftStatus.RESOLVING, // 10%
            DriftEvent.DriftStatus.RESOLVED, DriftEvent.DriftStatus.RESOLVED,
            DriftEvent.DriftStatus.RESOLVED, // 25%
            DriftEvent.DriftStatus.IGNORED // 5%
    );
    /**
     * Sample drift notes for different severities.
     */
    private static final List<String> LOW_SEVERITY_NOTES = List.of(
            "Minor configuration property mismatch detected",
            "Non-critical environment variable differs from expected",
            "Logging level discrepancy observed",
            "Cache configuration drift detected",
            "Feature flag state differs");
    private static final List<String> MEDIUM_SEVERITY_NOTES = List.of(
            "Database connection pool size mismatch",
            "API rate limiting configuration drift",
            "Message queue settings differ from baseline",
            "Authentication timeout configuration changed",
            "Circuit breaker threshold modified");
    private static final List<String> HIGH_SEVERITY_NOTES = List.of(
            "Security configuration drift detected - requires immediate review",
            "Critical service endpoint configuration mismatch",
            "Production database connection string differs",
            "OAuth2 configuration drift detected",
            "API key rotation not synchronized");
    private static final List<String> CRITICAL_SEVERITY_NOTES = List.of(
            "CRITICAL: Production encryption keys differ from secure vault",
            "CRITICAL: Service mesh configuration completely out of sync",
            "CRITICAL: Load balancer health check configuration incorrect",
            "CRITICAL: Backup configuration disabled in production instance",
            "CRITICAL: Firewall rules differ from security baseline");
    /**
     * Resolution notes for resolved drifts.
     */
    private static final List<String> RESOLUTION_NOTES = List.of(
            "Configuration resynced from config server",
            "Manual correction applied and verified",
            "Automated remediation successful",
            "Instance restarted with correct configuration",
            "Config refresh triggered via Spring Cloud Bus",
            "Consul KV updated and instance refreshed",
            "Hot reload applied successfully");
    private final Faker faker;

    /**
     * Generates a {@link DriftEvent} for a service instance.
     *
     * @param serviceName service name
     * @param serviceId   service ID
     * @param teamId      team ID
     * @param instance    service instance that has drift
     * @return generated drift event
     */
    public DriftEvent generate(String serviceName, String serviceId, String teamId, ServiceInstance instance) {
        DriftEvent.DriftSeverity severity = selectSeverity();
        DriftEvent.DriftStatus status = selectStatus();

        Instant detectedAt = generateDetectedAt();
        Instant resolvedAt = shouldResolve(status) ? generateResolvedAt(detectedAt) : null;

        String detectedBy = "system"; // Automated detection
        String resolvedBy = (resolvedAt != null) ? selectResolver() : null;

        String notes = generateNotes(severity, status);

        String expectedHash = instance.getExpectedHash();
        String appliedHash = instance.getConfigHash();

        log.debug("Generated drift event: service={} instance={} severity={} status={}",
                serviceName, instance.getInstanceId(), severity, status);

        return DriftEvent.builder()
                .id(DriftEventId.of(UUID.randomUUID().toString()))
                .serviceName(serviceName)
                .instanceId(instance.getInstanceId())
                .serviceId(serviceId)
                .teamId(teamId)
                .environment(instance.getEnvironment())
                .expectedHash(expectedHash)
                .appliedHash(appliedHash)
                .severity(severity)
                .status(status)
                .detectedAt(detectedAt)
                .resolvedAt(resolvedAt)
                .detectedBy(detectedBy)
                .resolvedBy(resolvedBy)
                .notes(notes)
                .build();
    }

    /**
     * Selects drift severity based on distribution.
     *
     * @return drift severity
     */
    private DriftEvent.DriftSeverity selectSeverity() {
        return SEVERITIES.get(faker.random().nextInt(SEVERITIES.size()));
    }

    /**
     * Selects drift status based on distribution.
     *
     * @return drift status
     */
    private DriftEvent.DriftStatus selectStatus() {
        return STATUSES.get(faker.random().nextInt(STATUSES.size()));
    }

    /**
     * Determines if drift should be marked as resolved based on status.
     *
     * @param status drift status
     * @return true if should have resolution timestamp
     */
    private boolean shouldResolve(DriftEvent.DriftStatus status) {
        return status == DriftEvent.DriftStatus.RESOLVED;
    }

    /**
     * Selects resolver (user who resolved the drift).
     *
     * @return resolver user ID
     */
    private String selectResolver() {
        // 70% system auto-resolution, 30% manual by user
        if (faker.random().nextInt(100) < 70) {
            return "system";
        } else {
            return faker.options().option("admin", "devops-team", "sre-oncall", "team-lead");
        }
    }

    /**
     * Generates drift detection timestamp with realistic distribution.
     * Distribution: last 7 days (50%), 7-30 days (30%), 30+ days (20%)
     *
     * @return detection instant
     */
    private Instant generateDetectedAt() {
        int roll = faker.random().nextInt(100);

        if (roll < 50) {
            // Last 7 days (50%)
            long daysAgo = faker.number().numberBetween(0, 7);
            long hoursAgo = faker.number().numberBetween(0, 24);
            return Instant.now()
                    .minus(daysAgo, ChronoUnit.DAYS)
                    .minus(hoursAgo, ChronoUnit.HOURS);
        } else if (roll < 80) {
            // 7-30 days (30%)
            long daysAgo = faker.number().numberBetween(7, 30);
            return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        } else {
            // 30-90 days (20%)
            long daysAgo = faker.number().numberBetween(30, 90);
            return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        }
    }

    /**
     * Generates resolution timestamp after detection time.
     *
     * @param detectedAt detection timestamp
     * @return resolution instant
     */
    private Instant generateResolvedAt(Instant detectedAt) {
        // Resolution takes 1 hour to 3 days after detection
        long hoursToResolve = faker.number().numberBetween(1, 72);
        return detectedAt.plus(hoursToResolve, ChronoUnit.HOURS);
    }

    /**
     * Generates drift notes based on severity and status.
     *
     * @param severity drift severity
     * @param status   drift status
     * @return notes text
     */
    private String generateNotes(DriftEvent.DriftSeverity severity, DriftEvent.DriftStatus status) {
        StringBuilder notes = new StringBuilder();

        // Add severity-specific drift description
        switch (severity) {
            case LOW:
                notes.append(LOW_SEVERITY_NOTES.get(faker.random().nextInt(LOW_SEVERITY_NOTES.size())));
                break;
            case MEDIUM:
                notes.append(MEDIUM_SEVERITY_NOTES.get(faker.random().nextInt(MEDIUM_SEVERITY_NOTES.size())));
                break;
            case HIGH:
                notes.append(HIGH_SEVERITY_NOTES.get(faker.random().nextInt(HIGH_SEVERITY_NOTES.size())));
                break;
            case CRITICAL:
                notes.append(CRITICAL_SEVERITY_NOTES.get(faker.random().nextInt(CRITICAL_SEVERITY_NOTES.size())));
                break;
        }

        // Add resolution notes if resolved
        if (status == DriftEvent.DriftStatus.RESOLVED) {
            notes.append(". Resolution: ");
            notes.append(RESOLUTION_NOTES.get(faker.random().nextInt(RESOLUTION_NOTES.size())));
        } else if (status == DriftEvent.DriftStatus.ACKNOWLEDGED) {
            notes.append(". Investigation in progress.");
        } else if (status == DriftEvent.DriftStatus.RESOLVING) {
            notes.append(". Remediation steps being applied.");
        } else if (status == DriftEvent.DriftStatus.IGNORED) {
            notes.append(". Marked as acceptable deviation from baseline.");
        }

        return notes.toString();
    }
}
