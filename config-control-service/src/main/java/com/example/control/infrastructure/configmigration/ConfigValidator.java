package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Service for validating configuration files and detecting issues.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ConfigValidator {

    private final Yaml yaml;

    public ConfigValidator() {
        this.yaml = new Yaml();
    }

    /**
     * Validates YAML configuration and returns validation results.
     *
     * @param yamlContent the YAML content
     * @return validation result
     */
    public ValidationResult validate(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return ValidationResult.builder()
                    .valid(false)
                    .errors(List.of("YAML content is empty"))
                    .build();
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        try {
            // Validate YAML syntax
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            if (yamlMap == null) {
                errors.add("YAML file is empty or invalid");
            } else {
                // Check for common issues
                validateStructure(yamlMap, errors, warnings, suggestions);
            }

        } catch (Exception e) {
            errors.add("Invalid YAML syntax: " + e.getMessage());
        }

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .suggestions(suggestions)
                .build();
    }

    /**
     * Validates YAML structure and content.
     *
     * @param yamlMap     the YAML map
     * @param errors      list to add errors
     * @param warnings    list to add warnings
     * @param suggestions list to add suggestions
     */
    @SuppressWarnings("unchecked")
    private void validateStructure(Map<String, Object> yamlMap, List<String> errors, 
                                   List<String> warnings, List<String> suggestions) {
        // Check for Spring Boot application name
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object application = springMap.get("application");
            if (application instanceof Map) {
                Map<String, Object> appMap = (Map<String, Object>) application;
                if (!appMap.containsKey("name")) {
                    warnings.add("spring.application.name is not set - required for SDK integration");
                }
            } else {
                warnings.add("spring.application section is missing");
            }
        }

        // Check for Config Server configuration
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object cloud = springMap.get("cloud");
            if (cloud instanceof Map) {
                Map<String, Object> cloudMap = (Map<String, Object>) cloud;
                if (!cloudMap.containsKey("config")) {
                    suggestions.add("Consider adding Spring Cloud Config Server configuration for centralized config management");
                }
            }
        }

        // Check for @Value usage patterns (would need source code analysis)
        suggestions.add("Consider using @ConfigurationProperties instead of @Value for better type safety and validation");
    }

    /**
     * Validation result.
     */
    @lombok.Data
    @lombok.Builder
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private List<String> suggestions;
    }
}

