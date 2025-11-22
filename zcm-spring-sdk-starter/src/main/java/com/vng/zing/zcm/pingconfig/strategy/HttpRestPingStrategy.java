package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import com.vng.zing.zcm.pingconfig.auth.ClientCredentialsTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP REST implementation of the ping strategy.
 * <p>
 * This strategy sends heartbeat messages using HTTP POST requests to the
 * control service's REST API endpoint.
 * <p>
 * Requires client credentials authentication (Keycloak client credentials flow).
 * API key authentication is deprecated.
 */
@Slf4j
public class HttpRestPingStrategy implements PingStrategy {

  private final SdkProperties sdkProperties;
  private final RestClient restClient;
  private final ClientCredentialsTokenService tokenService;

  /**
   * Constructor with SdkProperties and RestClient.
   * <p>
   * Validates client credentials configuration and initializes token service.
   *
   * @param sdkProperties SDK configuration properties
   * @param restClient RestClient for HTTP requests
   * @throws IllegalStateException if client credentials are required but not configured
   */
  public HttpRestPingStrategy(SdkProperties sdkProperties, RestClient restClient) {
    this.sdkProperties = sdkProperties;
    this.restClient = restClient;

    // Validate and initialize client credentials
    SdkProperties.Ping.ClientCredentials clientCredentials = sdkProperties != null
            && sdkProperties.getPing() != null
            ? sdkProperties.getPing().getClientCredentials()
            : null;

    if (clientCredentials == null || clientCredentials.isRequired()) {
      validateClientCredentials(clientCredentials);
      this.tokenService = new ClientCredentialsTokenService(restClient, clientCredentials);
    } else {
      // Client credentials not required (backward compatibility)
      log.warn("Client credentials authentication is disabled. This is deprecated and may not work in future versions.");
      this.tokenService = null;
    }
  }

  /**
   * Constructor with SdkProperties (creates default RestClient).
   *
   * @param sdkProperties SDK configuration properties
   */
  public HttpRestPingStrategy(SdkProperties sdkProperties) {
    this(sdkProperties, RestClient.builder().build());
  }

  /**
   * Validates client credentials configuration.
   *
   * @param clientCredentials client credentials configuration
   * @throws IllegalStateException if validation fails
   */
  private void validateClientCredentials(SdkProperties.Ping.ClientCredentials clientCredentials) {
    if (clientCredentials == null) {
      throw new IllegalStateException(
              "Client credentials are required for ping authentication. " +
                      "Please configure zcm.sdk.ping.client-credentials.client-id and " +
                      "zcm.sdk.ping.client-credentials.client-secret. " +
                      "Obtain credentials from Admin Dashboard after service approval.");
    }

    if (!StringUtils.hasText(clientCredentials.getClientId())) {
      throw new IllegalStateException(
              "Client ID is required for ping authentication. " +
                      "Set zcm.sdk.ping.client-credentials.client-id or " +
                      "ZCM_SDK_PING_CLIENT_CREDENTIALS_CLIENT_ID environment variable.");
    }

    if (!StringUtils.hasText(clientCredentials.getClientSecret())) {
      throw new IllegalStateException(
              "Client secret is required for ping authentication. " +
                      "Set zcm.sdk.ping.client-credentials.client-secret or " +
                      "ZCM_SDK_PING_CLIENT_CREDENTIALS_CLIENT_SECRET environment variable.");
    }

    // Validate token endpoint can be constructed
    if (!StringUtils.hasText(clientCredentials.getTokenEndpoint())
            && !StringUtils.hasText(clientCredentials.getKeycloakUrl())) {
      throw new IllegalStateException(
              "Keycloak token endpoint not configured. " +
                      "Either set zcm.sdk.ping.client-credentials.token-endpoint or " +
                      "set zcm.sdk.ping.client-credentials.keycloak-url.");
    }
  }

