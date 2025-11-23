package com.vng.zing.zcm.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vng.zing.zcm.config.SdkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Unified service for fetching and caching client credentials tokens from Keycloak.
 * <p>
 * This service is used by both ping operations and KV operations for M2M authentication.
 * It implements token caching with automatic refresh before expiration and uses a
 * thread-safe approach with double-checked locking to prevent concurrent token requests.
 * </p>
 * <p>
 * <strong>Token Endpoint Discovery:</strong>
 * If tokenEndpoint is not provided in configuration, the service will attempt to
 * discover it from config-control-service. If discovery fails, it will fall back to
 * constructing the endpoint from keycloakBaseUrl + realm.
 * </p>
 */
@Slf4j
public class ClientCredentialsTokenService {

    private static final int TOKEN_REFRESH_BUFFER_SECONDS = 30;

    private final RestClient restClient;
    private final RestClient discoveryRestClient; // For token endpoint discovery
    private final SdkProperties.ClientCredentials clientCredentialsConfig;
    private final SdkProperties sdkProperties;

    private String cachedAccessToken;
    private Instant tokenExpiresAt;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // Cache for discovered token endpoint
    private volatile String cachedTokenEndpoint;
    private final ReentrantLock endpointLock = new ReentrantLock();

    /**
     * Creates a new ClientCredentialsTokenService.
     *
     * @param restClient RestClient for HTTP requests to Keycloak
     * @param discoveryRestClient RestClient for discovering token endpoint from config-control-service (optional)
     * @param clientCredentialsConfig Client credentials configuration
     * @param sdkProperties SDK properties (for service name and control URL)
     */
    public ClientCredentialsTokenService(
            RestClient restClient,
            RestClient discoveryRestClient,
            SdkProperties.ClientCredentials clientCredentialsConfig,
            SdkProperties sdkProperties) {
        this.restClient = restClient;
        this.discoveryRestClient = discoveryRestClient;
        this.clientCredentialsConfig = clientCredentialsConfig;
        this.sdkProperties = sdkProperties;
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
            log.debug("Fetching new Keycloak token for client credentials authentication");
            String tokenUrl = getTokenEndpoint();

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

                log.debug("Keycloak token cached, expires at: {}", tokenExpiresAt);
                return cachedAccessToken;
            } catch (Exception e) {
                log.error("Failed to get Keycloak token", e);
                throw new RuntimeException("Failed to get Keycloak token: " + e.getMessage(), e);
            }
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Gets the token endpoint URL, with automatic discovery if not configured.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Use explicit tokenEndpoint if provided</li>
     *   <li>Try to discover from config-control-service</li>
     *   <li>Construct from keycloakBaseUrl + realm</li>
     *   <li>Throw exception if none available</li>
     * </ol>
     *
     * @return token endpoint URL
     * @throws IllegalStateException if configuration is insufficient
     */
    private String getTokenEndpoint() {
        // Use explicit token endpoint if provided
        if (StringUtils.hasText(clientCredentialsConfig.getTokenEndpoint())) {
            return clientCredentialsConfig.getTokenEndpoint();
        }

        // Check cache first
        if (cachedTokenEndpoint != null) {
            return cachedTokenEndpoint;
        }

        // Try to discover from config-control-service
        endpointLock.lock();
        try {
            // Double-check after acquiring lock
            if (cachedTokenEndpoint != null) {
                return cachedTokenEndpoint;
            }

            String discovered = discoverTokenEndpoint();
            if (discovered != null) {
                cachedTokenEndpoint = discovered;
                log.info("Discovered token endpoint from config-control-service: {}", discovered);
                return discovered;
            }

            // Fallback: construct from keycloakBaseUrl + realm
            String constructed = buildTokenEndpoint();
            if (constructed != null) {
                cachedTokenEndpoint = constructed;
                log.info("Constructed token endpoint from keycloakBaseUrl + realm: {}", constructed);
                return constructed;
            }

            throw new IllegalStateException(
                    "Keycloak token endpoint not configured. " +
                            "Either set zcm.sdk.client-credentials.token-endpoint, " +
                            "set zcm.sdk.client-credentials.keycloak-base-url, " +
                            "or ensure zcm.sdk.control-url is configured for auto-discovery.");
        } finally {
            endpointLock.unlock();
        }
    }

    /**
     * Attempts to discover the token endpoint from config-control-service.
     * <p>
     * Calls GET /api/services/{serviceName}/credentials endpoint to retrieve
     * the tokenEndpoint from the service credentials response.
     * </p>
     *
     * @return discovered token endpoint URL, or null if discovery fails
     */
    private String discoverTokenEndpoint() {
        if (discoveryRestClient == null) {
            log.debug("Discovery RestClient not available, skipping token endpoint discovery");
            return null;
        }

        String serviceName = sdkProperties.getServiceName();
        String controlUrl = sdkProperties.getControlUrl();

        if (!StringUtils.hasText(serviceName)) {
            log.debug("Service name not configured, cannot discover token endpoint");
            return null;
        }

        if (!StringUtils.hasText(controlUrl)) {
            log.debug("Control URL not configured, cannot discover token endpoint");
            return null;
        }

        try {
            // Use service name as serviceId (assuming serviceId matches service name)
            // Note: This endpoint requires authentication, so discovery may fail
            // In that case, we fall back to construction
            String discoveryUrl = String.format("%s/api/services/%s/credentials", controlUrl, serviceName);

            log.debug("Attempting to discover token endpoint from: {}", discoveryUrl);

            ServiceCredentialResponse response = discoveryRestClient.get()
                    .uri(discoveryUrl)
                    .retrieve()
                    .body(ServiceCredentialResponse.class);

            if (response != null && StringUtils.hasText(response.getTokenEndpoint())) {
                return response.getTokenEndpoint();
            }

            log.debug("Token endpoint not found in discovery response");
            return null;
        } catch (RestClientException e) {
            log.debug("Failed to discover token endpoint from config-control-service: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.debug("Unexpected error during token endpoint discovery: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Constructs the token endpoint URL from keycloakBaseUrl and realm.
     *
     * @return constructed token endpoint URL, or null if keycloakBaseUrl is not configured
     */
    private String buildTokenEndpoint() {
        String keycloakBaseUrl = clientCredentialsConfig.getKeycloakBaseUrl();
        if (!StringUtils.hasText(keycloakBaseUrl)) {
            return null;
        }

        String realm = StringUtils.hasText(clientCredentialsConfig.getRealm())
                ? clientCredentialsConfig.getRealm()
                : "config-control";

        // Remove trailing slash if present
        if (keycloakBaseUrl.endsWith("/")) {
            keycloakBaseUrl = keycloakBaseUrl.substring(0, keycloakBaseUrl.length() - 1);
        }

        return String.format("%s/realms/%s/protocol/openid-connect/token", keycloakBaseUrl, realm);
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

    /**
     * Service credential response from config-control-service.
     * Used for token endpoint discovery.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ServiceCredentialResponse {
        @JsonProperty("clientId")
        private String clientId;

        @JsonProperty("clientSecret")
        private String clientSecret;

        @JsonProperty("status")
        private String status;

        @JsonProperty("tokenEndpoint")
        private String tokenEndpoint;

        public String getTokenEndpoint() {
            return tokenEndpoint;
        }

        public void setTokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
        }
    }
}

