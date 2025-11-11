package com.vng.zing.zcm.kv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * Hybrid token provider that attempts to use JWT from Spring Security context
 * (pass-through) and falls back to client credentials if no JWT is available.
 * <p>
 * This provider first checks if there is an authenticated user with a JWT token
 * in the SecurityContext. If found, it extracts and returns that token. Otherwise,
 * it falls back to the ClientCredentialsTokenService to obtain a service account token.
 * <p>
 * Spring Security is optional - if it's not available, the provider will always
 * use client credentials.
 */
@Slf4j
@RequiredArgsConstructor
public class HybridKVTokenProvider implements KVTokenProvider {

  private final ClientCredentialsTokenService clientCredentialsTokenService;

  // Cache reflection lookups
  private static final Class<?> SECURITY_CONTEXT_HOLDER_CLASS;
  private static final Class<?> AUTHENTICATION_CLASS;
  private static final Class<?> JWT_CLASS;
  private static final Method GET_CONTEXT_METHOD;
  private static final Method GET_AUTHENTICATION_METHOD;
  private static final Method GET_PRINCIPAL_METHOD;
  private static final Method GET_TOKEN_VALUE_METHOD;

  static {
    Class<?> securityContextHolder = null;
    Class<?> authentication = null;
    Class<?> jwt = null;
    Method getContext = null;
    Method getAuthentication = null;
    Method getPrincipal = null;
    Method getTokenValue = null;

    try {
      securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
      authentication = Class.forName("org.springframework.security.core.Authentication");
      jwt = Class.forName("org.springframework.security.oauth2.jwt.Jwt");
      getContext = securityContextHolder.getMethod("getContext");
      getAuthentication = Class.forName("org.springframework.security.core.context.SecurityContext").getMethod("getAuthentication");
      getPrincipal = authentication.getMethod("getPrincipal");
      getTokenValue = jwt.getMethod("getTokenValue");
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      // Spring Security not available - will always use client credentials
      // Note: Cannot use log here as it's not initialized in static block
    }

    SECURITY_CONTEXT_HOLDER_CLASS = securityContextHolder;
    AUTHENTICATION_CLASS = authentication;
    JWT_CLASS = jwt;
    GET_CONTEXT_METHOD = getContext;
    GET_AUTHENTICATION_METHOD = getAuthentication;
    GET_PRINCIPAL_METHOD = getPrincipal;
    GET_TOKEN_VALUE_METHOD = getTokenValue;
  }

  /**
   * Gets an access token, preferring pass-through JWT from SecurityContext,
   * falling back to client credentials if no JWT is available.
   *
   * @return JWT access token string
   * @throws RuntimeException if token cannot be obtained
   */
  @Override
  public String getAccessToken() {
    // Try to get JWT from SecurityContext (pass-through) if Spring Security is available
    if (SECURITY_CONTEXT_HOLDER_CLASS != null) {
      try {
        Object securityContext = GET_CONTEXT_METHOD.invoke(null);
        Object auth = GET_AUTHENTICATION_METHOD.invoke(securityContext);
        if (auth != null) {
          Object principal = GET_PRINCIPAL_METHOD.invoke(auth);
          if (principal != null && JWT_CLASS.isInstance(principal)) {
            String tokenValue = (String) GET_TOKEN_VALUE_METHOD.invoke(principal);
            if (tokenValue != null && !tokenValue.isBlank()) {
              log.debug("Using pass-through JWT from SecurityContext for KV authentication");
              return tokenValue;
            }
          }
        }
      } catch (Exception e) {
        // Spring Security might not be configured or JWT not available
        log.debug("Could not extract JWT from SecurityContext, falling back to client credentials: {}",
            e.getMessage());
      }
    }

    // Fallback to client credentials
    log.debug("Using client credentials token for KV authentication");
    return clientCredentialsTokenService.getAccessToken();
  }
}

