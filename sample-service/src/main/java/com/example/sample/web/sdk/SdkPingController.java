package com.example.sample.web.sdk;

import com.vng.zing.zcm.client.ClientApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for SDK ping operations.
 * <p>
 * Provides endpoints to manually trigger heartbeat pings to the control service.
 */
@RestController
@RequestMapping("/api/sdk/ping")
@RequiredArgsConstructor
@Tag(name = "SDK Ping", description = "Endpoints for triggering SDK heartbeat pings")
public class SdkPingController {

  private final ClientApi client;

  /**
   * Triggers an immediate ping to the control service.
   *
   * @return success confirmation
   */
  @Operation(
      summary = "Trigger SDK ping",
      description = "Manually triggers a ping to the control service to update heartbeat and configuration hash")
  @ApiResponse(responseCode = "200", description = "Ping triggered successfully")
  @PostMapping
  public ResponseEntity<Map<String, String>> triggerPing() {
    client.pingNow();
    return ResponseEntity.ok(Map.of("status", "ok", "message", "Ping triggered"));
  }
}

