package com.example.control.e2e.base;

import com.example.control.e2e.client.HealthCheckClient;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;

/**
 * Abstract base class for all E2E tests.
 * <p>
 * Provides common setup and teardown functionality including:
 * - Health checks for all required services
 * - Token management initialization
 * - Common test utilities and assertions
 * - Allure reporting configuration
 * </p>
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseE2ETest {

    protected static final TestConfig config = TestConfig.getInstance();
    protected static final AuthTokenManager tokenManager = AuthTokenManager.getInstance();
    protected static final HealthCheckClient healthCheckClient = new HealthCheckClient();

    protected String adminToken;
    protected String user1Token;
    protected String user2Token;
    protected String user3Token;
    protected String user4Token;
    protected String user5Token;

    /**
     * Setup method executed before all tests in the class.
     * <p>
     * Performs health checks and acquires authentication tokens.
     * This method is called once per test class.
     * </p>
     */
    @BeforeAll
    @Step("Setup E2E Test Environment")
    @Description("Initialize test environment with health checks and authentication tokens")
    void setUpTestEnvironment() {
        log.info("Setting up E2E test environment for: {}", getClass().getSimpleName());
        
        try {
            // Wait for all services to be ready
            waitForServices();
            
            // Acquire authentication tokens
            acquireTokens();
            
            log.info("E2E test environment setup completed successfully");
            
            Allure.addAttachment("Test Environment", "text/plain", 
                    String.format("Test Class: %s\nSetup Time: %s\nAPI Base URL: %s", 
                            getClass().getSimpleName(), 
                            Instant.now(), 
                            config.getApiBaseUrl()));
                            
        } catch (Exception e) {
            log.error("Failed to setup E2E test environment", e);
            Allure.addAttachment("Setup Failure", "text/plain", 
                    String.format("Setup failed: %s", e.getMessage()));
            throw e;
        }
    }

    /**
     * Cleanup method executed after all tests in the class.
     * <p>
     * Performs cleanup operations and clears cached tokens.
     * This method is called once per test class.
     * </p>
     */
    @AfterAll
    @Step("Cleanup E2E Test Environment")
    @Description("Clean up test environment and clear cached resources")
    void tearDownTestEnvironment() {
        log.info("Cleaning up E2E test environment for: {}", getClass().getSimpleName());
        
        try {
            // Clear token cache
            if (config.isTestDataCleanupEnabled()) {
                tokenManager.clearCache();
                log.debug("Token cache cleared");
            }
            
            log.info("E2E test environment cleanup completed");
            
            Allure.addAttachment("Test Environment Cleanup", "text/plain", 
                    String.format("Test Class: %s\nCleanup Time: %s", 
                            getClass().getSimpleName(), Instant.now()));
                            
        } catch (Exception e) {
            log.warn("Error during test environment cleanup", e);
            Allure.addAttachment("Cleanup Warning", "text/plain", 
                    String.format("Cleanup warning: %s", e.getMessage()));
        }
    }

    /**
     * Wait for all required services to be ready.
     * <p>
     * Performs health checks for Keycloak and config-control-service.
     * </p>
     */
    @Step("Wait for Services")
    @Description("Wait for all required services to be ready")
    private void waitForServices() {
        log.info("Waiting for all services to be ready...");
        healthCheckClient.waitForAllServices();
    }

    /**
     * Acquire authentication tokens for all test users.
     * <p>
     * Gets tokens for admin and all test users (user1-user5).
     * Tokens are cached and automatically refreshed when needed.
     * </p>
     */
    @Step("Acquire Authentication Tokens")
    @Description("Acquire authentication tokens for all test users")
    private void acquireTokens() {
        log.info("Acquiring authentication tokens...");
        
        try {
            adminToken = tokenManager.getAdminToken();
            user1Token = tokenManager.getUser1Token();
            user2Token = tokenManager.getUser2Token();
            user3Token = tokenManager.getUser3Token();
            user4Token = tokenManager.getUser4Token();
            user5Token = tokenManager.getUser5Token();
            
            log.info("Authentication tokens acquired successfully");
            
            Allure.addAttachment("Token Acquisition", "text/plain", 
                    String.format("Tokens acquired for: admin, user1, user2, user3, user4, user5\nTime: %s", 
                            Instant.now()));
                            
        } catch (Exception e) {
            log.error("Failed to acquire authentication tokens", e);
            throw new RuntimeException("Token acquisition failed", e);
        }
    }

    /**
     * Get token for admin user.
     *
     * @return admin token
     */
    protected String getAdminToken() {
        return adminToken;
    }

    /**
     * Get token for user1.
     *
     * @return user1 token
     */
    protected String getUser1Token() {
        return user1Token;
    }

    /**
     * Get token for user2.
     *
     * @return user2 token
     */
    protected String getUser2Token() {
        return user2Token;
    }

    /**
     * Get token for user3.
     *
     * @return user3 token
     */
    protected String getUser3Token() {
        return user3Token;
    }

    /**
     * Get token for user4.
     *
     * @return user4 token
     */
    protected String getUser4Token() {
        return user4Token;
    }

    /**
     * Get token for user5.
     *
     * @return user5 token
     */
    protected String getUser5Token() {
        return user5Token;
    }

    /**
     * Get test configuration.
     *
     * @return TestConfig instance
     */
    protected TestConfig getConfig() {
        return config;
    }

    /**
     * Get authentication token manager.
     *
     * @return AuthTokenManager instance
     */
    protected AuthTokenManager getTokenManager() {
        return tokenManager;
    }

    /**
     * Get health check client.
     *
     * @return HealthCheckClient instance
     */
    protected HealthCheckClient getHealthCheckClient() {
        return healthCheckClient;
    }

    /**
     * Log test step for Allure reporting.
     *
     * @param stepName the step name
     * @param stepDescription the step description
     */
    @Step("{stepName}")
    protected void logTestStep(String stepName, String stepDescription) {
        log.info("Test Step: {} - {}", stepName, stepDescription);
        Allure.addAttachment("Test Step", "text/plain", 
                String.format("Step: %s\nDescription: %s\nTime: %s", 
                        stepName, stepDescription, Instant.now()));
    }

    /**
     * Log test data for Allure reporting.
     *
     * @param dataName the data name
     * @param data the data content
     */
    protected void logTestData(String dataName, String data) {
        log.debug("Test Data: {} - {}", dataName, data);
        Allure.addAttachment(dataName, "text/plain", data);
    }

    /**
     * Log test result for Allure reporting.
     *
     * @param resultName the result name
     * @param result the result content
     */
    protected void logTestResult(String resultName, String result) {
        log.info("Test Result: {} - {}", resultName, result);
        Allure.addAttachment(resultName, "text/plain", result);
    }
}
