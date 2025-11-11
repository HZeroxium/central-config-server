package com.vng.zing.zcm.client.featureflag;

import io.getunleash.UnleashContext;
import io.getunleash.Variant;

import java.util.Map;

/**
 * Feature Flags API for checking flag states and variants.
 * <p>
 * This API provides access to Unleash Feature Flags functionality,
 * allowing services to check if features are enabled and retrieve variants.
 */
public interface FeatureFlagApi {

  /**
   * Checks if a feature flag is enabled.
   * 
   * @param flagName the name of the feature flag
   * @param context the Unleash context (userId, sessionId, remoteAddress, etc.)
   * @param fallback the fallback value if flag is not found or server is unavailable
   * @return true if the flag is enabled, false otherwise
   */
  boolean isEnabled(String flagName, UnleashContext context, boolean fallback);

  /**
   * Checks if a feature flag is enabled (with default fallback false).
   * 
   * @param flagName the name of the feature flag
   * @param context the Unleash context
   * @return true if the flag is enabled, false otherwise
   */
  boolean isEnabled(String flagName, UnleashContext context);

  /**
   * Gets the variant for a feature flag.
   * 
   * @param flagName the name of the feature flag
   * @param context the Unleash context
   * @return the variant, or a disabled variant if flag is not found
   */
  Variant getVariant(String flagName, UnleashContext context);

  /**
   * Gets all available feature flags.
   * 
   * @return map of flag names to their enabled state
   */
  Map<String, Boolean> getAllFlags();
}

