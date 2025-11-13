package com.example.sample.web;

import com.vng.zing.zcm.client.ClientApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Key-Value store operations demo.
 * <p>
 * This controller demonstrates the usage of KVApi from zcm-spring-sdk-starter
 * for reading KV entries from config-control-service.
 */
@Slf4j
@RestController
@RequestMapping("/api/kv")
@RequiredArgsConstructor
@Tag(name = "Key-Value Store", description = "Key-Value store operations demo")
public class KVController {

  private final ClientApi clientApi;

  /**
   * Get a KV entry value as string.
   *
   * @param serviceId the service ID
   * @param key       the key path (relative to service root, may contain slashes)
   * @return the string value or 404 if not found
   */
  @GetMapping("/{serviceId}/string")
  @Operation(
      summary = "Get a KV entry as string",
      description = "Retrieve a KV entry value as UTF-8 string. Key may contain slashes (e.g., config/api.endpoint)"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "KV entry found"),
      @ApiResponse(responseCode = "404", description = "KV entry not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<String> getString(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/db.url") @RequestParam String key) {
    // Normalize key: strip leading slash if present (/{*key} captures with leading slash)
    String normalizedKey = (key != null && key.startsWith("/")) ? key.substring(1) : key;
    log.debug("Getting string KV entry for service: {}, key: {} (normalized: {})", serviceId, key, normalizedKey);

    try {
      String value = clientApi.kv().getString(serviceId, normalizedKey);
      if (value == null) {
        log.debug("KV entry not found for service: {}, key: {}", serviceId, normalizedKey);
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(value);
    } catch (Exception e) {
      log.error("Error getting KV entry for service: {}, key: {}", serviceId, normalizedKey, e);
      return ResponseEntity.status(500).body("Error: " + e.getMessage());
    }
  }

  /**
   * Get a KV entry value as integer.
   *
   * @param serviceId the service ID
   * @param key       the key path
   * @return the integer value or 404 if not found
   */
  @GetMapping("/{serviceId}/integer")
  @Operation(
      summary = "Get a KV entry as integer",
      description = "Retrieve a KV entry value as integer"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "KV entry found"),
      @ApiResponse(responseCode = "404", description = "KV entry not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<Integer> getInteger(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/server.port") @RequestParam String key) {
    String normalizedKey = (key != null && key.startsWith("/")) ? key.substring(1) : key;
    log.debug("Getting integer KV entry for service: {}, key: {} (normalized: {})", serviceId, key, normalizedKey);

    Integer value = clientApi.kv().getInteger(serviceId, normalizedKey);
    if (value == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(value);
  }

  /**
   * Get a KV entry value as boolean.
   *
   * @param serviceId the service ID
   * @param key       the key path
   * @return the boolean value or 404 if not found
   */
  @GetMapping("/{serviceId}/boolean")
  @Operation(
      summary = "Get a KV entry as boolean",
      description = "Retrieve a KV entry value as boolean"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "KV entry found"),
      @ApiResponse(responseCode = "404", description = "KV entry not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<Boolean> getBoolean(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/feature.enabled") @RequestParam String key) {
    String normalizedKey = (key != null && key.startsWith("/")) ? key.substring(1) : key;
    log.debug("Getting boolean KV entry for service: {}, key: {} (normalized: {})", serviceId, key, normalizedKey);

    Boolean value = clientApi.kv().getBoolean(serviceId, normalizedKey);
    if (value == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(value);
  }

  /**
   * Get a KV entry value as raw bytes.
   *
   * @param serviceId the service ID
   * @param key       the key path
   * @return the raw bytes or 404 if not found
   */
  @GetMapping("/{serviceId}/bytes")
  @Operation(
      summary = "Get a KV entry as raw bytes",
      description = "Retrieve a KV entry value as raw bytes"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "KV entry found"),
      @ApiResponse(responseCode = "404", description = "KV entry not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<byte[]> getBytes(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/db.url") @RequestParam String key) {
    String normalizedKey = (key != null && key.startsWith("/")) ? key.substring(1) : key;
    log.debug("Getting raw KV entry for service: {}, key: {} (normalized: {})", serviceId, key, normalizedKey);

    byte[] value = clientApi.kv().getBytes(serviceId, normalizedKey);
    if (value == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(value);
  }

  /**
   * Get a KV entry value as a list of strings.
   *
   * @param serviceId the service ID
   * @param key       the key path
   * @return list of strings or empty list if not found
   */
  @GetMapping("/{serviceId}/list")
  @Operation(
      summary = "Get a KV entry as list",
      description = "Retrieve a KV entry value as a list of strings (comma-separated or structured list)"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "KV entry found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<String>> getList(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/hosts") @RequestParam String key) {
    String normalizedKey = (key != null && key.startsWith("/")) ? key.substring(1) : key;
    log.debug("Getting list KV entry for service: {}, key: {} (normalized: {})", serviceId, key, normalizedKey);

    List<String> value = clientApi.kv().getList(serviceId, normalizedKey);
    return ResponseEntity.ok(value);
  }

  /**
   * Get all KV entries under a prefix as a map.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix to list (optional, defaults to empty string for root)
   * @return map of key-value pairs
   */
  @GetMapping("/{serviceId}")
  @Operation(
      summary = "Get KV entries as map",
      description = "Get all KV entries under a prefix as a flat map"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved map"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<Map<String, Object>> getMap(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Prefix to list", example = "config/") @RequestParam(defaultValue = "") String prefix) {
    log.debug("Getting KV map for service: {}, prefix: {}", serviceId, prefix);

    Map<String, Object> map = clientApi.kv().getMap(serviceId, prefix);
    return ResponseEntity.ok(map);
  }

  /**
   * Get a structured list stored under a prefix.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix to list
   * @return list of maps (each map represents an item)
   */
  @GetMapping("/{serviceId}/structured-list")
  @Operation(
      summary = "Get structured list",
      description = "Get a structured list stored under a prefix as a list of maps"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved structured list"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<Map<String, Object>>> getStructuredList(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Prefix", example = "config/items") @RequestParam String prefix) {
    String normalizedPrefix = (prefix != null && prefix.startsWith("/")) ? prefix.substring(1) : prefix;
    log.debug("Getting structured list for service: {}, prefix: {} (normalized: {})", serviceId, prefix, normalizedPrefix);

    List<Map<String, Object>> list = clientApi.kv().getStructuredList(serviceId, normalizedPrefix);
    return ResponseEntity.ok(list);
  }

  /**
   * List only keys (not full entries) under a prefix.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix to list (optional, defaults to empty string for root)
   * @return list of key paths
   */
  @GetMapping("/{serviceId}/keys")
  @Operation(
      summary = "List KV keys",
      description = "List only key paths (not full entries) under a prefix for a service"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<String>> listKeys(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Prefix to list", example = "config/") @RequestParam(defaultValue = "") String prefix) {
    log.debug("Listing KV keys for service: {}, prefix: {}", serviceId, prefix);

    List<String> keys = clientApi.kv().listKeys(serviceId, prefix);
    return ResponseEntity.ok(keys);
  }

  /**
   * Check if a KV entry exists.
   *
   * @param serviceId the service ID
   * @param key       the key path
   * @return true if exists, false otherwise
   */
  @GetMapping("/{serviceId}/exists")
  @Operation(
      summary = "Check if KV entry exists",
      description = "Check if a KV entry exists"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Check completed"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<Boolean> exists(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/db.url") @RequestParam String key) {
    String normalizedKey = (key != null && key.startsWith("/")) ? key.substring(1) : key;
    log.debug("Checking if KV entry exists for service: {}, key: {} (normalized: {})", serviceId, key, normalizedKey);

    boolean exists = clientApi.kv().exists(serviceId, normalizedKey);
    return ResponseEntity.ok(exists);
  }
}

