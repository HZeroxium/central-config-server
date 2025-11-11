package com.vng.zing.zcm.featureflags;

import io.getunleash.UnleashContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;


/**
 * Request-scoped provider that builds UnleashContext from HttpServletRequest
 * and SecurityContext.
 * <p>
 * Extracts:
 * <ul>
 *   <li>userId from JWT claims (sub or preferred_username)</li>
 *   <li>sessionId from HttpServletRequest</li>
 *   <li>remoteAddress from HttpServletRequest</li>
 *   <li>appName and environment from Spring Environment</li>
 *   <li>Additional properties from JWT claims (tenantId, country, etc.)</li>
 * </ul>
 */
@Slf4j
@Component
@RequestScope
@RequiredArgsConstructor
public class SpringRequestUnleashContextProvider implements SpringUnleashContextProvider {

  private final HttpServletRequest request;
  private final Environment environment;

  @Override
  public UnleashContext getContext() {
    UnleashContext.Builder builder = UnleashContext.builder();

    // Extract userId from SecurityContext (JWT claims) - optional, only if Spring Security is available
    try {
      Object securityContext = Class.forName("org.springframework.security.core.context.SecurityContextHolder")
          .getMethod("getContext")
          .invoke(null);
      if (securityContext != null) {
        Object authentication = securityContext.getClass().getMethod("getAuthentication").invoke(securityContext);
        if (authentication != null) {
          Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
          if (principal != null && principal.getClass().getName().equals("org.springframework.security.oauth2.jwt.Jwt")) {
            // Extract userId from JWT
            String userId = (String) principal.getClass().getMethod("getClaimAsString", String.class).invoke(principal, "sub");
            if (userId == null || userId.isBlank()) {
              userId = (String) principal.getClass().getMethod("getClaimAsString", String.class).invoke(principal, "preferred_username");
            }
            if (userId != null && !userId.isBlank()) {
              builder.userId(userId);
            }

            // Extract additional properties from JWT claims
            // Tenant ID
            String tenantId = (String) principal.getClass().getMethod("getClaimAsString", String.class).invoke(principal, "tenantId");
            if (tenantId != null && !tenantId.isBlank()) {
              builder.addProperty("tenantId", tenantId);
            }
            
            // Country
            String country = (String) principal.getClass().getMethod("getClaimAsString", String.class).invoke(principal, "country");
            if (country != null && !country.isBlank()) {
              builder.addProperty("country", country);
            }
            
            // Groups (teams)
            Object groups = principal.getClass().getMethod("getClaim", String.class).invoke(principal, "groups");
            if (groups instanceof java.util.List<?> groupList && !groupList.isEmpty()) {
              builder.addProperty("groups", String.join(",", groupList.stream().map(String::valueOf).toList()));
            }
          }
        }
      }
    } catch (Exception e) {
      // Spring Security not available or JWT not present - skip userId extraction
      log.debug("Spring Security not available or JWT not present, skipping userId extraction: {}", e.getMessage());
    }

    // Extract sessionId from HttpServletRequest
    String sessionId = request.getSession(false) != null ? request.getSession().getId() : null;
    if (sessionId != null && !sessionId.isBlank()) {
      builder.sessionId(sessionId);
    }

    // Extract remoteAddress
    String remoteAddress = request.getRemoteAddr();
    if (remoteAddress != null && !remoteAddress.isBlank()) {
      builder.remoteAddress(remoteAddress);
    }

    // Extract appName and environment from Spring Environment
    String appName = environment.getProperty("spring.application.name");
    if (appName != null && !appName.isBlank()) {
      builder.appName(appName);
    }

    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles.length > 0) {
      builder.environment(activeProfiles[0]);
    }

    UnleashContext context = builder.build();
    log.debug("Built UnleashContext: userId={}, sessionId={}, remoteAddress={}, appName={}, environment={}, properties={}",
        context.getUserId(), context.getSessionId(), context.getRemoteAddress(),
        context.getAppName(), context.getEnvironment(), context.getProperties());
    
    return context;
  }
}

