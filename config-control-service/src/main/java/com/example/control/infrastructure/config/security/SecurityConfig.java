package com.example.control.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Central Spring Security configuration for an OAuth2 Resource Server secured
 * by Keycloak.
 *
 * <p>
 * <strong>Overview</strong>:
 * <ul>
 * <li>Defines multiple {@link SecurityFilterChain}s (ordered) to separate API
 * concerns from public endpoints.</li>
 * <li>Configures JWT validation using {@link JwtDecoder} with issuer discovery
 * and an additional audience check.</li>
 * <li>Maps Keycloak realm/client roles and group memberships into
 * {@link GrantedAuthority} for method/URL security.</li>
 * <li>Enables CORS to support a browser-based client during development.</li>
 * </ul>
 * This configuration follows Spring Security's multi-chain pattern where each
 * chain is selected via
 * {@link HttpSecurity#securityMatcher} and priority is governed by
 * {@link Order}.
 * </p>
 *
 * <p>
 * <strong>Keycloak conventions</strong>:
 * <ul>
 * <li>Realm roles are read from {@code realm_access.roles} and converted to
 * {@code ROLE_*} authorities.</li>
 * <li>Client roles are read from {@code resource_access[clientId].roles} and
 * also converted to {@code ROLE_*}.</li>
 * <li>Group memberships are read from {@code groups} and converted to
 * {@code GROUP_*} authorities.</li>
 * </ul>
 * Adjust claim processing as needed for your realm/client configuration.
 * </p>
 *
 * <p>
 * <strong>Threading</strong>:
 * All beans declared here are thread-safe singletons under normal Spring usage.
 * </p>
 *
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final Environment environment;

    /**
     * Primary API security chain.
     *
     * <p>
     * <strong>Scope</strong>: Applies to requests matching {@code /api/**}.
     * </p>
     *
     * <p>
     * <strong>Behavior</strong>:
     * <ul>
     * <li>Disables CSRF since the API is stateless and authenticated via bearer
     * tokens.</li>
     * <li>Enables CORS using {@link #corsConfigurationSource()}.</li>
     * <li>Sets session creation policy to
     * {@link SessionCreationPolicy#STATELESS}.</li>
     * <li>Authorizes {@code /api/heartbeat/**} as public; all other API endpoints
     * require authentication.</li>
     * <li>Customizes 401/403 responses to return minimal JSON bodies.</li>
     * <li>Enables the OAuth 2.0 Resource Server with JWT:
     * <ul>
     * <li>{@link #jwtAuthenticationConverter()} maps JWT claims to
     * authorities.</li>
     * <li>{@link #jwtDecoder()} performs signature/issuer/time validation +
     * audience validation.</li>
     * </ul>
     * </li>
     * </ul>
     * </p>
     *
     * @param http the {@link HttpSecurity} builder
     * @return the built {@link SecurityFilterChain} for API requests
     * @throws Exception if security configuration fails
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http,
                                        @org.springframework.beans.factory.annotation.Autowired(required = false) ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) throws Exception {
        log.info("Configuring API security chain with Keycloak issuer: {}", securityProperties.getJwt().getIssuerUri());
        
        // Log API key authentication status
        if (apiKeyAuthenticationFilter != null) {
            log.info("API key authentication filter is enabled");
        } else {
            log.debug("API key authentication filter is disabled or not configured");
        }

        var httpBuilder = http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        // Add API key authentication filter before JWT authentication (if enabled)
        if (apiKeyAuthenticationFilter != null) {
            httpBuilder.addFilterBefore(apiKeyAuthenticationFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);
        }
        
        httpBuilder
                .authorizeHttpRequests(authz -> {
                    authz.requestMatchers("/api/heartbeat/**").permitAll();

                    // Permit seeding endpoints in dev/local/seed-data profiles without
                    // authentication
                    if (isSeedingAllowed()) {
                        log.info(
                                "Seeding endpoints (/api/admin/seed/**) are permitted without authentication in current profile");
                        authz.requestMatchers("/api/admin/seed/**").permitAll();
                    }

                    authz.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"unauthorized\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"forbidden\"}");
                        }))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                                .decoder(jwtDecoder())));

        return http.build();
    }

    /**
     * Secondary "public" chain.
     *
     * <p>
     * <strong>Scope</strong>: Applies to Swagger UI, OpenAPI docs, health/info and
     * a broad catch-all pattern.
     * </p>
     *
     * <p>
     * <strong>Important</strong>:
     * This chain includes {@code "/**"} with {@code permitAll()}, which makes
     * <em>all</em> non-{@code /api/**}
     * endpoints publicly accessible. This is appropriate only if your design
     * intentionally exposes everything
     * outside {@code /api/}. Otherwise, restrict patterns explicitly to avoid
     * unintended exposure.
     * </p>
     *
     * @param http the {@link HttpSecurity} builder
     * @return the built {@link SecurityFilterChain} for public endpoints
     * @throws Exception if security configuration fails
     */
    @Bean
    @Order(2)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/actuator/info", "/**")
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Creates a {@link JwtDecoder} using the Keycloak issuer metadata and composes
     * default and custom validators.
     *
     * <p>
     * <strong>Issuer discovery</strong>:
     * Uses {@link JwtDecoders#fromIssuerLocation(String)} to fetch OpenID Provider
     * metadata and build a decoder
     * with the provider's JWK Set URI and defaults.
     * </p>
     *
     * <p>
     * <strong>Validation</strong>:
     * <ul>
     * <li>Default validators include issuer and timestamp checks.</li>
     * <li>An additional audience validator requires {@code aud} to contain
     * {@code securityProperties.jwt.audience}.</li>
     * </ul>
     * On failure, an {@code invalid_token} {@link OAuth2Error} is returned by the
     * validator.
     * </p>
     *
     * @return configured {@link JwtDecoder}
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Build from issuer to get jwks_uri and defaults
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(
                securityProperties.getJwt().getIssuerUri());

        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(
                securityProperties.getJwt().getIssuerUri());

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> aud = jwt.getAudience();
            boolean ok = aud != null && aud.contains(securityProperties.getJwt().getAudience());
            return ok ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audienceValidator));
        return decoder;
    }

    /**
     * Configures a {@link JwtAuthenticationConverter} that maps JWT claims to
     * Spring Security authorities.
     *
     * <p>
     * <strong>Sources</strong>:
     * <ul>
     * <li>Standard scopes via {@link JwtGrantedAuthoritiesConverter} (e.g.,
     * {@code scope}/{@code scp} → {@code SCOPE_*}).</li>
     * <li>Keycloak {@code realm_access.roles} → {@code ROLE_*}.</li>
     * <li>Keycloak {@code resource_access[clientId].roles} → {@code ROLE_*}
     * (clientId hard-coded here).</li>
     * <li>Keycloak {@code groups} → {@code GROUP_*} (normalized path).</li>
     * </ul>
     * The principal claim is set to {@code sub}.
     * </p>
     *
     * @return the configured {@link JwtAuthenticationConverter}
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakGrantedAuthoritiesConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    /**
     * Global CORS configuration for browser clients.
     *
     * <p>
     * <strong>Development defaults</strong>:
     * <ul>
     * <li>{@code allowedOriginPatterns("*")} enables all origins for
     * convenience.</li>
     * <li>{@code allowCredentials(true)} allows cookies/credentials across
     * origins.</li>
     * </ul>
     * <strong>Note</strong>: The CORS specification disallows {@code "*"} when
     * credentials are used.
     * Spring's {@code allowedOriginPatterns} can echo the request origin to satisfy
     * this case,
     * but production deployments should restrict origins explicitly.
     * </p>
     *
     * @return a {@link CorsConfigurationSource} registered for all paths
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // configuration.setAllowedOriginPatterns(List.of("http://localhost:3000",
        // "http://localhost:3001"));
        // Enable all origins for development purposes
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Determines if seeding endpoints should be accessible without authentication.
     * <p>
     * Allows seeding in development, local, or explicitly seed-data profiles.
     * This provides a safe escape hatch for data setup in non-production
     * environments
     * while maintaining security in production.
     * </p>
     *
     * @return true if current profile allows seeding, false otherwise
     */
    private boolean isSeedingAllowed() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equals("dev") || p.equals("local") || p.equals("seed-data"));
    }

    /**
     * Converter that extracts authorities from a Keycloak JWT.
     *
     * <p>
     * <strong>Mapping rules</strong>:
     * <ul>
     * <li>Scopes via {@link JwtGrantedAuthoritiesConverter} (defaults to
     * {@code SCOPE_*}).</li>
     * <li>Realm roles from {@code realm_access.roles} → {@code ROLE_*}
     * (upper-cased).</li>
     * <li>Client roles from {@code resource_access[clientId].roles} →
     * {@code ROLE_*} (upper-cased).</li>
     * <li>Groups from {@code groups} → {@code GROUP_*}, with leading slashes
     * removed and optional {@code teams/} prefix stripped.</li>
     * </ul>
     *
     * <p>
     * <strong>Note</strong>: The clientId used for {@code resource_access} is
     * hard-coded as
     * {@code "config-control-service"} in this example and may be externalized to
     * configuration.
     * </p>
     */
    public static class KeycloakGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        /**
         * Internal composition of the default scope converter with Keycloak-specific
         * role/group mapping.
         *
         * @return a {@link Converter} combining scopes, roles, and groups
         */
        private static Converter<Jwt, Collection<GrantedAuthority>> keycloakAuthoritiesStatic() {
            JwtGrantedAuthoritiesConverter scopeConv = new JwtGrantedAuthoritiesConverter();
            return (Jwt jwt) -> {
                Collection<GrantedAuthority> out = new java.util.LinkedHashSet<>(scopeConv.convert(jwt));

                // Realm roles -> ROLE_*
                Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
                if (realm != null && realm.get("roles") instanceof List<?> rr) {
                    for (Object r : rr)
                        out.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
                }

                // Client roles -> ROLE_* for a specific client
                Map<String, Object> ra = jwt.getClaimAsMap("resource_access");
                String clientId = "config-control-service";
                if (ra != null && ra.get(clientId) instanceof Map<?, ?> m && m.get("roles") instanceof List<?> cr) {
                    for (Object r : cr)
                        out.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
                }

                // Groups -> GROUP_*
                List<String> groups = jwt.getClaimAsStringList("groups");
                if (groups != null) {
                    for (String g : groups) {
                        String norm = g.replaceFirst("^/+", "").replaceFirst("^teams/", "");
                        out.add(new SimpleGrantedAuthority("GROUP_" + norm));
                    }
                }
                return out;
            };
        }

        /**
         * Converts the given {@link Jwt} into a collection of {@link GrantedAuthority}
         * according to Keycloak conventions.
         *
         * @param jwt the JSON Web Token issued by Keycloak
         * @return an immutable {@link Collection} of {@link GrantedAuthority}
         */
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            return List.copyOf(keycloakAuthoritiesStatic().convert(jwt));
        }
    }
}
