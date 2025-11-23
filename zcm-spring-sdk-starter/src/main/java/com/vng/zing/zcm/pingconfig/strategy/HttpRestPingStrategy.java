package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.auth.ClientCredentialsTokenService;
import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

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
   * Constructor with SdkProperties, RestClient, and optional token service.
   * <p>
   * If tokenService is provided, it will be used. Otherwise, validates client credentials
   * configuration and creates a new token service.
   *
   * @param sdkProperties SDK configuration properties
   * @param restClient RestClient for HTTP requests
   * @param tokenService Optional token service (if null, will be created from config)
   * @throws IllegalStateException if client credentials are required but not configured
   */
  public HttpRestPingStrategy(SdkProperties sdkProperties, RestClient restClient, ClientCredentialsTokenService tokenService) {
    this.sdkProperties = sdkProperties;
    this.restClient = restClient;
    this.tokenService = tokenService;
  }

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

    // Validate client credentials configuration
    SdkProperties.ClientCredentials clientCredentials = sdkProperties != null
            ? sdkProperties.getClientCredentials()
            : null;

    if (clientCredentials == null || clientCredentials.isRequired()) {
      validateClientCredentials(clientCredentials);
      // Token service will be created by SdkAutoConfiguration, so we set it to null here
      // and expect it to be injected via constructor
      this.tokenService = null;
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
  private void validateClientCredentials(SdkProperties.ClientCredentials clientCredentials) {
    if (clientCredentials == null) {
      throw new IllegalStateException(
              "Client credentials are required for ping authentication. " +
                      "Please configure zcm.sdk.client-credentials.client-id and " +
                      "zcm.sdk.client-credentials.client-secret. " +
                      "Obtain credentials from Admin Dashboard after service approval.");
    }

    if (!StringUtils.hasText(clientCredentials.getClientId())) {
      throw new IllegalStateException(
              "Client ID is required for ping authentication. " +
                      "Set zcm.sdk.client-credentials.client-id or " +
                      "ZCM_SDK_CLIENT_CREDENTIALS_CLIENT_ID environment variable.");
    }

    if (!StringUtils.hasText(clientCredentials.getClientSecret())) {
      throw new IllegalStateException(
              "Client secret is required for ping authentication. " +
                      "Set zcm.sdk.client-credentials.client-secret or " +
                      "ZCM_SDK_CLIENT_CREDENTIALS_CLIENT_SECRET environment variable.");
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
      throw new IllegalStateException(
              "No authentication configured. Client credentials are required for ping authentication. " +
                      "Please configure zcm.sdk.client-credentials.client-id and " +
                      "zcm.sdk.client-credentials.client-secret. " +
                      "Obtain credentials from Admin Dashboard after service approval.");
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
