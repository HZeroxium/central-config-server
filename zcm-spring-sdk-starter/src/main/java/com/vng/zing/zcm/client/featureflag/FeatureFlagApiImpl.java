package com.vng.zing.zcm.client.featureflag;

import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.Variant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of FeatureFlagApi wrapping Unleash SDK.
 */
@Slf4j
@RequiredArgsConstructor
public class FeatureFlagApiImpl implements FeatureFlagApi {

  private final Unleash unleash;

  @Override
  public boolean isEnabled(String flagName, UnleashContext context, boolean fallback) {
    try {
      return unleash.isEnabled(flagName, context, fallback);
    } catch (Exception e) {
      log.warn("Failed to check feature flag '{}', using fallback: {}", flagName, fallback, e);
      return fallback;
    }
  }

  @Override
  public boolean isEnabled(String flagName, UnleashContext context) {
    return isEnabled(flagName, context, false);
  }

  @Override
  public Variant getVariant(String flagName, UnleashContext context) {
    try {
      Variant variant = unleash.getVariant(flagName, context);
      return variant != null ? variant : Variant.DISABLED_VARIANT;
    } catch (Exception e) {
      log.warn("Failed to get variant for feature flag '{}', returning disabled variant", flagName, e);
      return Variant.DISABLED_VARIANT;
    }
  }

  @Override
  public Map<String, Boolean> getAllFlags() {
    try {
      // Unleash SDK doesn't provide a direct method to list all flags
      // This is a placeholder - in practice, you'd need to track flags or query Unleash API
      log.debug("getAllFlags() called - not fully implemented, returning empty map");
      return new HashMap<>();
    } catch (Exception e) {
      log.warn("Failed to get all flags", e);
      return new HashMap<>();
    }
  }
}

