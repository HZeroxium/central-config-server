package com.example.control.infrastructure.configfile;

import com.example.control.infrastructure.config.keycloak.KeycloakAdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates initial application.yml template for new services.
 * <p>
 * Creates a template configuration file with ZCM SDK configuration placeholders
 * and client credentials section (commented out until activated).
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceConfigTemplateGenerator {

    private final KeycloakAdminProperties keycloakAdminProperties;

    /**
     * Generates application.yml template for a new service.
     * <p>
     * Includes:
     * <ul>
     * <li>Basic Spring Boot configuration</li>
     * <li>ZCM SDK configuration section with placeholders</li>
     * <li>Client credentials section (commented out, to be uncommented after activation)</li>
     * <li>Service-specific properties</li>
     * </ul>
     * </p>
     *
     * @param serviceId   the service identifier
     * @param displayName the service display name
     * @param environments list of environments (for reference, not used in template)
     * @param clientId    the Keycloak client ID (optional, for placeholder)
     * @return generated YAML content as string
     */
    public String generateApplicationYmlTemplate(String serviceId, String displayName,
                                                 List<String> environments, String clientId) {
        log.debug("Generating application.yml template for service: {} (displayName: {})", serviceId, displayName);

        // Build token endpoint URL
        String tokenEndpoint = buildTokenEndpoint();

        // Build environments list string for comments
        String environmentsStr = environments != null && !environments.isEmpty()
                ? String.join(", ", environments)
                : "dev, staging, prod";

        // Generate template
        StringBuilder template = new StringBuilder();
        template.append("# ").append(displayName).append(" - Base configuration\n");
        template.append("# Generated config file for service: ").append(serviceId).append("\n");
        template.append("# Environments: ").append(environmentsStr).append("\n\n");

        // Spring Boot configuration
        template.append("spring:\n");
        template.append("  application:\n");
        template.append("    name: ").append(serviceId).append("\n");
        template.append("  jackson:\n");
        template.append("    serialization:\n");
        template.append("      WRITE_DATES_AS_TIMESTAMPS: false\n");
        template.append("  main:\n");
        template.append("    banner-mode: \"off\"\n");
        template.append("  threads:\n");
        template.append("    virtual:\n");
        template.append("      enabled: true\n\n");

        // Management endpoints
        template.append("management:\n");
        template.append("  endpoints:\n");
        template.append("    web:\n");
        template.append("      exposure:\n");
        template.append("        include: \"health,info,env,prometheus\"\n\n");

        // Logging
        template.append("logging:\n");
        template.append("  level:\n");
        template.append("    root: INFO\n");
        template.append("    com.example.").append(serviceId).append(": INFO\n\n");

        // ZCM SDK Configuration
        template.append("# ZCM SDK Configuration\n");
        template.append("# This section configures the ZCM SDK for configuration management and service discovery\n");
        template.append("zcm:\n");
        template.append("  sdk:\n");
        template.append("    service:\n");
        template.append("      name: ").append(serviceId).append("\n");
        template.append("    config:\n");
        template.append("      server:\n");
        template.append("        url: ${CONFIG_SERVER_URL:http://config-server:8888}\n");
        template.append("    control:\n");
        template.append("      url: ${CONFIG_CONTROL_URL:http://config-control-service:8080}\n");
        template.append("    ping:\n");
        template.append("      enabled: true\n");
        template.append("      fixed-delay: 30000  # 30 seconds\n");
        template.append("      protocol: HTTP  # Options: HTTP, THRIFT, GRPC, KAFKA\n");
        template.append("      service-discovery-name: config-control-service\n");
        template.append("      # Client credentials for M2M authentication\n");
        template.append("      # Uncomment and fill in after activating credentials via POST /api/services/{serviceId}/credentials/activate\n");
        template.append("      client-credentials:\n");
        template.append("        required: true\n");
        if (clientId != null && !clientId.isBlank()) {
            template.append("        client-id: ").append(clientId).append("\n");
        } else {
            template.append("        # client-id: ").append(serviceId).append("  # Will be set after approval\n");
        }
        template.append("        # client-secret: <your-client-secret>  # Get from GET /api/services/").append(serviceId).append("/credentials\n");
        template.append("        token-endpoint: ").append(tokenEndpoint).append("\n");
        template.append("        realm: ").append(keycloakAdminProperties.getRealm()).append("\n");
        template.append("    bus:\n");
        template.append("      refresh:\n");
        template.append("        enabled: true\n");
        template.append("        topic: springCloudBus\n");
        template.append("      kafka:\n");
        template.append("        bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}\n");
        template.append("    discovery:\n");
        template.append("      provider: CONSUL\n");
        template.append("      consul:\n");
        template.append("        host: ${CONSUL_HOST:localhost}\n");
        template.append("        port: ${CONSUL_PORT:8500}\n");
        template.append("        register: true\n");
        template.append("        heartbeat:\n");
        template.append("          enabled: true\n");
        template.append("          ttl: 10s\n\n");

        // Service-specific configuration
        template.append("# Service-specific configuration\n");
        template.append("service:\n");
        template.append("  id: ").append(serviceId).append("\n");
        template.append("  name: ").append(displayName).append("\n");
        template.append("  version: 1.0.0\n\n");

        // Instructions comment
        template.append("# Configuration Instructions:\n");
        template.append("# 1. After approval, retrieve client credentials: GET /api/services/").append(serviceId).append("/credentials\n");
        template.append("# 2. Uncomment and fill in the client-credentials section above\n");
        template.append("# 3. Activate credentials: POST /api/services/").append(serviceId).append("/credentials/activate\n");
        template.append("# 4. Restart your service to apply the configuration\n");

        String generated = template.toString();
        log.debug("Generated application.yml template for service: {} ({} characters)", serviceId, generated.length());
        return generated;
    }

    /**
     * Builds the Keycloak token endpoint URL.
     *
     * @return token endpoint URL
     */
    private String buildTokenEndpoint() {
        String url = keycloakAdminProperties.getUrl();
        String realm = keycloakAdminProperties.getRealm();

        // Remove trailing slash if present
        if (url != null && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return String.format("%s/realms/%s/protocol/openid-connect/token", url, realm);
    }
}

