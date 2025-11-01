package com.example.control.infrastructure.seeding.factory;

import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.example.control.domain.model.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for generating realistic {@link ServiceInstance} mock data.
 * <p>
 * Generates diverse service instances with varied environments, statuses,
 * versions, and configurations suitable for testing filtering and monitoring.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Environments: dev (40%), staging (30%), prod (30%)</li>
 * <li>Status: UP (60%), DOWN (20%), STARTING (10%), UNKNOWN (10%)</li>
 * <li>Versions: Mix of v1.0.0, v1.1.0, v2.0.0, v2.1.0</li>
 * <li>Hosts: Realistic IPs and hostnames</li>
 * <li>Config: Realistic Spring Boot properties with variations</li>
 * <li>Drift: 20% of instances have config drift</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceInstanceFactory {

    /**
     * Available service versions with distribution weights.
     */
    private static final List<String> VERSIONS = List.of(
            "1.0.0", "1.0.0", "1.0.0", // 30%
            "1.1.0", "1.1.0", // 20%
            "2.0.0", "2.0.0", "2.0.0", // 30%
            "2.1.0", "2.1.0" // 20%
    );
    /**
     * Environment distribution configuration.
     */
    private static final List<String> ENVIRONMENTS = List.of(
            "dev", "dev", "dev", "dev", // 40%
            "staging", "staging", "staging", // 30%
            "prod", "prod", "prod" // 30%
    );
    /**
     * Instance status distribution.
     */
    private static final List<ServiceInstance.InstanceStatus> STATUSES = List.of(
            ServiceInstance.InstanceStatus.HEALTHY, ServiceInstance.InstanceStatus.HEALTHY,
            ServiceInstance.InstanceStatus.HEALTHY, ServiceInstance.InstanceStatus.HEALTHY,
            ServiceInstance.InstanceStatus.HEALTHY, ServiceInstance.InstanceStatus.HEALTHY, // 60%
            ServiceInstance.InstanceStatus.UNHEALTHY, ServiceInstance.InstanceStatus.UNHEALTHY, // 20%
            ServiceInstance.InstanceStatus.DRIFT, // 10%
            ServiceInstance.InstanceStatus.UNKNOWN // 10%
    );
    private final Faker faker;

    /**
     * Generates a single {@link ServiceInstance} with realistic attributes.
     *
     * @param serviceName   service name
     * @param serviceId     service ID
     * @param teamId        team ID that owns the service
     * @param instanceIndex instance index within the service
     * @return generated service instance
     */
    public ServiceInstance generate(String serviceName, String serviceId, String teamId, int instanceIndex) {
        String environment = selectEnvironment();
        String instanceId = generateInstanceId(serviceName, environment, instanceIndex);

        String host = generateHost(serviceName, environment, instanceIndex);
        Integer port = generatePort();
        String version = selectVersion();

        ServiceInstance.InstanceStatus status = selectStatus();

        // Generate config hashes
        String expectedHash = faker.internet().uuid();
        String configHash = generateConfigHash(expectedHash, status);
        String lastAppliedHash = configHash;

        // Determine if this instance has drift (20% probability or status is DRIFT)
        boolean hasDrift = (status == ServiceInstance.InstanceStatus.DRIFT) || (faker.random().nextInt(100) < 20);
        Instant driftDetectedAt = hasDrift ? generateDriftDetectedAt() : null;

        Instant createdAt = generateCreatedAt();
        Instant lastSeenAt = generateLastSeenAt(createdAt, status);
        Instant updatedAt = generateUpdatedAt(createdAt, lastSeenAt);

        Map<String, String> metadata = generateMetadata(environment);

        log.debug("Generated instance: {}:{} env={} status={} hasDrift={}",
                serviceName, instanceId, environment, status, hasDrift);

        return ServiceInstance.builder()
                .id(ServiceInstanceId.of(instanceId))
                .serviceId(serviceId)
                .teamId(teamId)
                .host(host)
                .port(port)
                .environment(environment)
                .version(version)
                .configHash(configHash)
                .expectedHash(expectedHash)
                .lastAppliedHash(lastAppliedHash)
                .status(status)
                .lastSeenAt(lastSeenAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .metadata(metadata)
                .hasDrift(hasDrift)
                .driftDetectedAt(driftDetectedAt)
                .build();
    }

    /**
     * Generates instance ID with environment and index.
     *
     * @param serviceName service name
     * @param environment environment
     * @param index       instance index
     * @return instance ID
     */
    private String generateInstanceId(String serviceName, String environment, int index) {
        return String.format("%s-%s-%d", serviceName, environment, index + 1);
    }

    /**
     * Generates realistic hostname.
     *
     * @param serviceName service name
     * @param environment environment
     * @param index       instance index
     * @return hostname
     */
    private String generateHost(String serviceName, String environment, int index) {
        // 60% internal DNS names, 40% IP addresses
        if (faker.random().nextInt(100) < 60) {
            return String.format("%s-%s-%d.internal.company.com", serviceName, environment, index + 1);
        } else {
            return faker.internet().privateIpV4Address();
        }
    }

    /**
     * Generates service port (8000-9999 range).
     *
     * @return port number
     */
    private Integer generatePort() {
        return faker.number().numberBetween(8000, 10000);
    }

    /**
     * Selects environment based on distribution.
     *
     * @return environment name
     */
    private String selectEnvironment() {
        return ENVIRONMENTS.get(faker.random().nextInt(ENVIRONMENTS.size()));
    }

    /**
     * Selects version based on distribution.
     *
     * @return version string
     */
    private String selectVersion() {
        return VERSIONS.get(faker.random().nextInt(VERSIONS.size()));
    }

    /**
     * Selects instance status based on distribution.
     *
     * @return instance status
     */
    private ServiceInstance.InstanceStatus selectStatus() {
        return STATUSES.get(faker.random().nextInt(STATUSES.size()));
    }

    /**
     * Generates config hash based on expected hash and status.
     * For drifted instances, generates a different hash.
     *
     * @param expectedHash expected configuration hash
     * @param status       instance status
     * @return configuration hash
     */
    private String generateConfigHash(String expectedHash, ServiceInstance.InstanceStatus status) {
        // If status is DRIFT or 20% chance, generate different hash
        if (status == ServiceInstance.InstanceStatus.DRIFT || faker.random().nextInt(100) < 20) {
            return faker.internet().uuid();
        }
        return expectedHash;
    }

    /**
     * Generates drift detection timestamp (within last 30 days).
     *
     * @return drift detection instant
     */
    private Instant generateDriftDetectedAt() {
        long daysAgo = faker.number().numberBetween(1, 30);
        return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
    }

    /**
     * Generates instance creation timestamp with mixed distribution.
     * <p>
     * Strategy: 50% recent (1-7 days ago), 50% older (30-180 days ago).
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
            // 50% older: 30-180 days ago
            daysAgo = faker.number().numberBetween(30, 181);
        }

        return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
    }

    /**
     * Generates last seen timestamp based on status.
     * DOWN instances: 1-7 days ago
     * HEALTHY instances: within last 5 minutes
     * STARTING/UNKNOWN: within last hour
     *
     * @param createdAt creation timestamp
     * @param status    instance status
     * @return last seen instant
     */
    private Instant generateLastSeenAt(Instant createdAt, ServiceInstance.InstanceStatus status) {
        switch (status) {
            case UNHEALTHY:
                // Not seen for 1-7 days
                long daysAgo = faker.number().numberBetween(1, 7);
                return Instant.now().minus(daysAgo, ChronoUnit.DAYS);

            case HEALTHY:
            case DRIFT:
                // Seen within last 5 minutes
                long secondsAgo = faker.number().numberBetween(10, 300);
                return Instant.now().minus(secondsAgo, ChronoUnit.SECONDS);

            case UNKNOWN:
            default:
                // Seen within last hour
                long minutesAgo = faker.number().numberBetween(5, 60);
                return Instant.now().minus(minutesAgo, ChronoUnit.MINUTES);
        }
    }

    /**
     * Generates update timestamp between creation and last seen.
     *
     * @param createdAt  creation timestamp
     * @param lastSeenAt last seen timestamp
     * @return update instant
     */
    private Instant generateUpdatedAt(Instant createdAt, Instant lastSeenAt) {
        // Updated time should be close to lastSeenAt
        if (lastSeenAt.isAfter(createdAt)) {
            return lastSeenAt;
        }
        return createdAt;
    }

    /**
     * Generates instance metadata.
     *
     * @param environment environment name
     * @return metadata map
     */
    private Map<String, String> generateMetadata() {
        Map<String, String> metadata = new HashMap<>();

        metadata.put("region", faker.options().option("us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1"));
        metadata.put("zone", faker.options().option("a", "b", "c"));
        metadata.put("instance-type", faker.options().option("t3.medium", "t3.large", "m5.large", "m5.xlarge"));
        metadata.put("k8s-namespace", faker.options().option("default", "production", "staging", "development"));
        metadata.put("k8s-pod", faker.internet().uuid().substring(0, 8));

        return metadata;
    }

    /**
     * Generates instance metadata with environment context.
     *
     * @param environment environment name
     * @return metadata map
     */
    private Map<String, String> generateMetadata(String environment) {
        Map<String, String> metadata = generateMetadata();

        // Add environment-specific metadata
        if ("prod".equals(environment)) {
            metadata.put("monitoring", "enabled");
            metadata.put("alerts", "enabled");
            metadata.put("backup", "enabled");
        } else if ("staging".equals(environment)) {
            metadata.put("monitoring", "enabled");
            metadata.put("alerts", "disabled");
        } else {
            metadata.put("monitoring", "basic");
        }

        return metadata;
    }
}
