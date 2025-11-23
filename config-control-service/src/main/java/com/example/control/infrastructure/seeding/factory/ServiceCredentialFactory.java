package com.example.control.infrastructure.seeding.factory;

import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.valueobject.id.ServiceCredentialId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Factory for generating realistic {@link ServiceCredential} mock data.
 * <p>
 * Generates service credentials for services that have been approved and
 * have ownership transferred to teams. Credentials are created with
 * Keycloak client IDs matching service display names.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceCredentialFactory {

    private final Faker faker;

    /**
     * Generates a service credential for an approved service.
     * <p>
     * Only generates credentials for services that have an owner team
     * (non-orphan services).
     * </p>
     *
     * @param service   the application service (must have ownerTeamId)
     * @param createdBy user ID who created the credential
     * @return generated service credential, or null if service is orphaned
     */
    public ServiceCredential generate(ApplicationService service, String createdBy) {
        // Only generate credentials for services with owners
        if (service.getOwnerTeamId() == null) {
            return null;
        }

        Instant createdAt = generateCreatedAt();
        Instant updatedAt = createdAt;

        // Generate status: mostly ACTIVE (80%), some PENDING (20%)
        ServiceCredential.CredentialStatus status = generateStatus();

        // Generate Keycloak client ID (matches service displayName)
        String keycloakClientId = service.getDisplayName();
        String keycloakClientUuid = UUID.randomUUID().toString(); // Mock UUID

        // Generate expiration (30% have expiration, 70% no expiration)
        Instant expiresAt = generateExpiresAt(createdAt);

        log.debug("Generated service credential: service={}, clientId={}, status={}",
                service.getId().id(), keycloakClientId, status);

        return ServiceCredential.builder()
                .id(ServiceCredentialId.of(UUID.randomUUID().toString()))
                .serviceId(service.getId())
                .keycloakClientId(keycloakClientId)
                .keycloakClientUuid(keycloakClientUuid)
                .status(status)
                .expiresAt(expiresAt)
                .revokedAt(status == ServiceCredential.CredentialStatus.REVOKED 
                        ? updatedAt.plus(faker.number().numberBetween(1, 30), ChronoUnit.DAYS) 
                        : null)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(0)
                .build();
    }

    /**
     * Generates creation timestamp.
     * Distribution: 60% recent (1-30 days ago), 40% older (31-180 days ago).
     */
    private Instant generateCreatedAt() {
        long daysAgo;
        if (faker.random().nextInt(100) < 60) {
            // 60% recent: 1-30 days ago
            daysAgo = faker.number().numberBetween(1, 31);
        } else {
            // 40% older: 31-180 days ago
            daysAgo = faker.number().numberBetween(31, 181);
        }
        return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
    }

    /**
     * Generates credential status.
     * Distribution: ACTIVE (80%), PENDING (15%), REVOKED (5%).
     */
    private ServiceCredential.CredentialStatus generateStatus() {
        int roll = faker.random().nextInt(100);
        if (roll < 80) {
            return ServiceCredential.CredentialStatus.ACTIVE;
        } else if (roll < 95) {
            return ServiceCredential.CredentialStatus.PENDING;
        } else {
            return ServiceCredential.CredentialStatus.REVOKED;
        }
    }

    /**
     * Generates expiration timestamp.
     * Distribution: No expiry (70%), Future expiry 30-365 days (30%).
     */
    private Instant generateExpiresAt(Instant createdAt) {
        // 70% no expiration
        if (faker.random().nextInt(100) < 70) {
            return null;
        }

        // 30% expiration 30-365 days from creation
        long daysToExpire = faker.number().numberBetween(30, 365);
        return createdAt.plus(daysToExpire, ChronoUnit.DAYS);
    }
}

