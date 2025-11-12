package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import lombok.RequiredArgsConstructor;
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
 * Supports API key authentication via X-API-Key header when configured.
 */
@Slf4j
public class HttpRestPingStrategy implements PingStrategy {

  private final SdkProperties sdkProperties;
  private final RestClient restClient;

  /**
   * Constructor with SdkProperties for API key support.
   *
   * @param sdkProperties SDK configuration properties
   */
  public HttpRestPingStrategy(SdkProperties sdkProperties) {
    this.sdkProperties = sdkProperties;
    this.restClient = RestClient.builder().build();
  }

  /**
   * Constructor with custom RestClient (for testing).
   *
   * @param sdkProperties SDK configuration properties
   * @param restClient custom RestClient instance
   */
  public HttpRestPingStrategy(SdkProperties sdkProperties, RestClient restClient) {
    this.sdkProperties = sdkProperties;
    this.restClient = restClient;
  }

  @Override
  public void sendHeartbeat(String endpoint, HeartbeatPayload payload) throws Exception {
    Map<String, Object> body = convertToMap(payload);

    var requestBuilder = restClient.post()
        .uri(endpoint + "/api/heartbeat")
        .contentType(MediaType.APPLICATION_JSON);

    // Add API key header if configured and enabled
    if (sdkProperties != null 
        && sdkProperties.getApiKey() != null 
        && sdkProperties.getApiKey().isEnabled()
        && StringUtils.hasText(sdkProperties.getApiKey().getKey())) {
      requestBuilder.header("X-API-Key", sdkProperties.getApiKey().getKey());
      log.debug("Including API key in heartbeat request");
    }

    requestBuilder
        .body(body)
        .retrieve()
        .toBodilessEntity();

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
