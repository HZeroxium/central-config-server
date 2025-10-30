package com.example.control.infrastructure.config.mongo;

import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * MongoDB auditing configuration.
 * <p>
 * Enables automatic population of audit fields (@CreatedBy, @LastModifiedBy,
 *
 * @CreatedDate, @LastModifiedDate) for MongoDB documents.
 * </p>
 */
@Slf4j
@Configuration
@EnableMongoAuditing
@RequiredArgsConstructor
public class MongoAuditingConfig {

    /**
     * AuditorAware implementation that extracts the current user ID from Spring Security context.
     * <p>
     * Returns the authenticated user's ID for audit fields, or "system" for anonymous/system operations.
     * </p>
     *
     * @return AuditorAware implementation
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return new SecurityContextAuditorAware();
    }

    /**
     * Implementation of AuditorAware that extracts user ID from Spring Security context.
     */
    private static class SecurityContextAuditorAware implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                    log.debug("No authenticated user found, using 'system' for audit fields");
                    return Optional.of("system");
                }

                // Extract user ID from authentication principal
                Object principal = authentication.getPrincipal();
                String userId = null;

                if (principal instanceof UserContext userContext) {
                    userId = userContext.getUserId();
                } else if (principal instanceof String) {
                    userId = (String) principal;
                } else {
                    // Fallback: try to extract from authentication name
                    userId = authentication.getName();
                }

                if (userId != null && !userId.equals("anonymousUser")) {
                    log.debug("Using user ID '{}' for audit fields", userId);
                    return Optional.of(userId);
                } else {
                    log.debug("No valid user ID found, using 'system' for audit fields");
                    return Optional.of("system");
                }

            } catch (Exception e) {
                log.warn("Error extracting auditor from security context, using 'system': {}", e.getMessage());
                return Optional.of("system");
            }
        }
    }
}
