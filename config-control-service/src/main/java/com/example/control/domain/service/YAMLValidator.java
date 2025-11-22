package com.example.control.domain.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Validates YAML syntax before committing to GitHub.
 * <p>
 * Uses SnakeYAML to parse and validate YAML content.
 * Returns validation result with success flag and error message if invalid.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YAMLValidator {

    private final Yaml yaml = new Yaml();

    /**
     * Validates YAML syntax.
     *
     * @param yamlContent the YAML content to validate
     * @return validation result with success flag and error message
     */
    public ValidationResult validate(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return ValidationResult.failure("YAML content cannot be null or empty");
        }

        try {
            // Attempt to parse YAML
            Object parsed = yaml.load(yamlContent);

            // Basic validation: parsed content should not be null for non-empty input
            if (parsed == null && yamlContent.trim().length() > 0) {
                log.warn("YAML parsed to null for non-empty content");
                // This is actually valid YAML (empty document), but we'll allow it
            }

            log.debug("YAML validation successful");
            return ValidationResult.success();

        } catch (YAMLException e) {
            String errorMessage = extractErrorMessage(e);
            log.warn("YAML validation failed: {}", errorMessage);
            return ValidationResult.failure(errorMessage);
        } catch (Exception e) {
            log.error("Unexpected error during YAML validation", e);
            return ValidationResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Extracts a user-friendly error message from YAMLException.
     *
     * @param e the YAML exception
     * @return formatted error message
     */
    private String extractErrorMessage(YAMLException e) {
        String message = e.getMessage();
        if (message == null) {
            return "Invalid YAML syntax";
        }

        // Extract line number if available
        if (message.contains("line")) {
            return message;
        }

        return "Invalid YAML syntax: " + message;
    }

    /**
     * Validation result containing success flag and optional error message.
     */
    @Data
    public static class ValidationResult {
        private final boolean success;
        private final String errorMessage;

        private ValidationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}

