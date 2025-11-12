package com.example.sample.web;

import com.vng.zing.zcm.client.ClientApi;
import com.vng.zing.zcm.kv.dto.KVEntry;
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
import java.util.Optional;

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
   * Get a single KV entry.
   *
   * @param serviceId the service ID
   * @param key       the key path (relative to service root)
   * @return the KV entry or 404 if not found
   */
  @GetMapping("/{serviceId}/{key}")
  @Operation(
      summary = "Get a KV entry",
      description = "Retrieve a single KV entry by service ID and key path"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "KV entry found"),
      @ApiResponse(responseCode = "404", description = "KV entry not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<KVEntry> get(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/db.url") @PathVariable String key) {
    log.debug("Getting KV entry for service: {}, key: {}", serviceId, key);

    Optional<KVEntry> entry = clientApi.kv().get(serviceId, key);
    if (entry.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(entry.get());
  }

  /**
   * Get a KV entry value as raw bytes.
   *
   * @param serviceId the service ID
   * @param key       the key path
   * @return the raw bytes or 404 if not found
   */
  @GetMapping("/{serviceId}/{key}/raw")
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
  public ResponseEntity<byte[]> getRaw(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/db.url") @PathVariable String key) {
    log.debug("Getting raw KV entry for service: {}, key: {}", serviceId, key);

    Optional<byte[]> value = clientApi.kv().getRaw(serviceId, key);
    if (value.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(value.get());
  }

  /**
   * Get a KV entry value as string.
   *
   * @param serviceId the service ID
   * @param key       the key path
   * @return the string value or 404 if not found
   */
  @GetMapping("/{serviceId}/{key}/string")
  @Operation(
      summary = "Get a KV entry as string",
      description = "Retrieve a KV entry value as UTF-8 string"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "KV entry found"),
      @ApiResponse(responseCode = "404", description = "KV entry not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<String> getString(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Key path", example = "config/db.url") @PathVariable String key) {
    log.debug("Getting string KV entry for service: {}, key: {}", serviceId, key);

    Optional<String> value = clientApi.kv().getString(serviceId, key);
    if (value.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(value.get());
  }

  /**
   * List KV entries under a prefix.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix to list (optional, defaults to empty string for root)
   * @return list of KV entries
   */
  @GetMapping("/{serviceId}")
  @Operation(
      summary = "List KV entries",
      description = "List all KV entries under a prefix for a service"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<KVEntry>> list(
      @Parameter(description = "Service ID", example = "sample-service") @PathVariable String serviceId,
      @Parameter(description = "Prefix to list", example = "config/") @RequestParam(defaultValue = "") String prefix) {
    log.debug("Listing KV entries for service: {}, prefix: {}", serviceId, prefix);

    List<KVEntry> entries = clientApi.kv().list(serviceId, prefix);
    return ResponseEntity.ok(entries);
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
}

