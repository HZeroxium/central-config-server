package com.example.control.e2e.client;

import com.example.control.e2e.base.TestConfig;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Health check utilities for E2E tests.
 * <p>
 * Provides methods to wait for services to be ready before running tests.
 * Includes health checks for Keycloak, config-control-service, and other
 * dependencies.
 * </p>
 */
@Slf4j
public class HealthCheckClient {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final TestConfig config;

    public HealthCheckClient() {
        this.config = TestConfig.getInstance();
    }

    /**
     * Wait for Keycloak to be ready.
     * <p>
     * Checks Keycloak realm endpoint availability.
     * </p>
     *
     * @throws RuntimeException if Keycloak doesn't become ready within timeout
     */
    public void waitForKeycloak() {
        String keycloakUrl = String.format("%s/realms/%s",
                config.getKeycloakBaseUrl(), config.getKeycloakRealm());

        log.info("Waiting for Keycloak at: {}", keycloakUrl);
        waitForService("Keycloak", keycloakUrl, 200);
        log.info("Keycloak is ready");

        Allure.addAttachment("Health Check", "text/plain",
                String.format("Keycloak ready at: %s", keycloakUrl));
    }

    /**
     * Wait for config-control-service to be ready.
     * <p>
     * Checks service actuator health endpoint.
     * </p>
     *
     * @throws RuntimeException if service doesn't become ready within timeout
     */
    public void waitForConfigControlService() {
        String serviceUrl = config.getApiBaseUrl().replace("/api", "") + "/actuator/health";

        log.info("Waiting for config-control-service at: {}", serviceUrl);
        waitForService("Config Control Service", serviceUrl, 200);
        log.info("Config Control Service is ready");

        Allure.addAttachment("Health Check", "text/plain",
                String.format("Config Control Service ready at: %s", serviceUrl));
    }

    /**
     * Wait for all required services to be ready.
     * <p>
     * Performs health checks for Keycloak and config-control-service.
     * This is the main method to call in test setup.
     * </p>
     */
    public void waitForAllServices() {
        log.info("Starting health checks for all services...");

        try {
            waitForKeycloak();
            waitForConfigControlService();
            log.info("All services are ready");

            Allure.addAttachment("Health Check Summary", "text/plain",
                    "All required services are ready and healthy");

        } catch (Exception e) {
            log.error("Health check failed", e);
            Allure.addAttachment("Health Check Failure", "text/plain",
                    String.format("Health check failed: %s", e.getMessage()));
            throw e;
        }
    }

    /**
     * Wait for a service to be ready by checking HTTP endpoint.
     *
     * @param serviceName        name of the service for logging
     * @param url                URL to check
     * @param expectedStatusCode expected HTTP status code
     * @throws RuntimeException if service doesn't become ready within timeout
     */
    private void waitForService(String serviceName, String url, int expectedStatusCode) {
        int maxAttempts = config.getHealthCheckTimeoutSeconds() / config.getHealthCheckIntervalSeconds();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == expectedStatusCode) {
                    log.debug("{} is ready (attempt {}/{})", serviceName, attempt, maxAttempts);
                    return;
                }

                log.debug("{} not ready yet: HTTP {} (attempt {}/{})",
                        serviceName, response.statusCode(), attempt, maxAttempts);

            } catch (Exception e) {
                log.debug("{} not ready yet: {} (attempt {}/{})",
                        serviceName, e.getMessage(), attempt, maxAttempts);
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(config.getHealthCheckIntervalSeconds() * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Health check interrupted", e);
                }
            }
        }

        throw new RuntimeException(String.format(
                "%s did not become ready within %d seconds",
                serviceName, config.getHealthCheckTimeoutSeconds()));
    }

    /**
     * Check if a service is currently healthy without waiting.
     *
     * @param url                URL to check
     * @param expectedStatusCode expected HTTP status code
     * @return true if service is healthy, false otherwise
     */
    public boolean isServiceHealthy(String url, int expectedStatusCode) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == expectedStatusCode;

        } catch (Exception e) {
            log.debug("Service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if Keycloak is currently healthy.
     *
     * @return true if Keycloak is healthy, false otherwise
     */
    public boolean isKeycloakHealthy() {
        String keycloakUrl = String.format("%s/realms/%s",
                config.getKeycloakBaseUrl(), config.getKeycloakRealm());
        return isServiceHealthy(keycloakUrl, 200);
    }

    /**
     * Check if config-control-service is currently healthy.
     *
     * @return true if service is healthy, false otherwise
     */
    public boolean isConfigControlServiceHealthy() {
        String serviceUrl = config.getApiBaseUrl().replace("/api", "") + "/actuator/health";
        return isServiceHealthy(serviceUrl, 200);
    }
}
