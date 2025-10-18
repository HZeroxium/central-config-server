package com.example.control.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Custom JWT validator for audience claim validation.
 * <p>
 * Ensures that JWT tokens contain the expected audience claim for the
 * config-control-service, providing additional security validation.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        
        if (audiences == null || audiences.isEmpty()) {
            log.warn("JWT token has no audience claim");
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "JWT token has no audience claim", null));
        }
        
        if (audiences.contains(expectedAudience)) {
            log.debug("JWT token audience validation successful for: {}", audiences);
            return OAuth2TokenValidatorResult.success();
        }
        
        log.warn("JWT token audience validation failed. Expected: {}, Actual: {}", 
                expectedAudience, audiences);
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", 
                "JWT token audience does not match expected value", null));
    }
}
