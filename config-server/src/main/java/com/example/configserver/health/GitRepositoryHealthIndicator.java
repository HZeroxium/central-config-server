package com.example.configserver.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Git repository connectivity.
 * <p>
 * Checks if the Git repository configured for Config Server is accessible.
 * Since Spring Cloud Config Server doesn't expose GitRepository directly,
 * we check the EnvironmentRepository and attempt to validate connectivity.
 * </p>
 *
 * @author Config Server Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitRepositoryHealthIndicator implements HealthIndicator {

    private final EnvironmentRepository environmentRepository;

    @Override
    public Health health() {
        try {
            // Attempt to fetch a test configuration to verify Git repository accessibility
            // Using a non-existent application to minimize impact
            String testApplication = "__health_check__";
            String testProfile = "default";
            String testLabel = null;

            try {
                environmentRepository.findOne(testApplication, testProfile, testLabel);
                // If we can call the repository without exception, it's accessible
                return Health.up()
                        .withDetail("repository", "git")
                        .withDetail("status", "accessible")
                        .withDetail("testApplication", testApplication)
                        .build();
            } catch (Exception e) {
                // If we get a specific exception (e.g., repository not found), repository might be accessible
                // but the test application doesn't exist, which is expected
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("No such label")) {
                    // Repository is accessible, but label doesn't exist
                    return Health.up()
                            .withDetail("repository", "git")
                            .withDetail("status", "accessible")
                            .withDetail("note", "Repository accessible, test label not found (expected)")
                            .build();
                } else {
                    // Other exceptions might indicate connectivity issues
                    log.debug("Git repository health check encountered exception: {}", e.getMessage());
                    return Health.down()
                            .withDetail("repository", "git")
                            .withDetail("status", "unreachable")
                            .withDetail("error", e.getMessage())
                            .build();
                }
            }

        } catch (Exception e) {
            log.warn("Error checking Git repository health", e);
            return Health.down()
                    .withDetail("repository", "git")
                    .withDetail("status", "error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

