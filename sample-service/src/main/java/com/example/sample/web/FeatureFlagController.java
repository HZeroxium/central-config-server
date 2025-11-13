package com.example.sample.web;

import com.vng.zing.zcm.client.ClientApi;
import com.vng.zing.zcm.featureflags.SpringUnleashContextProvider;
import io.getunleash.UnleashContext;
import io.getunleash.Variant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for testing and interacting with Unleash Feature Flags.
 * <p>
 * This controller provides endpoints to:
 * <ul>
 *   <li>Check if a feature flag is enabled</li>
 *   <li>Get variant for a feature flag</li>
 *   <li>List all available feature flags</li>
 * </ul>
 * <p>
 * All endpoints automatically build UnleashContext from the current request
 * (userId from SecurityContext, sessionId, remoteAddress, etc.).
 */
@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
@Tag(name = "Feature Flags Controller", description = "Endpoints for checking and managing Unleash Feature Flags")
public class FeatureFlagController {

  private final ClientApi client;
  private final ObjectProvider<SpringUnleashContextProvider> contextProviderProvider;

  /**
   * Checks if a feature flag is enabled.
   * Automatically builds UnleashContext from the current request.
   *
   * @param flagName the name of the feature flag
   * @param fallback optional fallback value (default: false)
   * @return flag enabled status
   */
  @Operation(
      summary = "Check if feature flag is enabled",
      description = "Checks if a feature flag is enabled using context from the current request (userId, sessionId, remoteAddress, etc.)")
  @ApiResponse(responseCode = "200", description = "Flag status retrieved successfully")
  @GetMapping("/{flagName}")
  public ResponseEntity<Map<String, Object>> checkFlag(
      @Parameter(description = "The name of the feature flag", required = true, example = "checkout.one-click")
      @PathVariable String flagName,
      @Parameter(description = "Fallback value if flag is not found", required = false)
      @RequestParam(required = false, defaultValue = "false") boolean fallback) {
    
    try {
      SpringUnleashContextProvider provider = contextProviderProvider.getIfAvailable();
      if (provider == null) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("message", "SpringUnleashContextProvider is not available. Feature flags may not be properly configured.");
        return ResponseEntity.badRequest().body(error);
      }
      UnleashContext context = provider.getContext();
      boolean enabled = client.featureFlags().isEnabled(flagName, context, fallback);
      
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", "ok");
      result.put("flagName", flagName);
      result.put("enabled", enabled);
      result.put("fallback", fallback);
      result.put("context", Map.of(
          "userId", context.getUserId() != null ? context.getUserId() : "null",
          "sessionId", context.getSessionId() != null ? context.getSessionId() : "null",
          "remoteAddress", context.getRemoteAddress() != null ? context.getRemoteAddress() : "null",
          "appName", context.getAppName() != null ? context.getAppName() : "null",
          "environment", context.getEnvironment() != null ? context.getEnvironment() : "null",
          "properties", context.getProperties() != null ? context.getProperties() : Map.of()
      ));
      
      return ResponseEntity.ok(result);
    } catch (IllegalStateException e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", "Feature flags are not enabled or configured. " + e.getMessage());
      return ResponseEntity.badRequest().body(error);
    } catch (Exception e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to check feature flag: " + e.getMessage());
      return ResponseEntity.internalServerError().body(error);
    }
  }

  /**
   * Gets the variant for a feature flag.
   * Automatically builds UnleashContext from the current request.
   *
   * @param flagName the name of the feature flag
   * @return variant information
   */
  @Operation(
      summary = "Get feature flag variant",
      description = "Gets the variant for a feature flag using context from the current request")
  @ApiResponse(responseCode = "200", description = "Variant retrieved successfully")
  @GetMapping("/{flagName}/variant")
  public ResponseEntity<Map<String, Object>> getVariant(
      @Parameter(description = "The name of the feature flag", required = true, example = "checkout.button-color")
      @PathVariable String flagName) {
    
    try {
      SpringUnleashContextProvider provider = contextProviderProvider.getIfAvailable();
      if (provider == null) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("message", "SpringUnleashContextProvider is not available. Feature flags may not be properly configured.");
        return ResponseEntity.badRequest().body(error);
      }
      UnleashContext context = provider.getContext();
      Variant variant = client.featureFlags().getVariant(flagName, context);
      
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", "ok");
      result.put("flagName", flagName);
      result.put("variant", Map.of(
          "name", variant.getName(),
          "enabled", variant.isEnabled(),
          "payload", variant.getPayload() != null ? variant.getPayload() : Map.of()
      ));
      result.put("context", Map.of(
          "userId", context.getUserId() != null ? context.getUserId() : "null",
          "sessionId", context.getSessionId() != null ? context.getSessionId() : "null",
          "remoteAddress", context.getRemoteAddress() != null ? context.getRemoteAddress() : "null",
          "appName", context.getAppName() != null ? context.getAppName() : "null",
          "environment", context.getEnvironment() != null ? context.getEnvironment() : "null",
          "properties", context.getProperties() != null ? context.getProperties() : Map.of()
      ));
      
      return ResponseEntity.ok(result);
    } catch (IllegalStateException e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", "Feature flags are not enabled or configured. " + e.getMessage());
      return ResponseEntity.badRequest().body(error);
    } catch (Exception e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to get variant: " + e.getMessage());
      return ResponseEntity.internalServerError().body(error);
    }
  }

  /**
   * Lists all available feature flags.
   * Note: This is a placeholder - Unleash SDK doesn't provide a direct method to list all flags.
   *
   * @return list of flags (may be empty)
   */
  @Operation(
      summary = "List all feature flags",
      description = "Lists all available feature flags (placeholder - may return empty)")
  @ApiResponse(responseCode = "200", description = "Flags list retrieved successfully")
  @GetMapping
  public ResponseEntity<Map<String, Object>> getAllFlags() {
    try {
      Map<String, Boolean> flags = client.featureFlags().getAllFlags();
      
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", "ok");
      result.put("flags", flags);
      result.put("count", flags.size());
      result.put("note", "This endpoint is a placeholder. Unleash SDK doesn't provide a direct method to list all flags.");
      
      return ResponseEntity.ok(result);
    } catch (IllegalStateException e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", "Feature flags are not enabled or configured. " + e.getMessage());
      return ResponseEntity.badRequest().body(error);
    } catch (Exception e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to get all flags: " + e.getMessage());
      return ResponseEntity.internalServerError().body(error);
    }
  }
}

