package com.vng.zing.zcm.featureflags;

import io.getunleash.UnleashContext;
import io.getunleash.UnleashContextProvider;

/**
 * Spring-specific provider interface for building UnleashContext from request context.
 * <p>
 * This interface extends Unleash SDK's UnleashContextProvider to provide
 * Spring-specific context extraction (userId from SecurityContext, sessionId, etc.).
 * <p>
 * Implementations should extract relevant information from the current
 * request (userId, sessionId, remoteAddress, etc.) and build an appropriate
 * UnleashContext for feature flag evaluation.
 */
public interface SpringUnleashContextProvider extends UnleashContextProvider {

  /**
   * Builds an UnleashContext from the current request context.
   * 
   * @return the UnleashContext, or null if no context is available
   */
  @Override
  UnleashContext getContext();
}

