package com.example.control.e2e.base;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration management for E2E tests.
 * <p>
 * Loads configuration from test-config.properties and provides
 * typed access to configuration values with sensible defaults.
 * </p>
 */
@Slf4j
public class TestConfig {

    private static final String CONFIG_FILE = "test-config.properties";
    private static TestConfig instance;
    private final Properties properties;

    private TestConfig() {
        this.properties = loadProperties();
        log.info("E2E Test configuration loaded successfully");
    }

    /**
     * Get singleton instance of TestConfig.
     *
     * @return TestConfig instance
     */
    public static synchronized TestConfig getInstance() {
        if (instance == null) {
            instance = new TestConfig();
        }
        return instance;
    }

    /**
     * Load properties from test-config.properties file.
     *
     * @return Properties object with loaded configuration
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                log.warn("Configuration file {} not found, using defaults", CONFIG_FILE);
                return getDefaultProperties();
            }
            props.load(input);
            log.debug("Loaded {} properties from {}", props.size(), CONFIG_FILE);
        } catch (IOException e) {
            log.error("Failed to load configuration file: {}", CONFIG_FILE, e);
            return getDefaultProperties();
        }
        return props;
    }

    /**
     * Get default properties when config file is not available.
     *
     * @return Properties with default values
     */
    private Properties getDefaultProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("api.base.url", "http://localhost:8081/api");
        defaults.setProperty("keycloak.base.url", "http://localhost:8080");
        defaults.setProperty("keycloak.realm", "config-control");
        defaults.setProperty("keycloak.client.id", "config-control-service");
        defaults.setProperty("keycloak.client.secret", "config-control-service-secret");
        defaults.setProperty("test.admin.username", "admin");
        defaults.setProperty("test.admin.password", "admin123");
        defaults.setProperty("test.user1.username", "user1");
        defaults.setProperty("test.user1.password", "user123");
        defaults.setProperty("test.user2.username", "user2");
        defaults.setProperty("test.user2.password", "user123");
        defaults.setProperty("test.user3.username", "user3");
        defaults.setProperty("test.user3.password", "user123");
        defaults.setProperty("test.user4.username", "user4");
        defaults.setProperty("test.user4.password", "user123");
        defaults.setProperty("test.user5.username", "user5");
        defaults.setProperty("test.user5.password", "user123");
        defaults.setProperty("health.check.timeout.seconds", "120");
        defaults.setProperty("health.check.interval.seconds", "2");
        defaults.setProperty("api.request.timeout.seconds", "30");
        defaults.setProperty("token.refresh.threshold.seconds", "60");
        defaults.setProperty("test.data.uuid.prefix", "e2e-test");
        defaults.setProperty("test.data.cleanup.enabled", "true");
        return defaults;
    }

    // Service URLs
    public String getApiBaseUrl() {
        return getProperty("api.base.url", "http://localhost:8081/api");
    }

    public String getKeycloakBaseUrl() {
        return getProperty("keycloak.base.url", "http://localhost:8080");
    }

    public String getKeycloakRealm() {
        return getProperty("keycloak.realm", "config-control");
    }

    public String getKeycloakClientId() {
        return getProperty("keycloak.client.id", "config-control-service");
    }

    public String getKeycloakClientSecret() {
        return getProperty("keycloak.client.secret", "config-control-service-secret");
    }

    // Test Users
    public String getAdminUsername() {
        return getProperty("test.admin.username", "admin");
    }

    public String getAdminPassword() {
        return getProperty("test.admin.password", "admin123");
    }

    public String getUser1Username() {
        return getProperty("test.user1.username", "user1");
    }

    public String getUser1Password() {
        return getProperty("test.user1.password", "user123");
    }

    public String getUser2Username() {
        return getProperty("test.user2.username", "user2");
    }

    public String getUser2Password() {
        return getProperty("test.user2.password", "user123");
    }

    public String getUser3Username() {
        return getProperty("test.user3.username", "user3");
    }

    public String getUser3Password() {
        return getProperty("test.user3.password", "user123");
    }

    public String getUser4Username() {
        return getProperty("test.user4.username", "user4");
    }

    public String getUser4Password() {
        return getProperty("test.user4.password", "user123");
    }

    public String getUser5Username() {
        return getProperty("test.user5.username", "user5");
    }

    public String getUser5Password() {
        return getProperty("test.user5.password", "user123");
    }

    // Timeouts
    public int getHealthCheckTimeoutSeconds() {
        return getIntProperty("health.check.timeout.seconds", 120);
    }

    public int getHealthCheckIntervalSeconds() {
        return getIntProperty("health.check.interval.seconds", 2);
    }

    public int getApiRequestTimeoutSeconds() {
        return getIntProperty("api.request.timeout.seconds", 30);
    }

    public int getTokenRefreshThresholdSeconds() {
        return getIntProperty("token.refresh.threshold.seconds", 60);
    }

    // Test Data
    public String getTestDataUuidPrefix() {
        return getProperty("test.data.uuid.prefix", "e2e-test");
    }

    public boolean isTestDataCleanupEnabled() {
        return getBooleanProperty("test.data.cleanup.enabled", true);
    }

    // Helper methods
    private String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for property {}: {}, using default: {}", key, properties.getProperty(key), defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Get all properties for debugging purposes.
     *
     * @return Properties object (read-only view)
     */
    public Properties getAllProperties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }
}
