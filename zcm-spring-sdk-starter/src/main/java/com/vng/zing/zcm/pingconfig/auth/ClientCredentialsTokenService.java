package com.vng.zing.zcm.pingconfig.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vng.zing.zcm.config.SdkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for fetching and caching client credentials tokens from Keycloak for ping authentication.
 * <p>
 * This service implements token caching with automatic refresh before expiration.
 * It uses a thread-safe approach with double-checked locking to prevent
 * concurrent token requests.
 * </p>
 */
@Slf4j
public class ClientCredentialsTokenService {

    private static final int TOKEN_REFRESH_BUFFER_SECONDS = 30;

    private final RestClient restClient;
    private final SdkProperties.Ping.ClientCredentials clientCredentialsConfig;

    private String cachedAccessToken;
    private Instant tokenExpiresAt;
    private final ReentrantLock tokenLock = new ReentrantLock();

    /**
     * Creates a new ClientCredentialsTokenService for ping authentication.
     *
     * @param restClient RestClient for HTTP requests
     * @param clientCredentialsConfig Client credentials configuration
     */
    public ClientCredentialsTokenService(
            RestClient restClient,
            SdkProperties.Ping.ClientCredentials clientCredentialsConfig) {
        this.restClient = restClient;
        this.clientCredentialsConfig = clientCredentialsConfig;
    }

    /**
     * Gets an access token using client credentials flow.
     * <p>
     * Caches token and refreshes automatically before expiration.
     * Thread-safe token retrieval.
     *
     * @return access token string
     * @throws RuntimeException if token fetch fails
     */
    public String getAccessToken() {
        // Check if token is still valid
        if (cachedAccessToken != null && tokenExpiresAt != null
                && Instant.now().isBefore(tokenExpiresAt.minusSeconds(TOKEN_REFRESH_BUFFER_SECONDS))) {
            return cachedAccessToken;
        }

        // Acquire lock to prevent concurrent token requests
        tokenLock.lock();
        try {
            // Double-check after acquiring lock
            if (cachedAccessToken != null && tokenExpiresAt != null
                    && Instant.now().isBefore(tokenExpiresAt.minusSeconds(TOKEN_REFRESH_BUFFER_SECONDS))) {
                return cachedAccessToken;
            }

            // Fetch new token
            log.debug("Fetching new Keycloak token for ping authentication");
            String tokenUrl = buildTokenEndpoint();

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");
            formData.add("client_id", clientCredentialsConfig.getClientId());
            formData.add("client_secret", clientCredentialsConfig.getClientSecret());

            try {
                KeycloakTokenResponse response = restClient.post()
                        .uri(tokenUrl)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(formData)
                        .retrieve()
                        .body(KeycloakTokenResponse.class);

                if (response == null || response.getAccessToken() == null) {
                    throw new RuntimeException("No access token in response from Keycloak");
                }

                // Cache token with expiration
                cachedAccessToken = response.getAccessToken();
                int expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 300; // Default 5 min
                tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

                log.debug("Keycloak token cached for ping authentication, expires at: {}", tokenExpiresAt);
                return cachedAccessToken;
            } catch (Exception e) {
                log.error("Failed to get Keycloak token for ping authentication", e);
                throw new RuntimeException("Failed to get Keycloak token: " + e.getMessage(), e);
            }
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Builds the Keycloak token endpoint URL.
     * <p>
     * Uses explicit tokenEndpoint if provided, otherwise constructs from realm and keycloakUrl.
     * </p>
     *
     * @return token endpoint URL
     * @throws IllegalStateException if configuration is insufficient
     */
    private String buildTokenEndpoint() {
        // Use explicit token endpoint if provided
        if (StringUtils.hasText(clientCredentialsConfig.getTokenEndpoint())) {
            return clientCredentialsConfig.getTokenEndpoint();
        }

        // Construct from realm and keycloakUrl
        String realm = StringUtils.hasText(clientCredentialsConfig.getRealm())
                ? clientCredentialsConfig.getRealm()
                : "config-control";

        String keycloakUrl = clientCredentialsConfig.getKeycloakUrl();
        if (!StringUtils.hasText(keycloakUrl)) {
            throw new IllegalStateException(
                    "Keycloak token endpoint not configured. " +
                            "Either set zcm.sdk.ping.client-credentials.token-endpoint or " +
                            "set zcm.sdk.ping.client-credentials.keycloak-url");
        }

        // Remove trailing slash if present
        if (keycloakUrl.endsWith("/")) {
            keycloakUrl = keycloakUrl.substring(0, keycloakUrl.length() - 1);
        }

        return String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
    }

    /**
     * Token response from Keycloak.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class KeycloakTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public Integer getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Integer expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }
    }
}

