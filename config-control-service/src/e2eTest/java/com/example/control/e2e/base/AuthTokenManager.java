package com.example.control.e2e.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized authentication token management for E2E tests.
 * <p>
 * Provides token acquisition, caching, and automatic refresh functionality
 * for all test users. Tokens are cached per user and automatically refreshed
 * when they approach expiry.
 * </p>
 */
@Slf4j
public class AuthTokenManager {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static AuthTokenManager instance;
    private final TestConfig config;
    private final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    private AuthTokenManager() {
        this.config = TestConfig.getInstance();
        log.info("AuthTokenManager initialized");
    }

    /**
     * Get singleton instance of AuthTokenManager.
     *
     * @return AuthTokenManager instance
     */
    public static synchronized AuthTokenManager getInstance() {
        if (instance == null) {
            instance = new AuthTokenManager();
        }
        return instance;
    }

    /**
     * Get valid access token for the specified user.
     * <p>
     * Returns cached token if valid, otherwise acquires new token from Keycloak.
     * Automatically handles token refresh when approaching expiry.
     * </p>
     *
     * @param username the username to get token for
     * @return valid access token
     * @throws RuntimeException if token acquisition fails
     */
    public String getToken(String username) {
        TokenInfo cachedToken = tokenCache.get(username);
        
        if (cachedToken != null && !isTokenExpiringSoon(cachedToken)) {
            log.debug("Using cached token for user: {}", username);
            return cachedToken.getAccessToken();
        }

        log.info("Acquiring new token for user: {}", username);
        TokenInfo newToken = acquireToken(username);
        tokenCache.put(username, newToken);
        
        Allure.addAttachment("Token Acquisition", "text/plain", 
                String.format("User: %s, Token acquired at: %s", username, Instant.now()));
        
        return newToken.getAccessToken();
    }

    /**
     * Get token for admin user.
     *
     * @return admin access token
     */
    public String getAdminToken() {
        return getToken(config.getAdminUsername());
    }

    /**
     * Get token for user1.
     *
     * @return user1 access token
     */
    public String getUser1Token() {
        return getToken(config.getUser1Username());
    }

    /**
     * Get token for user2.
     *
     * @return user2 access token
     */
    public String getUser2Token() {
        return getToken(config.getUser2Username());
    }

    /**
     * Get token for user3.
     *
     * @return user3 access token
     */
    public String getUser3Token() {
        return getToken(config.getUser3Username());
    }

    /**
     * Get token for user4.
     *
     * @return user4 access token
     */
    public String getUser4Token() {
        return getToken(config.getUser4Username());
    }

    /**
     * Get token for user5.
     *
     * @return user5 access token
     */
    public String getUser5Token() {
        return getToken(config.getUser5Username());
    }

    /**
     * Clear all cached tokens.
     * <p>
     * Useful for test cleanup or when forcing fresh token acquisition.
     * </p>
     */
    public void clearCache() {
        log.info("Clearing token cache");
        tokenCache.clear();
    }

    /**
     * Clear cached token for specific user.
     *
     * @param username the username to clear token for
     */
    public void clearToken(String username) {
        log.info("Clearing token for user: {}", username);
        tokenCache.remove(username);
    }

    /**
     * Acquire new token from Keycloak for the specified user.
     *
     * @param username the username
     * @return TokenInfo containing token and expiry information
     */
    private TokenInfo acquireToken(String username) {
        String password = getPasswordForUser(username);
        if (password == null) {
            throw new IllegalArgumentException("Unknown user: " + username);
        }

        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                config.getKeycloakBaseUrl(), config.getKeycloakRealm());

        String requestBody = String.format(
                "grant_type=password&client_id=%s&client_secret=%s&username=%s&password=%s",
                config.getKeycloakClientId(),
                config.getKeycloakClientSecret(),
                username,
                password
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(config.getApiRequestTimeoutSeconds()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException(String.format(
                        "Failed to acquire token for user %s: HTTP %d - %s",
                        username, response.statusCode(), response.body()));
            }

            JsonNode tokenResponse = objectMapper.readTree(response.body());
            String accessToken = tokenResponse.get("access_token").asText();
            int expiresIn = tokenResponse.get("expires_in").asInt();
            
            Instant expiresAt = Instant.now().plusSeconds(expiresIn);
            
            log.debug("Token acquired for user: {}, expires at: {}", username, expiresAt);
            return new TokenInfo(accessToken, expiresAt);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to acquire token for user: " + username, e);
        }
    }

    /**
     * Check if token is expiring soon and needs refresh.
     *
     * @param tokenInfo the token information
     * @return true if token expires within refresh threshold
     */
    private boolean isTokenExpiringSoon(TokenInfo tokenInfo) {
        Instant now = Instant.now();
        Instant refreshThreshold = now.plusSeconds(config.getTokenRefreshThresholdSeconds());
        return tokenInfo.getExpiresAt().isBefore(refreshThreshold);
    }

    /**
     * Get password for the specified user.
     *
     * @param username the username
     * @return password or null if user not found
     */
    private String getPasswordForUser(String username) {
        return switch (username) {
            case "admin" -> config.getAdminPassword();
            case "user1" -> config.getUser1Password();
            case "user2" -> config.getUser2Password();
            case "user3" -> config.getUser3Password();
            case "user4" -> config.getUser4Password();
            case "user5" -> config.getUser5Password();
            default -> null;
        };
    }

    /**
     * Token information container.
     */
    private static class TokenInfo {
        private final String accessToken;
        private final Instant expiresAt;

        public TokenInfo(String accessToken, Instant expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
