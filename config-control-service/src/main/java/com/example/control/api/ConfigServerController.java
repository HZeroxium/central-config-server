package com.example.control.api;

import com.example.control.application.ConfigServerClient;
import com.example.control.config.ConfigServerProperties;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "Config Server", description = "Explore Spring Cloud Config Server endpoints via a normalized facade")
public class ConfigServerController {

  private final ConfigServerClient client;
  private final ConfigServerProperties props;

  @GetMapping(value = "/actuator", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Actuator index", description = "Proxy to Config Server /actuator")
  @Timed(value = "api.config.actuator")
  public ResponseEntity<String> actuatorIndex() {
    return ResponseEntity.ok(client.getActuatorIndex());
  }

  @GetMapping(value = "/actuator/{*path}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Actuator path", description = "Proxy to Config Server /actuator/{path}")
  @Timed(value = "api.config.actuator.path")
  public ResponseEntity<String> actuatorPath(@PathVariable("path") String path) {
    return ResponseEntity.ok(client.getActuatorPath(path));
  }

  @GetMapping(value = "/env/{application}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Environment (JSON)", description = "Get environment for application and profiles")
  @Timed(value = "api.config.env")
  public ResponseEntity<String> env(
      @PathVariable String application,
      @RequestParam(required = false, defaultValue = "default") @Parameter(description = "Comma-separated profiles") String profiles) {
    return ResponseEntity.ok(client.getEnvironment(application, profiles));
  }

  @GetMapping(value = "/env/{application}/label/{label}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Environment with label", description = "Get environment for application, profiles and label")
  @Timed(value = "api.config.env.label")
  public ResponseEntity<String> envWithLabel(
      @PathVariable String application,
      @PathVariable String label,
      @RequestParam(required = false, defaultValue = "default") String profiles) {
    return ResponseEntity.ok(client.getEnvironmentWithLabel(application, profiles, label));
  }

  @GetMapping("/base")
  @Operation(summary = "Config Server base", description = "Return current Config Server base URL")
  public ResponseEntity<Map<String, String>> base() {
    return ResponseEntity.ok(Map.of("url", props.getUrl()));
  }
}
