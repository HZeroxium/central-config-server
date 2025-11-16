package com.example.control.infrastructure.configfile;

import com.example.control.domain.model.ApplicationService;
import com.example.control.infrastructure.configfile.deterministic.DeterministicValueGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Template engine for generating config files from templates.
 * <p>
 * Supports variable substitution using ${variable} syntax.
 * Caches template content after first load for performance.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigFileTemplateEngine {

    private final ResourceLoader resourceLoader;
    private final DeterministicValueGenerator valueGenerator;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    /**
     * Renders a template with variables from ApplicationService.
     *
     * @param templatePath template resource path
     * @param service      application service
     * @param environment  environment name (dev, staging, prod, test) or null for base
     * @return rendered template content
     * @throws IOException if template cannot be loaded
     */
    public String render(String templatePath, ApplicationService service, String environment) throws IOException {
        String template = loadTemplate(templatePath);
        Map<String, String> variables = buildVariables(service, environment);
        return substitute(template, variables);
    }

    /**
     * Loads template content, using cache if available.
     *
     * @param templatePath template resource path
     * @return template content
     * @throws IOException if template cannot be loaded
     */
    private String loadTemplate(String templatePath) throws IOException {
        return templateCache.computeIfAbsent(templatePath, path -> {
            try {
                Resource resource = resourceLoader.getResource(path);
                if (!resource.exists()) {
                    throw new IOException("Template not found: " + path);
                }
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load template: " + path, e);
            }
        });
    }

    /**
     * Builds variable map from ApplicationService and environment.
     *
     * @param service     application service
     * @param environment environment name or null
     * @return variable map
     */
    private Map<String, String> buildVariables(ApplicationService service, String environment) {
        Map<String, String> vars = new HashMap<>();
        String serviceId = service.getId().id();

        // Basic service info
        vars.put("serviceId", serviceId);
        vars.put("displayName", service.getDisplayName() != null ? service.getDisplayName() : serviceId);
        vars.put("environment", environment != null ? environment : "default");

        // Attributes
        Map<String, String> attributes = service.getAttributes();
        if (attributes != null) {
            vars.put("version", attributes.getOrDefault("version", "1.0.0"));
            vars.put("framework", attributes.getOrDefault("framework", "spring-boot"));
            vars.put("language", attributes.getOrDefault("language", "java-21"));
            vars.put("buildTool", attributes.getOrDefault("build-tool", "gradle"));
        } else {
            vars.put("version", "1.0.0");
            vars.put("framework", "spring-boot");
            vars.put("language", "java-21");
            vars.put("buildTool", "gradle");
        }

        // Deterministic values
        vars.put("configHash", valueGenerator.generateHex(serviceId, 16));
        vars.put("port", String.valueOf(valueGenerator.generatePort(serviceId)));
        vars.put("timeout", String.valueOf(valueGenerator.generateTimeout(serviceId)));
        vars.put("delay", String.valueOf(valueGenerator.generateDelay(serviceId)));

        // Environment-specific deterministic values
        if (environment != null) {
            String envKey = serviceId + ":" + environment;
            vars.put("envConfigHash", valueGenerator.generateHex(envKey, 16));
            vars.put("envTimeout", String.valueOf(valueGenerator.generateTimeout(envKey)));
            vars.put("envDelay", String.valueOf(valueGenerator.generateDelay(envKey)));
        }

        // Feature flags (deterministic)
        vars.put("darkMode", String.valueOf(valueGenerator.generateBoolean(serviceId, "dark-mode")));
        vars.put("newEndpointV2", String.valueOf(valueGenerator.generateBoolean(serviceId, "new-endpoint-v2")));
        vars.put("useCacheV1", String.valueOf(valueGenerator.generateBoolean(serviceId, "use-cache-v1")));

        // Business config (deterministic)
        vars.put("maxRetries", String.valueOf(valueGenerator.generateInt(serviceId, "max-retries", 1, 5)));
        vars.put("cacheTtl", String.valueOf(valueGenerator.generateInt(serviceId, "cache-ttl", 60, 7200)));
        vars.put("rateLimit", String.valueOf(valueGenerator.generateInt(serviceId, "rate-limit", 50, 1000)));

        // Logging level (deterministic)
        String[] logLevels = {"INFO", "DEBUG", "WARN"};
        vars.put("logLevel", valueGenerator.generateOption(serviceId, "log-level", logLevels));

        return vars;
    }

    /**
     * Substitutes variables in template using ${variable} syntax.
     *
     * @param template  template content
     * @param variables  variable map
     * @return substituted content
     */
    private String substitute(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }

    /**
     * Clears template cache (useful for testing or reloading templates).
     */
    public void clearCache() {
        templateCache.clear();
    }
}

