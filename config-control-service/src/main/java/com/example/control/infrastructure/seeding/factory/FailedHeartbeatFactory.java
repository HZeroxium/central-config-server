package com.example.control.infrastructure.seeding.factory;

import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Factory for generating realistic {@link FailedHeartbeat} mock data.
 * <p>
 * Generates failed heartbeats that simulate real-world scenarios where
 * heartbeat processing fails and messages are routed to DLQ.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailedHeartbeatFactory {

    private final Faker faker;

    /**
     * Generates a failed heartbeat for a service instance.
     *
     * @param serviceName the service name
     * @param instanceId  the instance ID
     * @param serviceId   the service ID (from ApplicationService)
     * @param teamId      the team ID (from ApplicationService.ownerTeamId)
     * @param environment the environment
     * @return generated failed heartbeat
     */
    public FailedHeartbeat generate(String serviceName, String instanceId, String serviceId,
                                     String teamId, String environment) {
        Instant firstSeenAt = generateFirstSeenAt();
        Instant lastSeenAt = firstSeenAt.plus(faker.number().numberBetween(0, 3600), 
                ChronoUnit.SECONDS);

        // Generate mock heartbeat payload
        HeartbeatPayload payload = HeartbeatPayload.builder()
                .serviceName(serviceName)
                .instanceId(instanceId)
                .configHash(faker.internet().uuid())
                .host(faker.internet().ipV4Address())
                .port(faker.number().numberBetween(8080, 9000))
                .environment(environment)
                .version(faker.app().version())
                .metadata(Map.of(
                        "os", faker.options().option("Linux", "Windows", "macOS", "Unix"),
                        "region", faker.address().countryCode()
                ))
                .build();

        // Generate exception details
        String exceptionMessage = generateExceptionMessage();
        String exceptionClass = generateExceptionClass();

        // Generate status (mostly NEW, some INVESTIGATING)
        FailedHeartbeat.FailedHeartbeatStatus status = generateStatus();

        // Generate retry count (1-5 retries before DLQ)
        int retryCount = faker.number().numberBetween(1, 6);

        log.debug("Generated failed heartbeat: service={}, instance={}, status={}, retries={}",
                serviceName, instanceId, status, retryCount);

        return FailedHeartbeat.builder()
                .id(FailedHeartbeatId.of(UUID.randomUUID().toString()))
                .serviceName(serviceName)
                .instanceId(instanceId)
                .serviceId(serviceId)
                .teamId(teamId)
                .environment(environment)
                .payload(payload)
                .originalTopic("heartbeat-queue")
                .originalPartition(faker.number().numberBetween(0, 10))
                .originalOffset(faker.number().randomNumber(10, false))
                .exceptionMessage(exceptionMessage)
                .exceptionClass(exceptionClass)
                .retryCount(retryCount)
                .status(status)
                .firstSeenAt(firstSeenAt)
                .lastSeenAt(lastSeenAt)
                .resolvedAt(status == FailedHeartbeat.FailedHeartbeatStatus.RESOLVED 
                        ? lastSeenAt.plus(faker.number().numberBetween(1, 7), ChronoUnit.DAYS) 
                        : null)
                .resolvedBy(status == FailedHeartbeat.FailedHeartbeatStatus.RESOLVED 
                        ? faker.name().firstName().toLowerCase() + "." + faker.name().lastName().toLowerCase()
                        : null)
                .notes(status == FailedHeartbeat.FailedHeartbeatStatus.RESOLVED 
                        ? "Resolved by team after investigation" 
                        : null)
                .build();
    }

    /**
     * Generates first seen timestamp.
     * Distribution: 70% recent (1-7 days ago), 30% older (8-30 days ago).
     */
    private Instant generateFirstSeenAt() {
        long daysAgo;
        if (faker.random().nextInt(100) < 70) {
            // 70% recent: 1-7 days ago
            daysAgo = faker.number().numberBetween(1, 8);
        } else {
            // 30% older: 8-30 days ago
            daysAgo = faker.number().numberBetween(8, 31);
        }
        return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
    }

    /**
     * Generates exception message.
     */
    private String generateExceptionMessage() {
        List<String> messages = List.of(
                "Service instance not found",
                "Config hash mismatch",
                "Database connection timeout",
                "Invalid service configuration",
                "Network error during processing",
                "Deserialization error",
                "Validation failed for heartbeat payload"
        );
        return messages.get(faker.random().nextInt(messages.size()));
    }

    /**
     * Generates exception class name.
     */
    private String generateExceptionClass() {
        List<String> classes = List.of(
                "com.example.control.domain.exception.ServiceInstanceNotFoundException",
                "com.example.control.domain.exception.ConfigDriftException",
                "java.sql.SQLTimeoutException",
                "com.fasterxml.jackson.databind.JsonMappingException",
                "java.net.SocketTimeoutException",
                "jakarta.validation.ConstraintViolationException"
        );
        return classes.get(faker.random().nextInt(classes.size()));
    }

    /**
     * Generates status.
     * Distribution: NEW (70%), INVESTIGATING (20%), RESOLVED (8%), IGNORED (2%).
     */
    private FailedHeartbeat.FailedHeartbeatStatus generateStatus() {
        int roll = faker.random().nextInt(100);
        if (roll < 70) {
            return FailedHeartbeat.FailedHeartbeatStatus.NEW;
        } else if (roll < 90) {
            return FailedHeartbeat.FailedHeartbeatStatus.INVESTIGATING;
        } else if (roll < 98) {
            return FailedHeartbeat.FailedHeartbeatStatus.RESOLVED;
        } else {
            return FailedHeartbeat.FailedHeartbeatStatus.IGNORED;
        }
    }
}

