package com.example.control.e2e.debug;

import com.example.control.e2e.base.TestConfig;
import com.example.control.e2e.base.AuthTokenManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Debug Test")
public class DebugTest {

    @Test
    @DisplayName("Test configuration loading")
    void testConfigLoading() {
        TestConfig config = TestConfig.getInstance();
        System.out.println("API Base URL: " + config.getApiBaseUrl());
        System.out.println("Keycloak Base URL: " + config.getKeycloakBaseUrl());
        System.out.println("Admin Username: " + config.getAdminUsername());
        System.out.println("Admin Password: " + config.getAdminPassword());
    }

    @Test
    @DisplayName("Test token acquisition")
    void testTokenAcquisition() {
        try {
            AuthTokenManager tokenManager = AuthTokenManager.getInstance();
            String adminToken = tokenManager.getAdminToken();
            System.out.println("Admin token acquired: " + (adminToken != null ? "SUCCESS" : "FAILED"));
            System.out.println("Token length: " + (adminToken != null ? adminToken.length() : 0));
        } catch (Exception e) {
            System.out.println("Token acquisition failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
