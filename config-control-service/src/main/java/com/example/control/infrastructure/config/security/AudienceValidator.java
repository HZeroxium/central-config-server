package com.example.control.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validates the {@code aud} (Audience) claim of a {@link Jwt} against an expected audience,
 * with a Keycloak-oriented convenience fallback.
 *
 * <p><strong>Purpose</strong>:
 * Ensures that an incoming JWT is actually intended for this Resource Server.
 * According to <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519</a> (Section 4.1.3),
 * if a principal processing a token is not listed in its {@code aud} claim, the token <em>must</em> be rejected.</p>
 *
 * <p><strong>Behavior</strong>:
 * <ol>
 *   <li>If the token has no {@code aud} claim (null/empty), validation fails.</li>
 *   <li>If {@code aud} contains the configured {@link #expectedAudience}, validation succeeds.</li>
 *   <li>Additionally, if {@code aud} contains {@code "account"}, validation also succeeds
 *       (a Keycloak-specific convenience for environments where {@code account} may appear by default).</li>
 *   <li>Otherwise, validation fails.</li>
 * </ol>
 * On failure, this validator returns an {@code invalid_token} error via
 * {@link OAuth2TokenValidatorResult#failure(org.springframework.security.oauth2.core.OAuth2Error)}.</p>
 *
 * <p><strong>Security note</strong>:
 * Accepting {@code "account"} as a valid audience is specific to certain Keycloak defaults and may broaden
 * acceptance beyond the intended API. Consider restricting validation to {@link #expectedAudience}
 * (or to a controlled allowlist) in production for stricter audience enforcement.</p>
 *
 * <p><strong>Logging</strong>:
 * <ul>
 *   <li>{@code WARN} when the audience is missing or does not match</li>
 *   <li>{@code DEBUG} when validation succeeds</li>
 * </ul>
 * Logging is intended for diagnostics; avoid logging sensitive token content in production.</p>
 *
 * @apiNote This validator focuses solely on the {@code aud} claim. It should be composed with the default
 * Spring Security validators (signature, {@code iss}, timestamps) using {@code DelegatingOAuth2TokenValidator}.
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    /**
     * The required audience value that this Resource Server expects to find in {@code Jwt.getAudience()}.
     * <p>Examples:</p>
     * <ul>
     *     <li>Client ID (e.g. {@code config-control-service})</li>
     *     <li>API identifier / URI (depending on your IdP conventions)</li>
     * </ul>
     */
    private final String expectedAudience;

    /**
     * Validate the {@code aud} claim of the provided {@link Jwt}.
     *
     * <p><strong>Validation steps</strong>:</p>
     * <ol>
     *   <li>Read {@code audiences = jwt.getAudience()}.</li>
     *   <li>If {@code audiences} is {@code null} or empty, return failure
     *       with {@code invalid_token}.</li>
     *   <li>If {@code audiences} contains {@link #expectedAudience} <em>or</em> {@code "account"},
     *       return success.</li>
     *   <li>Otherwise, return failure with {@code invalid_token}.</li>
     * </ol>
     *
     * <p><strong>Thread-safety</strong>:
     * This method is stateless and thread-safe; the class is immutable w.r.t. its configuration.</p>
     *
     * @param jwt a validated {@link Jwt} (signature/issuer/expiry handled by base validators)
     * @return {@link OAuth2TokenValidatorResult#success()} if audience is acceptable; a failure otherwise
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();

        if (audiences == null || audiences.isEmpty()) {
            log.warn("JWT token has no audience claim");
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "JWT token has no audience claim", null));
        }

        // Accept both the expected audience and "account" (Keycloak default)
        if (audiences.contains(expectedAudience) || audiences.contains("account")) {
            log.debug("JWT token audience validation successful for: {}", audiences);
            return OAuth2TokenValidatorResult.success();
        }

        log.warn("JWT token audience validation failed. Expected: {} or account, Actual: {}",
                expectedAudience, audiences);
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token",
                        "JWT token audience does not match expected value", null));
    }
}