  @Override
  public void sendHeartbeat(String endpoint, HeartbeatPayload payload) throws Exception {
    Map<String, Object> body = convertToMap(payload);

    var requestBuilder = restClient.post()
        .uri(endpoint + "/api/heartbeat")
        .contentType(MediaType.APPLICATION_JSON);

    // Add Bearer token for client credentials authentication
    if (tokenService != null) {
      try {
        String accessToken = tokenService.getAccessToken();
        requestBuilder.header("Authorization", "Bearer " + accessToken);
        log.debug("Including Bearer token in heartbeat request");
      } catch (Exception e) {
        log.error("Failed to obtain access token for heartbeat authentication", e);
        throw new RuntimeException("Failed to authenticate heartbeat request: " + e.getMessage(), e);
      }
    } else {
      // Fallback to API key (deprecated)
      SdkProperties.Ping.ClientCredentials clientCredentials = sdkProperties != null
              && sdkProperties.getPing() != null
              ? sdkProperties.getPing().getClientCredentials()
              : null;
      
      if (isDeprecationPeriodPassed(clientCredentials)) {
        throw new IllegalStateException(
                "API key authentication is no longer supported. Client credentials are required. " +
                "Please configure zcm.sdk.ping.client-credentials.client-id and " +
                "zcm.sdk.ping.client-credentials.client-secret. " +
                "Obtain credentials from Admin Dashboard after service approval.");
      }
      
      if (sdkProperties != null
              && sdkProperties.getApiKey() != null
              && sdkProperties.getApiKey().isEnabled()
              && StringUtils.hasText(sdkProperties.getApiKey().getKey())) {
        requestBuilder.header("X-API-Key", sdkProperties.getApiKey().getKey());
        log.warn("Using deprecated API key authentication. Please migrate to client credentials. " +
                "API key support will be removed after deprecation period ends.");
      } else {
        throw new IllegalStateException(
                "No authentication configured. Client credentials are required for ping authentication.");
      }
    }

    var responseEntity = requestBuilder
        .body(body)
        .retrieve()
        .toEntity(String.class);

    log.debug("Heartbeat response status: {}, body: {}",
            responseEntity.getStatusCode(), responseEntity.getBody());

    log.debug("HTTP ping sent to {}", endpoint);
  }

  @Override
  public String getName() {
    return "HTTP REST";
  }

  @Override
  public PingProtocol getProtocol() {
    return PingProtocol.HTTP;
  }

  /**
   * Checks if the API key deprecation period has passed.
   *
   * @param clientCredentials client credentials configuration
   * @return true if deprecation period has passed, false otherwise
   */
  private boolean isDeprecationPeriodPassed(SdkProperties.Ping.ClientCredentials clientCredentials) {
    if (clientCredentials == null || !StringUtils.hasText(clientCredentials.getDeprecationPeriodEnd())) {
      // No deprecation period configured - enforce immediately
      return true;
    }

    try {
      // Parse ISO-8601 date string (e.g., "2024-12-31T23:59:59Z")
      ZonedDateTime deprecationEnd = ZonedDateTime.parse(
          clientCredentials.getDeprecationPeriodEnd(),
          DateTimeFormatter.ISO_ZONED_DATE_TIME);
      ZonedDateTime now = ZonedDateTime.now(deprecationEnd.getZone());
      return now.isAfter(deprecationEnd) || now.isEqual(deprecationEnd);
    } catch (DateTimeParseException e) {
      log.warn("Invalid deprecation period end date format: {}. Enforcing client credentials requirement.",
          clientCredentials.getDeprecationPeriodEnd(), e);
      // If date format is invalid, enforce immediately
      return true;
    }
  }

  /**
   * Converts HeartbeatPayload to a Map for JSON serialization.
   * 
   * @param payload the heartbeat payload
   * @return map representation suitable for JSON serialization
   */
  private Map<String, Object> convertToMap(HeartbeatPayload payload) {
    Map<String, Object> map = new HashMap<>();
    map.put("serviceName", payload.getServiceName());
    map.put("instanceId", payload.getInstanceId());
    map.put("configHash", payload.getConfigHash());
    map.put("host", payload.getHost());
    map.put("port", payload.getPort());
    map.put("environment", payload.getEnvironment());
    map.put("version", payload.getVersion());
    map.put("metadata", payload.getMetadata());
    return map;
  }
}
