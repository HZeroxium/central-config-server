package com.example.control.infrastructure.config.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import com.example.control.infrastructure.config.api.OpenApiProperties;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger UI configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    private final OpenApiProperties openApiProperties;
    
    /**
     * Keycloak public URL for OAuth2 authorization (browser-accessible).
     * Defaults to internal Docker hostname for local development.
     * For remote deployment, use KEYCLOAK_PUBLIC_URL or extract from KEYCLOAK_ISSUER_URI.
     */
    @Value("${KEYCLOAK_PUBLIC_URL:${KEYCLOAK_ISSUER_URI:http://keycloak:8080}}")
    private String keycloakPublicUrl;
    
    /**
     * Keycloak realm name (defaults to config-control).
     */
    @Value("${KEYCLOAK_REALM:config-control}")
    private String keycloakRealm;

    public OpenApiConfig(OpenApiProperties openApiProperties) {
        this.openApiProperties = openApiProperties;
    }

    /**
     * Extracts base URL from Keycloak issuer URI if KEYCLOAK_PUBLIC_URL is not set.
     * Example: http://10.40.30.161:28080/realms/config-control -> http://10.40.30.161:28080
     */
    private String getKeycloakBaseUrl() {
        // If KEYCLOAK_PUBLIC_URL is explicitly set, use it
        if (keycloakPublicUrl != null && !keycloakPublicUrl.contains("/realms/")) {
            return keycloakPublicUrl;
        }
        
        // If it contains /realms/, extract base URL
        if (keycloakPublicUrl != null && keycloakPublicUrl.contains("/realms/")) {
            return keycloakPublicUrl.substring(0, keycloakPublicUrl.indexOf("/realms/"));
        }
        
        // Fallback to internal Docker hostname
        return "http://keycloak:8080";
    }

    /**
     * Configure OpenAPI specification with service metadata.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI configControlServiceOpenAPI() {
        String keycloakBaseUrl = getKeycloakBaseUrl();
        String authorizationUrl = String.format("%s/realms/%s/protocol/openid-connect/auth", 
                keycloakBaseUrl, keycloakRealm);
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                keycloakBaseUrl, keycloakRealm);
        
        return new OpenAPI()
                .info(new Info()
                        .title("Config Control Service API")
                        .description("""
                                Centralized configuration management and drift detection service.
                                
                                **Features:**
                                - Service instance heartbeat tracking
                                - Configuration drift detection and reporting
                                - Service discovery integration with Consul
                                - Config refresh orchestration via Kafka
                                - Real-time monitoring and alerts
                                
                                **Architecture:**
                                - Hexagonal architecture with distinct API, Application, Domain, and Infrastructure layers
                                - MongoDB for persistence
                                - Redis for caching
                                - Kafka for event broadcasting
                                """)
                        .version(openApiProperties.getVersion())
                        .contact(new Contact()
                                .name("Platform Team")
                                .email("platform@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description(openApiProperties.getEnvironment() + " server")))
                .components(new Components()
                        .addSecuritySchemes("oauth2_auth_code", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(authorizationUrl)
                                                .tokenUrl(tokenUrl)
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID")
                                                        .addString("profile", "Profile")
                                                        .addString("email", "Email")))))
                        .addSecuritySchemes("oauth2_password", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .password(new OAuthFlow()
                                                .tokenUrl(tokenUrl)
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID")
                                                        .addString("profile", "Profile")
                                                        .addString("email", "Email"))))))
                .addSecurityItem(new SecurityRequirement()
                        .addList("oauth2_auth_code")
                        .addList("oauth2_password"));
    }
}