package com.example.control.api;

import com.example.control.application.ConfigProxyService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for administrative operations including config refresh.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations for config management")
public class AdminController {

  private final ConfigProxyService configProxyService;
  private final CacheManager cacheManager;

  @PostMapping("/refresh")
  @Operation(summary = "Trigger refresh", description = "Broadcast config refresh event to all instances or specific destination via Config Server /busrefresh")
  @Timed(value = "api.admin.refresh", description = "Time taken to trigger refresh")
  public ResponseEntity<Map<String, Object>> triggerRefresh(
      @RequestParam(required = false) @Parameter(description = "Destination pattern (service:instance or service:** for all)") String destination) {

    log.info("Triggering config refresh for destination: {}", destination != null ? destination : "all");

    try {
      // Trigger refresh via Config Server's /busrefresh endpoint
      String response = configProxyService.triggerBusRefresh(destination);

      return ResponseEntity.ok(Map.of(
          "status", "ok",
          "message", "Refresh triggered via Config Server /busrefresh",
          "destination", destination != null ? destination : "all",
          "configServerResponse", response != null ? response : "success",
          "timestamp", System.currentTimeMillis()));
    } catch (Exception e) {
      log.error("Failed to trigger refresh for destination: {}", destination, e);
      return ResponseEntity.status(500).body(Map.of(
          "status", "error",
          "message", "Failed to trigger refresh: " + e.getMessage(),
          "destination", destination != null ? destination : "all",
          "timestamp", System.currentTimeMillis()));
    }
  }

  @PostMapping("/cache/clear")
  @Operation(summary = "Clear caches", description = "Clear all or specific cache")
  @Timed(value = "api.admin.cache.clear", description = "Time taken to clear cache")
  public ResponseEntity<Map<String, Object>> clearCache(
      @RequestParam(required = false) @Parameter(description = "Cache name to clear (or all if not specified)") String cacheName) {

    if (cacheName != null && !cacheName.isBlank()) {
      var cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
        log.info("Cleared cache: {}", cacheName);
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "message", "Cache cleared",
            "cache", cacheName));
      } else {
        throw new IllegalArgumentException("Cache not found: " + cacheName);
      }
    } else {
      // Clear all caches
      cacheManager.getCacheNames().forEach(name -> {
        var cache = cacheManager.getCache(name);
        if (cache != null) {
          cache.clear();
        }
      });
      log.info("Cleared all caches");

      return ResponseEntity.ok(Map.of(
          "status", "ok",
          "message", "All caches cleared",
          "cacheNames", cacheManager.getCacheNames()));
    }
  }

  @GetMapping("/health")
  @Operation(summary = "Admin health check", description = "Check if admin endpoints are operational")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "service", "admin-controller"));
  }
}
