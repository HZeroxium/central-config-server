package com.example.control.infrastructure.configmigration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Service for generating ZCM SDK configuration from existing application.yml.
 * <p>
 * Extracts service name, config server URL, discovery settings, etc.
 * and generates minimal SDK config with sensible defaults.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SdkConfigGenerator {

    private final ConfigAnalyzer configAnalyzer;

    /**
     * Generates ZCM SDK configuration from existing application.yml.
     *
     * @param applicationYml the application.yml content
     * @param serviceName     the service name (optional, will be extracted if not provided)
     * @return SDK config generation result
     */
    public SdkConfigGenerationResult generate(String applicationYml, String serviceName) {
        if (applicationYml == null || applicationYml.isBlank()) {
            throw new IllegalArgumentException("Application YAML content cannot be null or empty");
        }

        ConfigAnalyzer.ConfigAnalysisResult analysis = configAnalyzer.analyze(applicationYml);

        // Use provided service name or extract from config
        String effectiveServiceName = serviceName != null && !serviceName.isBlank() 
                ? serviceName 
                : (analysis.getServiceName() != null ? analysis.getServiceName() : "my-service");

        // Generate SDK config map
        Map<String, Object> sdkConfig = generateSdkConfigMap(analysis, effectiveServiceName);

        // Generate YAML string
        String sdkConfigYaml = generateYamlString(sdkConfig);

        // Generate integration steps
        List<String> integrationSteps = generateIntegrationSteps(effectiveServiceName, analysis);

        return SdkConfigGenerationResult.builder()
                .serviceName(effectiveServiceName)
                .generatedConfig(sdkConfig)
                .generatedConfigYaml(sdkConfigYaml)
                .integrationSteps(integrationSteps)
                .estimatedTime("15 minutes")
                .suggestions(analysis.getSuggestions())
                .build();
    }

    /**
     * Generates SDK config map structure.
     *
     * @param analysis     the config analysis result
     * @param serviceName  the service name
     * @return SDK config map
     */
    private Map<String, Object> generateSdkConfigMap(ConfigAnalyzer.ConfigAnalysisResult analysis, String serviceName) {
        Map<String, Object> zcm = new LinkedHashMap<>();
        Map<String, Object> sdk = new LinkedHashMap<>();

        // Service name
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("name", serviceName);
        sdk.put("service", service);

        // Config server URL
        Map<String, Object> config = new LinkedHashMap<>();
        Map<String, Object> server = new LinkedHashMap<>();
        String configServerUrl = analysis.getConfigServerUrl();
        if (configServerUrl == null || configServerUrl.isBlank()) {
            configServerUrl = "http://config-server:8888";
        }
        server.put("url", configServerUrl);
        config.put("server", server);
        sdk.put("config", config);

        // Control URL
        Map<String, Object> control = new LinkedHashMap<>();
        control.put("url", "http://config-control-service:8080");
        sdk.put("control", control);

        // Ping configuration
        Map<String, Object> ping = new LinkedHashMap<>();
        ping.put("enabled", true);
        ping.put("fixed-delay", "30000");
        ping.put("protocol", "HTTP");
        sdk.put("ping", ping);

        // Bus configuration
        Map<String, Object> bus = new LinkedHashMap<>();
        Map<String, Object> refresh = new LinkedHashMap<>();
        refresh.put("enabled", true);
        refresh.put("topic", "config-refresh");
        bus.put("refresh", refresh);
        if (analysis.getKafkaBootstrapServers() != null) {
            Map<String, Object> kafka = new LinkedHashMap<>();
            kafka.put("bootstrap-servers", analysis.getKafkaBootstrapServers());
            bus.put("kafka", kafka);
        }
        sdk.put("bus", bus);

        // Discovery configuration
        Map<String, Object> discovery = new LinkedHashMap<>();
        Map<String, Object> consul = new LinkedHashMap<>();
        String consulHost = analysis.getConsulHost();
        if (consulHost == null || consulHost.isBlank()) {
            consulHost = "consul";
        }
        consul.put("host", consulHost);
        
        Integer consulPort = analysis.getConsulPort();
        if (consulPort == null) {
            consulPort = 8500;
        }
        consul.put("port", consulPort);
        consul.put("register", true);
        consul.put("heartbeat", Map.of("enabled", true, "ttl", "10s"));
        discovery.put("consul", consul);
        sdk.put("discovery", discovery);

        // Optional features
        if (analysis.isHasKvStore()) {
            Map<String, Object> kv = new LinkedHashMap<>();
            kv.put("enabled", true);
            Map<String, Object> keycloak = new LinkedHashMap<>();
            keycloak.put("token-endpoint", "http://keycloak:8080/realms/config-control/protocol/openid-connect/token");
            keycloak.put("client-id", serviceName + "-kv-client");
            keycloak.put("client-secret", "${KV_CLIENT_SECRET}");
            keycloak.put("realm", "config-control");
            kv.put("keycloak", keycloak);
            sdk.put("kv", kv);
        }

        if (analysis.isHasFeatureFlags()) {
            Map<String, Object> featureFlags = new LinkedHashMap<>();
            featureFlags.put("enabled", true);
            featureFlags.put("unleash-api-url", "http://unleash:4242/api/");
            featureFlags.put("api-key", "${UNLEASH_API_KEY}");
            sdk.put("feature-flags", featureFlags);
        }

        zcm.put("sdk", sdk);

        return Map.of("zcm", zcm);
    }

    /**
     * Generates YAML string from config map.
     *
     * @param config the config map
     * @return YAML string
     */
    private String generateYamlString(Map<String, Object> config) {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        return yaml.dump(config);
    }

    /**
     * Generates integration steps checklist.
     *
     * @param serviceName the service name
     * @param analysis    the config analysis
     * @return list of integration steps
     */
    private List<String> generateIntegrationSteps(String serviceName, ConfigAnalyzer.ConfigAnalysisResult analysis) {
        List<String> steps = new ArrayList<>();

        steps.add("Add dependency to build.gradle:");
        steps.add("  implementation 'com.vng.zing:zcm-spring-sdk-starter:0.1.0'");
        steps.add("");
        steps.add("Add @ConfigurationPropertiesScan to main class:");
        steps.add("  @SpringBootApplication");
        steps.add("  @ConfigurationPropertiesScan");
        steps.add("  public class " + capitalize(serviceName) + "Application { ... }");
        steps.add("");
        steps.add("Copy generated SDK config to application.yml");
        steps.add("");
        steps.add("Update service name if needed: " + serviceName);
        
        if (analysis.getConsulHost() != null) {
            steps.add("Configure Consul discovery: " + analysis.getConsulHost() + ":" + 
                     (analysis.getConsulPort() != null ? analysis.getConsulPort() : 8500));
        }
        
        steps.add("");
        steps.add("Test integration:");
        steps.add("  - Test ping endpoint: GET /actuator/zcm/ping");
        steps.add("  - Verify service registration in Consul");
        steps.add("  - Monitor drift detection in admin dashboard");

        return steps;
    }

    /**
     * Capitalizes first letter.
     *
     * @param str the string
     * @return capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * SDK config generation result.
     */
    @lombok.Data
    @lombok.Builder
    public static class SdkConfigGenerationResult {
        private String serviceName;
        private Map<String, Object> generatedConfig;
        private String generatedConfigYaml;
        private List<String> integrationSteps;
        private String estimatedTime;
        private List<String> suggestions;
    }
}

