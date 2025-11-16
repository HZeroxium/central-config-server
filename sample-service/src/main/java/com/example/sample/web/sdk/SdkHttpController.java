package com.example.sample.web.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vng.zing.zcm.client.ClientApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * REST controller for HTTP service calls via SDK.
 * <p>
 * Provides endpoints to make HTTP calls to other services using the SDK's load-balanced RestClient.
 */
@RestController
@RequestMapping("/api/sdk/http")
@RequiredArgsConstructor
@Tag(name = "SDK HTTP Client", description = "Endpoints for making HTTP calls to discovered services")
public class SdkHttpController {

  private final ClientApi client;
  private final ObjectMapper objectMapper;

  /**
   * Calls a target service via the load-balanced {@link ClientApi#http()} RestClient.
   *
   * @param serviceName logical service name (registered in discovery)
   * @param endpoint    relative endpoint path (e.g., /api/health)
   * @return response body from the target service
   */
  @Operation(
      summary = "Call another HTTP service via SDK",
      description = "Perform a GET request to another service using the SDK's load-balanced RestClient")
  @ApiResponse(responseCode = "200", description = "Call succeeded",
      content = @Content(schema = @Schema(implementation = Map.class)))
  @ApiResponse(responseCode = "400", description = "Service call failed")
  @PostMapping("/call-http-service")
  public ResponseEntity<Map<String, Object>> callHttpService(
      @Parameter(description = "Service name to call", required = true, example = "sample-service")
      @RequestParam String serviceName,
      @Parameter(description = "Endpoint path to call within the service", required = true, example = "/api/sdk/config/snapshot")
      @RequestParam String endpoint) {

    try {
      // Use RestClient to make the call
      RestClient restClient = client.http().client();
      ResponseEntity<String> response = restClient.get()
          .uri("http://" + serviceName + endpoint)
          .retrieve()
          .toEntity(String.class);
      
      // Parse JSON response using ObjectMapper
      Map<String, Object> body;
      try {
        body = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
      } catch (Exception ex) {
        body = Map.of("rawResponse", response.getBody(), "parseError", ex.getMessage());
      }
      
      return ResponseEntity.ok(Map.of("status", "ok", "response", body));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of(
          "status", "error", 
          "message", "Failed to call HTTP service: " + e.getMessage()));
    }
  }
}

