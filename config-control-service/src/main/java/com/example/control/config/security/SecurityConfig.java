package com.example.control.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Spring Security configuration for OAuth2 Resource Server with Keycloak.
 * <p>
 * Configures JWT token validation, CORS, and security filter chain for the
 * config-control-service with team-based access control support.
 * </p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    /**
     * API security chain: applies to /api/**, stateless, JWT auth, deny-by-default within matcher.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        log.info("Configuring API security chain with Keycloak issuer: {}", securityProperties.getJwt().getIssuerUri());

        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/heartbeat/**").permitAll()
                .anyRequest().authenticated()
            )
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
                })
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    .decoder(jwtDecoder())
                )
            );

        return http.build();
    }

    /**
     * Public chain: expose docs and health/info; otherwise permitAll here and leave protection to API chain.
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
     * Configure JWT decoder with Keycloak issuer validation.
     *
     * @return configured JwtDecoder
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Build from issuer to get jwks_uri and defaults
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(
            securityProperties.getJwt().getIssuerUri()
        );

        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(
            securityProperties.getJwt().getIssuerUri()
        );

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
     * Configure JWT authentication converter to extract authorities from JWT claims.
     *
     * @return configured JwtAuthenticationConverter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakGrantedAuthoritiesConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    /**
     * Configure CORS for React client.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // configuration.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:3001"));
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
     * Custom granted authorities converter for Keycloak JWT tokens.
     * <p>
     * Extracts authorities from both realm roles and group membership claims.
     * </p>
     */
    public static class KeycloakGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            return java.util.List.copyOf(keycloakAuthoritiesStatic().convert(jwt));
        }

        private static Converter<Jwt, Collection<GrantedAuthority>> keycloakAuthoritiesStatic() {
            JwtGrantedAuthoritiesConverter scopeConv = new JwtGrantedAuthoritiesConverter();
            return (Jwt jwt) -> {
                Collection<GrantedAuthority> out = new java.util.LinkedHashSet<>(scopeConv.convert(jwt));
                Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
                if (realm != null && realm.get("roles") instanceof List<?> rr) {
                    for (Object r : rr) out.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
                }
                Map<String, Object> ra = jwt.getClaimAsMap("resource_access");
                String clientId = "config-control-service";
                if (ra != null && ra.get(clientId) instanceof Map<?,?> m && m.get("roles") instanceof List<?> cr) {
                    for (Object r : cr) out.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
                }
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
    }
}
