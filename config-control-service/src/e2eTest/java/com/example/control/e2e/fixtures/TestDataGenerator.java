package com.example.control.e2e.fixtures;

import com.example.control.e2e.base.TestConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Test data generator for E2E tests.
 * <p>
 * Provides methods to generate unique test data with UUID-based identifiers
 * to avoid conflicts between test runs and ensure test isolation.
 * </p>
 */
@Slf4j
public class TestDataGenerator {

    private static final TestConfig config = TestConfig.getInstance();
    private static final String UUID_PREFIX = config.getTestDataUuidPrefix();

    /**
     * Generate unique service name.
     *
     * @return unique service name
     */
    public static String generateServiceName() {
        String serviceName = String.format("%s-service-%s", UUID_PREFIX, UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated service name: {}", serviceName);
        return serviceName;
    }

    /**
     * Generate unique service name with custom suffix.
     *
     * @param suffix custom suffix
     * @return unique service name
     */
    public static String generateServiceName(String suffix) {
        String serviceName = String.format("%s-service-%s-%s", UUID_PREFIX, suffix, UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated service name with suffix: {}", serviceName);
        return serviceName;
    }

    /**
     * Generate unique instance ID.
     *
     * @return unique instance ID
     */
    public static String generateInstanceId() {
        String instanceId = String.format("%s-instance-%s", UUID_PREFIX, UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated instance ID: {}", instanceId);
        return instanceId;
    }

    /**
     * Generate unique instance ID with custom suffix.
     *
     * @param suffix custom suffix
     * @return unique instance ID
     */
    public static String generateInstanceId(String suffix) {
        String instanceId = String.format("%s-instance-%s-%s", UUID_PREFIX, suffix, UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated instance ID with suffix: {}", instanceId);
        return instanceId;
    }

    /**
     * Generate unique drift event ID.
     *
     * @return unique drift event ID
     */
    public static String generateDriftEventId() {
        String driftId = String.format("%s-drift-%s", UUID_PREFIX, UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated drift event ID: {}", driftId);
        return driftId;
    }

    /**
     * Generate unique approval request ID.
     *
     * @return unique approval request ID
     */
    public static String generateApprovalRequestId() {
        String requestId = String.format("%s-approval-%s", UUID_PREFIX, UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated approval request ID: {}", requestId);
        return requestId;
    }

    /**
     * Generate unique service share ID.
     *
     * @return unique service share ID
     */
    public static String generateServiceShareId() {
        String shareId = String.format("%s-share-%s", UUID_PREFIX, UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated service share ID: {}", shareId);
        return shareId;
    }

    /**
     * Generate unique UUID.
     *
     * @return unique UUID string
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate unique UUID with custom prefix.
     *
     * @param prefix custom prefix
     * @return unique UUID string with prefix
     */
    public static String generateUuid(String prefix) {
        return String.format("%s-%s", prefix, UUID.randomUUID().toString());
    }

    /**
     * Generate current timestamp.
     *
     * @return current timestamp as string
     */
    public static String generateTimestamp() {
        return Instant.now().toString();
    }

    /**
     * Generate test description.
     *
     * @param testName the test name
     * @return test description with timestamp
     */
    public static String generateTestDescription(String testName) {
        return String.format("E2E Test: %s - Generated at: %s", testName, Instant.now());
    }

    /**
     * Generate test tag.
     *
     * @param tagName the tag name
     * @return test tag with UUID
     */
    public static String generateTestTag(String tagName) {
        return String.format("%s-%s", tagName, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Generate test environment.
     *
     * @return test environment name
     */
    public static String generateTestEnvironment() {
        return String.format("%s-env-%s", UUID_PREFIX, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Generate test host.
     *
     * @return test host name
     */
    public static String generateTestHost() {
        return String.format("%s-host-%s", UUID_PREFIX, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Generate test port.
     *
     * @return test port number
     */
    public static int generateTestPort() {
        // Generate port in range 8000-8999
        return 8000 + (int) (Math.random() * 1000);
    }

    /**
     * Generate test version.
     *
     * @return test version string
     */
    public static String generateTestVersion() {
        return String.format("1.0.%d", (int) (Math.random() * 100));
    }

    /**
     * Generate test hash.
     *
     * @return test hash string
     */
    public static String generateTestHash() {
        return String.format("hash-%s", UUID.randomUUID().toString().substring(0, 16));
    }

    /**
     * Generate test URL.
     *
     * @param path the URL path
     * @return test URL
     */
    public static String generateTestUrl(String path) {
        return String.format("http://%s:%d%s", generateTestHost(), generateTestPort(), path);
    }

    /**
     * Generate test repository URL.
     *
     * @return test repository URL
     */
    public static String generateTestRepoUrl() {
        return String.format("https://github.com/test/%s.git", generateServiceName());
    }

    /**
     * Generate test email.
     *
     * @param username the username
     * @return test email
     */
    public static String generateTestEmail(String username) {
        return String.format("%s@%s-test.com", username, UUID_PREFIX);
    }

    /**
     * Generate test list of strings.
     *
     * @param count number of strings to generate
     * @param prefix prefix for each string
     * @return list of generated strings
     */
    public static List<String> generateStringList(int count, String prefix) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> String.format("%s-%d-%s", prefix, i, UUID.randomUUID().toString().substring(0, 4)))
                .toList();
    }

    /**
     * Generate test attributes map.
     *
     * @return map of test attributes
     */
    public static java.util.Map<String, Object> generateTestAttributes() {
        return java.util.Map.of(
                "environment", generateTestEnvironment(),
                "version", generateTestVersion(),
                "created_at", generateTimestamp(),
                "test_id", generateUuid(),
                "tags", generateStringList(3, "tag")
        );
    }

    /**
     * Generate test configuration map.
     *
     * @return map of test configuration
     */
    public static java.util.Map<String, Object> generateTestConfig() {
        return java.util.Map.of(
                "host", generateTestHost(),
                "port", generateTestPort(),
                "timeout", 30000,
                "retries", 3,
                "enabled", true,
                "metadata", generateTestAttributes()
        );
    }

    /**
     * Generate orphaned service name (for ownership request tests).
     *
     * @return orphaned service name
     */
    public static String generateOrphanedServiceName() {
        String serviceName = String.format("orphaned-service-%s", UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated orphaned service name: {}", serviceName);
        return serviceName;
    }

    /**
     * Generate service name for team ownership.
     *
     * @param teamId the team ID
     * @return service name for team
     */
    public static String generateServiceNameForTeam(String teamId) {
        String serviceName = String.format("%s-service-%s-%s", UUID_PREFIX, teamId, UUID.randomUUID().toString().substring(0, 8));
        log.debug("Generated service name for team {}: {}", teamId, serviceName);
        return serviceName;
    }
}
