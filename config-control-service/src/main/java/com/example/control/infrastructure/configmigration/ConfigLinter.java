package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Service for linting configuration files and suggesting improvements.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ConfigLinter {

    private final Yaml yaml;

    public ConfigLinter() {
        this.yaml = new Yaml();
    }

    /**
     * Lints YAML configuration and returns suggestions.
     *
     * @param yamlContent the YAML content
     * @return linting result
     */
    public LintingResult lint(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return LintingResult.builder()
                    .issues(Collections.emptyList())
                    .build();
        }

        List<LintingIssue> issues = new ArrayList<>();

        try {
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            if (yamlMap != null) {
                analyzeConfiguration(yamlMap, issues);
            }
        } catch (Exception e) {
            log.error("Failed to lint configuration", e);
        }

        return LintingResult.builder()
                .issues(issues)
                .build();
    }

    /**
     * Analyzes configuration and generates linting issues.
     *
     * @param yamlMap the YAML map
     * @param issues  list to add issues
     */
    private void analyzeConfiguration(Map<String, Object> yamlMap, List<LintingIssue> issues) {
        // Check for property grouping opportunities
        checkPropertyGrouping(yamlMap, issues);

        // Check for deprecated patterns
        checkDeprecatedPatterns(yamlMap, issues);

        // Check for SDK integration readiness
        checkSdkIntegrationReadiness(yamlMap, issues);
    }

    /**
     * Checks for property grouping opportunities.
     *
     * @param yamlMap the YAML map
     * @param issues  list to add issues
     */
    private void checkPropertyGrouping(Map<String, Object> yamlMap, List<LintingIssue> issues) {
        Set<String> prefixes = new HashSet<>();
        
        for (String key : yamlMap.keySet()) {
            if (key.contains(".")) {
                String prefix = key.substring(0, key.indexOf('.'));
                prefixes.add(prefix);
            }
        }

        for (String prefix : prefixes) {
            long count = yamlMap.keySet().stream()
                    .filter(k -> k.startsWith(prefix + "."))
                    .count();

            if (count > 2) {
                issues.add(LintingIssue.builder()
                        .severity("INFO")
                        .message("Multiple properties with prefix '" + prefix + "' detected")
                        .suggestion("Consider grouping these properties under @" + capitalize(prefix) + "Properties")
                        .property(prefix)
                        .build());
            }
        }
    }

    /**
     * Checks for deprecated patterns.
     *
     * @param yamlMap the YAML map
     * @param issues  list to add issues
     */
    private void checkDeprecatedPatterns(Map<String, Object> yamlMap, List<LintingIssue> issues) {
        // Check for old Spring Boot patterns
        if (yamlMap.containsKey("eureka")) {
            issues.add(LintingIssue.builder()
                    .severity("WARN")
                    .message("Eureka discovery detected")
                    .suggestion("Consider migrating to Consul for service discovery")
                    .property("eureka")
                    .build());
        }
    }

    /**
     * Checks SDK integration readiness.
     *
     * @param yamlMap the YAML map
     * @param issues  list to add issues
     */
    @SuppressWarnings("unchecked")
    private void checkSdkIntegrationReadiness(Map<String, Object> yamlMap, List<LintingIssue> issues) {
        // Check for required SDK properties
        if (!yamlMap.containsKey("zcm")) {
            issues.add(LintingIssue.builder()
                    .severity("INFO")
                    .message("ZCM SDK configuration not found")
                    .suggestion("Use the SDK Integration Wizard to generate SDK configuration")
                    .property("zcm")
                    .build());
        }

        // Check for Consul configuration
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object cloud = springMap.get("cloud");
            if (cloud instanceof Map) {
                Map<String, Object> cloudMap = (Map<String, Object>) cloud;
                if (!cloudMap.containsKey("consul")) {
                    issues.add(LintingIssue.builder()
                            .severity("INFO")
                            .message("Consul discovery not configured")
                            .suggestion("Add Consul configuration for service discovery")
                            .property("spring.cloud.consul")
                            .build());
                }
            }
        }
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
     * Linting result.
     */
    @lombok.Data
    @lombok.Builder
    public static class LintingResult {
        private List<LintingIssue> issues;
    }

    /**
     * Linting issue.
     */
    @lombok.Data
    @lombok.Builder
    public static class LintingIssue {
        private String severity; // INFO, WARN, ERROR
        private String message;
        private String suggestion;
        private String property;
    }
}

