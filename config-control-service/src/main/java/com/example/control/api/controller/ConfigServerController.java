package com.example.control.api.controller;

import com.example.control.api.dto.common.ApiResponseDto;
import com.example.control.api.dto.configserver.ConfigServerDto;
import com.example.control.application.ConfigServerClient;
import com.example.control.config.ConfigServerProperties;
import com.example.control.api.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
  @Operation(
      summary = "Get configuration environment",
      description = """
          Retrieves the configuration properties for a given application and profile from the Config Server.
          This acts as a proxy to the underlying Spring Cloud Config Server.
          """,
      security = @SecurityRequirement(name = "oauth2_auth_code"),
      operationId = "getEnvironmentConfigServer"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved configuration environment",
          content = @Content(schema = @Schema(implementation = ApiResponseDto.ApiResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid application or profile name",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Configuration not found for the specified application/profile",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error or Config Server unreachable",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Timed(value = "api.config-server.environment")
  public ResponseEntity<ApiResponseDto.ApiResponse<ConfigServerDto.ConfigEnvironmentResponse>> getEnvironment(
      @Parameter(description = "Name of the application", example = "payment-service") 
      @PathVariable String application,
      @Parameter(description = "Profile of the application (e.g., dev, prod)", example = "dev") 
      @PathVariable String profile,
      @Parameter(description = "Optional Git label/branch for configuration version", example = "main") 
      @RequestParam(required = false) String label) {

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
  @Operation(
      summary = "Get Config Server health status",
      description = """
          Retrieves the health status of the underlying Spring Cloud Config Server.
          This provides information about the Config Server's operational state.
          """,
      security = @SecurityRequirement(name = "oauth2_auth_code"),
      operationId = "getHealthConfigServer"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Config Server health status retrieved successfully",
          content = @Content(schema = @Schema(implementation = ApiResponseDto.ApiResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error or Config Server unreachable",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Timed(value = "api.config-server.health")
  public ResponseEntity<ApiResponseDto.ApiResponse<ConfigServerDto.ActuatorHealthResponse>> getHealth() {
    log.debug("Getting Config Server health status");

    String response = client.getActuatorPath("health");
    ConfigServerDto.ActuatorHealthResponse healthResponse = parseHealthResponse(response);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Health status retrieved successfully", healthResponse));
  }

  @GetMapping("/actuator/{path}")
  @Operation(
      summary = "Proxy to Config Server actuator endpoint",
      description = """
          Proxies requests to the underlying Spring Cloud Config Server actuator endpoints.
          This allows access to Config Server management and monitoring endpoints.
          """,
      security = @SecurityRequirement(name = "oauth2_auth_code"),
      operationId = "getActuatorEndpointConfigServer"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Actuator endpoint response retrieved successfully",
          content = @Content(schema = @Schema(implementation = ApiResponseDto.ApiResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid actuator path",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Actuator endpoint not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error or Config Server unreachable",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Timed(value = "api.config-server.actuator")
  public ResponseEntity<ApiResponseDto.ApiResponse<Object>> getActuatorEndpoint(
      @Parameter(description = "Actuator endpoint path", example = "env") 
      @PathVariable String path) {

    log.debug("Getting actuator endpoint: {}", path);

    String response = client.getActuatorPath(path);
    Object parsedResponse = parseJsonResponse(response);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Actuator endpoint retrieved successfully", parsedResponse));
  }

  @GetMapping("/info")
  @Operation(
      summary = "Get Config Server information",
      description = """
          Retrieves basic information about the Config Server instance.
          This includes URL, status, and version information.
          """,
      security = @SecurityRequirement(name = "oauth2_auth_code"),
      operationId = "getInfoConfigServer"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Config Server information retrieved successfully",
          content = @Content(schema = @Schema(implementation = ApiResponseDto.ApiResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
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
