package com.example.control.api;

import com.example.control.api.dto.ApiResponseDto;
import com.example.control.api.dto.ConfigServerDto;
import com.example.control.application.ConfigServerClient;
import com.example.control.config.ConfigServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/config-server")
@RequiredArgsConstructor
@Tag(name = "Config Server", description = "Spring Cloud Config Server API proxy with standardized responses")
public class ConfigServerController {

  private final ConfigServerClient client;
  private final ConfigServerProperties props;
  private final ObjectMapper objectMapper;

  @GetMapping("/environment/{application}/{profile}")
  @Operation(summary = "Get configuration environment", description = "Get configuration for application and profile")
  @Timed(value = "api.config-server.environment")
  public ResponseEntity<ApiResponseDto.ApiResponse<ConfigServerDto.ConfigEnvironmentResponse>> getEnvironment(
      @PathVariable @Parameter(description = "Application name") String application,
      @PathVariable @Parameter(description = "Profile name") String profile,
      @RequestParam(required = false) @Parameter(description = "Git label/branch") String label) {

    log.debug("Getting environment for application: {}, profile: {}, label: {}", application, profile, label);

    String response = label != null
        ? client.getEnvironmentWithLabel(application, profile, label)
        : client.getEnvironment(application, profile);

    // Parse JSON response to DTO
    ConfigServerDto.ConfigEnvironmentResponse envResponse = parseEnvironmentResponse(response);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Configuration retrieved successfully", envResponse));
  }

  @GetMapping("/health")
  @Operation(summary = "Config Server health", description = "Get Config Server health status")
  @Timed(value = "api.config-server.health")
  public ResponseEntity<ApiResponseDto.ApiResponse<ConfigServerDto.ActuatorHealthResponse>> getHealth() {
    log.debug("Getting Config Server health status");

    String response = client.getActuatorPath("health");
    ConfigServerDto.ActuatorHealthResponse healthResponse = parseHealthResponse(response);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Health status retrieved successfully", healthResponse));
  }

  @GetMapping("/actuator/{path}")
  @Operation(summary = "Actuator endpoint", description = "Proxy to Config Server actuator endpoint")
  @Timed(value = "api.config-server.actuator")
  public ResponseEntity<ApiResponseDto.ApiResponse<Object>> getActuatorEndpoint(
      @PathVariable @Parameter(description = "Actuator path") String path) {

    log.debug("Getting actuator endpoint: {}", path);

    String response = client.getActuatorPath(path);
    Object parsedResponse = parseJsonResponse(response);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Actuator endpoint retrieved successfully", parsedResponse));
  }

  @GetMapping("/info")
  @Operation(summary = "Config Server info", description = "Get Config Server information")
  @Timed(value = "api.config-server.info")
  public ResponseEntity<ApiResponseDto.ApiResponse<ConfigServerDto.ConfigServerInfo>> getInfo() {
    log.debug("Getting Config Server info");

    ConfigServerDto.ConfigServerInfo info = ConfigServerDto.ConfigServerInfo.builder()
        .url(props.getUrl())
        .status("UP")
        .version("1.0.0")
        .build();

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Config Server info retrieved successfully", info));
  }

  // Helper methods for JSON parsing
  private ConfigServerDto.ConfigEnvironmentResponse parseEnvironmentResponse(String json) {
    try {
      return objectMapper.readValue(json, ConfigServerDto.ConfigEnvironmentResponse.class);
    } catch (Exception e) {
      log.error("Failed to parse environment response", e);
      return ConfigServerDto.ConfigEnvironmentResponse.builder()
          .name("error")
          .build();
    }
  }

  private ConfigServerDto.ActuatorHealthResponse parseHealthResponse(String json) {
    try {
      return objectMapper.readValue(json, ConfigServerDto.ActuatorHealthResponse.class);
    } catch (Exception e) {
      log.error("Failed to parse health response", e);
      return ConfigServerDto.ActuatorHealthResponse.builder()
          .status("UNKNOWN")
          .build();
    }
  }

  private Object parseJsonResponse(String json) {
    try {
      return objectMapper.readValue(json, Object.class);
    } catch (Exception e) {
      log.error("Failed to parse JSON response", e);
      return Map.of("error", "Failed to parse JSON", "raw", json);
    }
  }
}
