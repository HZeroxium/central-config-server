package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing configuration templates for common patterns.
 * <p>
 * Provides templates for database, Redis, Kafka, HTTP clients, feature flags, etc.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class TemplateLibraryService {

    private final ResourceLoader resourceLoader;
    private final Yaml yaml;
    private final Map<String, TemplateInfo> templateCache = new LinkedHashMap<>();

    public TemplateLibraryService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.yaml = new Yaml();
        loadTemplates();
    }

    /**
     * Lists all available templates.
     *
     * @return list of template info
     */
    public List<TemplateInfo> listTemplates() {
        return new ArrayList<>(templateCache.values());
    }

    /**
     * Gets a template by ID.
     *
     * @param templateId the template ID
     * @return template info or null if not found
     */
    public TemplateInfo getTemplate(String templateId) {
        return templateCache.get(templateId);
    }

    /**
     * Gets template content by ID.
     *
     * @param templateId the template ID
     * @return template content or null if not found
     */
    public String getTemplateContent(String templateId) {
        TemplateInfo info = templateCache.get(templateId);
        return info != null ? info.getContent() : null;
    }

    /**
     * Loads all templates from classpath.
     */
    private void loadTemplates() {
        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources("classpath:templates/sdk-integration/*.yml.template");

            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename == null) {
                        continue;
                    }

                    String templateId = filename.replace(".yml.template", "");
                    String content = loadResourceContent(resource);
                    
                    // Parse YAML to extract metadata
                    Map<String, Object> yamlMap = yaml.load(content);
                    
                    TemplateInfo info = TemplateInfo.builder()
                            .id(templateId)
                            .name(toDisplayName(templateId))
                            .description(extractDescription(yamlMap))
                            .category(extractCategory(templateId))
                            .content(content)
                            .yamlMap(yamlMap)
                            .build();

                    templateCache.put(templateId, info);
                    log.debug("Loaded template: {}", templateId);

                } catch (Exception e) {
                    log.warn("Failed to load template: {}", resource.getFilename(), e);
                }
            }

            log.info("Loaded {} templates from template library", templateCache.size());

        } catch (IOException e) {
            log.error("Failed to load templates", e);
        }
    }

    /**
     * Loads resource content as string.
     *
     * @param resource the resource
     * @return content string
     * @throws IOException if read fails
     */
    private String loadResourceContent(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Converts template ID to display name.
     *
     * @param templateId the template ID
     * @return display name
     */
    private String toDisplayName(String templateId) {
        return Arrays.stream(templateId.split("-"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    /**
     * Extracts description from YAML map (from comments or metadata).
     *
     * @param yamlMap the YAML map
     * @return description
     */
    @SuppressWarnings("unchecked")
    private String extractDescription(Map<String, Object> yamlMap) {
        // Try to extract from metadata or use default
        if (yamlMap != null && yamlMap.containsKey("_metadata")) {
            Object metadata = yamlMap.get("_metadata");
            if (metadata instanceof Map) {
                Map<String, Object> meta = (Map<String, Object>) metadata;
                Object desc = meta.get("description");
                if (desc != null) {
                    return desc.toString();
                }
            }
        }
        return "Configuration template";
    }

    /**
     * Extracts category from template ID.
     *
     * @param templateId the template ID
     * @return category
     */
    private String extractCategory(String templateId) {
        if (templateId.contains("database") || templateId.contains("db")) {
            return "Database";
        }
        if (templateId.contains("redis") || templateId.contains("cache")) {
            return "Cache";
        }
        if (templateId.contains("kafka") || templateId.contains("messaging")) {
            return "Messaging";
        }
        if (templateId.contains("http") || templateId.contains("client")) {
            return "HTTP Client";
        }
        if (templateId.contains("feature") || templateId.contains("flag")) {
            return "Feature Flags";
        }
        return "General";
    }

    /**
     * Template information.
     */
    @lombok.Data
    @lombok.Builder
    public static class TemplateInfo {
        private String id;
        private String name;
        private String description;
        private String category;
        private String content;
        private Map<String, Object> yamlMap;
    }
}

