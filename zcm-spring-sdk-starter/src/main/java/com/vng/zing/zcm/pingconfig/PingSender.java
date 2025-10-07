package com.vng.zing.zcm.pingconfig;

import com.vng.zing.zcm.config.SdkProperties;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PingSender {

  private final RestClient rest;
  private final SdkProperties props;
  private final ConfigHashCalculator hash;

  public PingSender(RestClient rest, SdkProperties props, ConfigHashCalculator hash) {
    this.rest = rest;
    this.props = props;
    this.hash = hash;
  }

  public void send() {
    if (!props.getPing().isEnabled())
      return;
    String base = props.getControlUrl();
    if (!StringUtils.hasText(base))
      return;

    Map<String, Object> payload = new HashMap<>();
    payload.put("serviceName", props.getServiceName());
    payload.put("instanceId", props.getInstanceId());
    payload.put("configHash", hash.currentHash());
    payload.put("timestamp", Instant.now().toString());
    payload.put("host", host());
    payload.put("env", System.getenv());

    try {
      rest.post()
          .uri(base + "/api/heartbeat")
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ignored) {
      // swallow, avoid crashing schedule loop
    }
  }

  private String host() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      return "unknown";
    }
  }
}
