package com.example.control.infrastructure.external.fallback;

import com.example.control.infrastructure.resilience.fallback.CachedFallbackProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fallback provider for Keycloak IAM operations.
 * <p>
 * Returns cached IAM data when Keycloak is unavailable.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakFallback {

  private final CachedFallbackProvider cachedFallbackProvider;

  private static final String CACHE_NAME = "keycloak-iam";

  /**
   * Get fallback user data from cache.
   *
   * @param userId User ID
   * @return Cached user data if available
   */
  public String getFallbackUser(String userId) {
    Optional<String> cached = cachedFallbackProvider.getFromCache(CACHE_NAME, "user:" + userId, String.class);

    if (cached.isPresent()) {
      log.info("Returning cached user data for {} (Keycloak fallback)", userId);
      return cached.get();
    } else {
      log.warn("No cached user data for {}, using empty fallback", userId);
      return "{}";
    }
  }

  /**
   * Get fallback realm roles from cache.
   *
   * @param realm Realm name
   * @return Cached realm roles if available
   */
  public String getFallbackRealmRoles(String realm) {
    Optional<String> cached = cachedFallbackProvider.getFromCache(CACHE_NAME, "realm-roles:" + realm, String.class);

    if (cached.isPresent()) {
      log.info("Returning cached realm roles for {} (Keycloak fallback)", realm);
      return cached.get();
    } else {
      log.warn("No cached realm roles for {}, using empty fallback", realm);
      return "[]";
    }
  }

  /**
   * Save user data to cache for future fallback.
   *
   * @param userId   User ID
   * @param userData User data
   */
  public void saveUserToCache(String userId, String userData) {
    cachedFallbackProvider.saveToCache(CACHE_NAME, "user:" + userId, userData);
    log.debug("Saved user {} to fallback cache", userId);
  }

  /**
   * Save realm roles to cache for future fallback.
   *
   * @param realm Realm name
   * @param roles Roles data
   */
  public void saveRealmRolesToCache(String realm, String roles) {
    cachedFallbackProvider.saveToCache(CACHE_NAME, "realm-roles:" + realm, roles);
    log.debug("Saved realm roles for {} to fallback cache", realm);
  }
}
